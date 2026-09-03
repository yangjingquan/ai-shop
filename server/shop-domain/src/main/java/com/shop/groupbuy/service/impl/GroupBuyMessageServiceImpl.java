package com.shop.groupbuy.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.groupbuy.dto.GroupBuySubscribeRequest;
import com.shop.groupbuy.dto.GroupBuySubscriptionConfigVO;
import com.shop.groupbuy.entity.GroupBuyGroup;
import com.shop.groupbuy.entity.GroupBuyMember;
import com.shop.groupbuy.entity.GroupBuyNotificationLog;
import com.shop.groupbuy.entity.GroupBuyShareEvent;
import com.shop.groupbuy.entity.GroupBuySubscription;
import com.shop.groupbuy.enums.GroupBuyMemberStatus;
import com.shop.groupbuy.mapper.GroupBuyGroupMapper;
import com.shop.groupbuy.mapper.GroupBuyMemberMapper;
import com.shop.groupbuy.mapper.GroupBuyNotificationLogMapper;
import com.shop.groupbuy.mapper.GroupBuyShareEventMapper;
import com.shop.groupbuy.mapper.GroupBuySubscriptionMapper;
import com.shop.groupbuy.service.GroupBuyMessageService;
import com.shop.marketing.entity.MerchantMarketingFeature;
import com.shop.marketing.enums.MarketingActivityCode;
import com.shop.marketing.mapper.MerchantMarketingFeatureMapper;
import com.shop.merchant.entity.MerchantWechatConfig;
import com.shop.merchant.service.MerchantWechatConfigService;
import com.shop.product.entity.Product;
import com.shop.product.mapper.ProductMapper;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupBuyMessageServiceImpl implements GroupBuyMessageService {
    private static final String TYPE_FORMED = "FORMED";
    private static final String TYPE_EXPIRING = "EXPIRING";
    private static final String TYPE_FAILED = "FAILED";
    private static final String TEMPLATE_FORMED = "formed";
    private static final String TEMPLATE_EXPIRING = "expiring";
    private static final String TEMPLATE_FAILED = "failed";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GroupBuyGroupMapper groupMapper;
    private final GroupBuyMemberMapper memberMapper;
    private final GroupBuySubscriptionMapper subscriptionMapper;
    private final GroupBuyNotificationLogMapper notificationLogMapper;
    private final GroupBuyShareEventMapper shareEventMapper;
    private final MerchantMarketingFeatureMapper featureMapper;
    private final MerchantWechatConfigService wechatConfigService;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final com.shop.order.mapper.OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public GroupBuySubscriptionConfigVO subscriptionConfig(Long merchantId, Long groupId) {
        GroupBuyGroup group = groupMapper.selectById(groupId);
        if (group == null || !group.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_NOT_FOUND);
        }
        Map<String, String> config = config(merchantId);
        GroupBuySubscriptionConfigVO vo = new GroupBuySubscriptionConfigVO();
        List<GroupBuySubscriptionConfigVO.Template> templates = new ArrayList<>();
        addTemplate(templates, TEMPLATE_FORMED, config.get("formedTemplateId"));
        addTemplate(templates, TEMPLATE_EXPIRING, config.get("expiringTemplateId"));
        addTemplate(templates, TEMPLATE_FAILED, config.get("failedTemplateId"));
        vo.setTemplates(templates);
        return vo;
    }

    @Override
    @Transactional
    public void recordSubscriptions(Long userId, Long merchantId, GroupBuySubscribeRequest request) {
        GroupBuyGroup group = groupMapper.selectById(request.getGroupId());
        if (group == null || !group.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_NOT_FOUND);
        }
        Map<String, String> config = config(merchantId);
        for (GroupBuySubscribeRequest.Item item : request.getSubscriptions()) {
            String expectedId = templateId(config, item.getTemplateType());
            if (expectedId == null || !expectedId.equals(item.getTemplateId())) continue;
            GroupBuySubscription existing = subscriptionMapper.selectOne(new LambdaQueryWrapper<GroupBuySubscription>()
                    .eq(GroupBuySubscription::getGroupId, group.getId())
                    .eq(GroupBuySubscription::getUserId, userId)
                    .eq(GroupBuySubscription::getTemplateType, item.getTemplateType()));
            if (existing == null) {
                existing = new GroupBuySubscription();
                existing.setMerchantId(merchantId);
                existing.setGroupId(group.getId());
                existing.setUserId(userId);
                existing.setTemplateType(item.getTemplateType());
                existing.setTemplateId(item.getTemplateId());
            }
            existing.setStatus(item.getStatus());
            existing.setSubscribedAt(LocalDateTime.now());
            existing.setSendResult("");
            if (existing.getId() == null) subscriptionMapper.insert(existing);
            else subscriptionMapper.updateById(existing);
        }
    }

    @Override
    @Transactional
    public void recordShare(Long userId, Long merchantId, Long groupId, String source, boolean opened) {
        GroupBuyGroup group = groupMapper.selectById(groupId);
        if (group == null || !group.getMerchantId().equals(merchantId)) {
            throw new BusinessException(ErrorCode.GROUP_BUY_GROUP_NOT_FOUND);
        }
        GroupBuyShareEvent event = new GroupBuyShareEvent();
        event.setMerchantId(merchantId);
        event.setGroupId(groupId);
        event.setSharerUserId(userId);
        event.setOpenerUserId(opened ? userId : null);
        event.setSource(source == null || source.isBlank() ? "unknown" : source.trim());
        event.setOpenedAt(opened ? LocalDateTime.now() : null);
        shareEventMapper.insert(event);
    }

    @Override
    public void notifyGroupFormed(GroupBuyGroup group, List<GroupBuyMember> members) {
        for (GroupBuyMember member : members) {
            if (member.getStatus() == GroupBuyMemberStatus.PAID.getCode()) {
                send(group, member.getUserId(), TYPE_FORMED, TEMPLATE_FORMED, formedData(group));
            }
        }
    }

    @Override
    public void notifyGroupFailed(GroupBuyGroup group, List<GroupBuyMember> members) {
        for (GroupBuyMember member : members) {
            if (member.getStatus() == GroupBuyMemberStatus.WAIT_REFUND.getCode()
                    || member.getStatus() == GroupBuyMemberStatus.PAID.getCode()) {
                send(group, member.getUserId(), TYPE_FAILED, TEMPLATE_FAILED, failedData(group));
            }
        }
    }

    @Override
    public int notifyExpiring(int batchLimit) {
        int limit = Math.min(Math.max(batchLimit, 1), 500);
        LocalDateTime now = LocalDateTime.now();
        List<GroupBuyGroup> groups = groupMapper.selectList(new LambdaQueryWrapper<GroupBuyGroup>()
                .eq(GroupBuyGroup::getStatus, 0)
                .gt(GroupBuyGroup::getExpireAt, now)
                .le(GroupBuyGroup::getExpireAt, now.plusHours(1))
                .orderByAsc(GroupBuyGroup::getExpireAt)
                .last("LIMIT " + limit));
        int count = 0;
        for (GroupBuyGroup group : groups) {
            List<GroupBuyMember> members = memberMapper.selectList(new LambdaQueryWrapper<GroupBuyMember>()
                    .eq(GroupBuyMember::getGroupId, group.getId())
                    .eq(GroupBuyMember::getStatus, GroupBuyMemberStatus.PAID.getCode()));
            for (GroupBuyMember member : members) {
                if (send(group, member.getUserId(), TYPE_EXPIRING, TEMPLATE_EXPIRING, expiringData(group))) count++;
            }
        }
        return count;
    }

    private boolean send(GroupBuyGroup group, Long userId, String eventType, String templateType,
                         Map<String, Map<String, String>> data) {
        Map<String, String> config = config(group.getMerchantId());
        String templateId = templateId(config, templateType);
        if (templateId == null) return false;
        GroupBuySubscription subscription = subscriptionMapper.selectOne(new LambdaQueryWrapper<GroupBuySubscription>()
                .eq(GroupBuySubscription::getGroupId, group.getId())
                .eq(GroupBuySubscription::getUserId, userId)
                .eq(GroupBuySubscription::getTemplateType, templateType)
                .eq(GroupBuySubscription::getStatus, "accept"));
        if (subscription == null) return false;
        GroupBuyNotificationLog notification = notificationLogMapper.selectOne(new LambdaQueryWrapper<GroupBuyNotificationLog>()
                .eq(GroupBuyNotificationLog::getGroupId, group.getId())
                .eq(GroupBuyNotificationLog::getUserId, userId)
                .eq(GroupBuyNotificationLog::getEventType, eventType)
                .eq(GroupBuyNotificationLog::getTemplateType, templateType));
        if (notification != null && "success".equals(notification.getStatus())) return false;
        if (notification == null) {
            notification = new GroupBuyNotificationLog();
            notification.setMerchantId(group.getMerchantId());
            notification.setGroupId(group.getId());
            notification.setUserId(userId);
            notification.setEventType(eventType);
            notification.setTemplateType(templateType);
            notification.setTemplateId(templateId);
        }
        try {
            User user = userMapper.selectById(userId);
            MerchantWechatConfig wechat = wechatConfigService.getByMerchantId(group.getMerchantId());
            if (user == null || wechat == null || user.getOpenid() == null || user.getOpenid().isBlank()) {
                throw new IllegalStateException("用户或商户微信配置缺失");
            }
            JSONObject response = JSONUtil.parseObj(HttpRequest.post("https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken(wechat))
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(Map.of(
                            "touser", user.getOpenid(),
                            "template_id", templateId,
                            "page", "/pages/group-buy/group?groupId=" + group.getId(),
                            "data", data)))
                    .timeout(5000)
                    .execute().body());
            int errcode = response.getInt("errcode", -1);
            notification.setErrcode(errcode);
            notification.setErrmsg(response.getStr("errmsg", ""));
            notification.setStatus(errcode == 0 ? "success" : "failed");
            notification.setSentAt(errcode == 0 ? LocalDateTime.now() : null);
            if (notification.getId() == null) notificationLogMapper.insert(notification);
            else notificationLogMapper.updateById(notification);
            subscription.setSentAt(errcode == 0 ? LocalDateTime.now() : subscription.getSentAt());
            subscription.setSendResult(response.toString());
            subscriptionMapper.updateById(subscription);
            return errcode == 0;
        } catch (Exception e) {
            log.warn("团购订阅消息发送失败 groupId={}, userId={}, eventType={}", group.getId(), userId, eventType, e);
            notification.setStatus("failed");
            notification.setErrmsg(e.getMessage() == null ? "发送失败" : e.getMessage().substring(0, Math.min(500, e.getMessage().length())));
            if (notification.getId() == null) notificationLogMapper.insert(notification);
            else notificationLogMapper.updateById(notification);
            return false;
        }
    }

    private String accessToken(MerchantWechatConfig config) {
        String key = "wx:subscribe:access-token:" + config.getMerchantId();
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null && !cached.isBlank()) return cached;
        if (config.getWxAppId() == null || config.getWxAppId().isBlank()
                || config.getWxSecret() == null || config.getWxSecret().isBlank()) {
            throw new IllegalStateException("商户小程序 AppID 或 Secret 未配置");
        }
        JSONObject response = JSONUtil.parseObj(HttpUtil.get("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + config.getWxAppId() + "&secret=" + config.getWxSecret(), 5000));
        int errcode = response.getInt("errcode", 0);
        String token = response.getStr("access_token");
        if (errcode != 0 || token == null || token.isBlank()) throw new IllegalStateException("获取微信 access_token 失败");
        redisTemplate.opsForValue().set(key, token, 110, TimeUnit.MINUTES);
        return token;
    }

    private Map<String, String> config(Long merchantId) {
        MerchantMarketingFeature feature = featureMapper.selectOne(new LambdaQueryWrapper<MerchantMarketingFeature>()
                .eq(MerchantMarketingFeature::getMerchantId, merchantId)
                .eq(MerchantMarketingFeature::getFeatureCode, MarketingActivityCode.GROUP_BUY.getCode()));
        if (feature == null || feature.getConfigJson() == null || feature.getConfigJson().isBlank()) return Map.of();
        try {
            return objectMapper.readValue(feature.getConfigJson(), objectMapper.getTypeFactory()
                    .constructMapType(LinkedHashMap.class, String.class, String.class));
        } catch (Exception e) {
            log.warn("团购营销配置解析失败 merchantId={}", merchantId, e);
            return Map.of();
        }
    }

    private String templateId(Map<String, String> config, String type) {
        if (TEMPLATE_FORMED.equals(type)) return config.get("formedTemplateId");
        if (TEMPLATE_EXPIRING.equals(type)) return config.get("expiringTemplateId");
        return config.get("failedTemplateId");
    }

    private void addTemplate(List<GroupBuySubscriptionConfigVO.Template> templates, String type, String id) {
        if (id != null && !id.isBlank()) templates.add(new GroupBuySubscriptionConfigVO.Template(type, id));
    }

    private Product product(GroupBuyGroup group) { return productMapper.selectById(group.getProductId()); }

    private Map<String, Map<String, String>> formedData(GroupBuyGroup group) {
        Product product = product(group);
        List<GroupBuyMember> members = memberMapper.selectList(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupId, group.getId()).eq(GroupBuyMember::getStatus, 1));
        String names = members.stream().map(m -> {
            User u = userMapper.selectById(m.getUserId());
            return u == null || u.getNickname() == null ? "拼团成员" : u.getNickname();
        }).reduce((a, b) -> a + "、" + b).orElse("拼团成员");
        return data(Map.of("thing1", text(productName(product)), "thing2", text(names), "phrase5", "已成团"));
    }

    private Map<String, Map<String, String>> expiringData(GroupBuyGroup group) {
        GroupBuyMember member = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupId, group.getId()).eq(GroupBuyMember::getStatus, 1)
                .orderByAsc(GroupBuyMember::getId).last("LIMIT 1"));
        String orderNo = member == null ? "-" : member.getOrderNo();
        String progress = group.getPaidCount() + "人已成团，还差" + Math.max(0, group.getRequiredCount() - group.getPaidCount()) + "人";
        return data(Map.of("character_string1", text(orderNo), "thing2", text(progress),
                "time3", group.getExpireAt().format(TIME), "thing4", "请及时邀请好友参团"));
    }

    private Map<String, Map<String, String>> failedData(GroupBuyGroup group) {
        Product product = product(group);
        String amount = "0.00";
        GroupBuyMember paid = memberMapper.selectOne(new LambdaQueryWrapper<GroupBuyMember>()
                .eq(GroupBuyMember::getGroupId, group.getId()).eq(GroupBuyMember::getStatus, GroupBuyMemberStatus.WAIT_REFUND.getCode())
                .orderByAsc(GroupBuyMember::getId).last("LIMIT 1"));
        if (paid != null) {
            com.shop.order.entity.Order order = orderMapper.selectOne(new LambdaQueryWrapper<com.shop.order.entity.Order>()
                    .eq(com.shop.order.entity.Order::getOrderNo, paid.getOrderNo()));
            if (order != null && order.getPayAmount() != null) amount = order.getPayAmount().toPlainString();
        }
        return data(Map.of("thing1", text(productName(product)), "amount2", amount,
                "number4", String.valueOf(group.getPaidCount()), "amount5", amount, "thing6", "点击查看订单详情"));
    }

    private Map<String, Map<String, String>> data(Map<String, String> values) {
        Map<String, Map<String, String>> data = new HashMap<>();
        values.forEach((key, value) -> data.put(key, Map.of("value", value)));
        return data;
    }

    private String productName(Product product) { return product == null ? "团购商品" : product.getName(); }
    private String text(String value) { return value == null ? "-" : value.substring(0, Math.min(20, value.length())); }
}
