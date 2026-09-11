package com.shop.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.user.dto.AdminUserVO;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;

    @GetMapping("/page")
    public ApiResult<PageResult<AdminUserVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getId);
        if (merchantId != null) {
            query.eq(User::getMerchantId, merchantId);
        }
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            query.and(q -> q.like(User::getNickname, value)
                    .or().like(User::getPhone, value));
        }

        IPage<User> result = userMapper.selectPage(new Page<>(page, size), query);
        List<Long> merchantIds = result.getRecords().stream()
                .map(User::getMerchantId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> merchantNames = new HashMap<>();
        if (!merchantIds.isEmpty()) {
            merchantMapper.selectBatchIds(merchantIds)
                    .forEach(merchant -> merchantNames.put(merchant.getId(), merchant.getName()));
        }

        List<AdminUserVO> users = result.getRecords().stream().map(user -> {
            AdminUserVO vo = new AdminUserVO();
            vo.setId(user.getId());
            vo.setMerchantId(user.getMerchantId());
            vo.setMerchantName(merchantNames.getOrDefault(user.getMerchantId(), ""));
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setPhone(user.getPhone());
            vo.setCreatedAt(user.getCreatedAt());
            vo.setLastLoginAt(user.getLastLoginAt());
            return vo;
        }).toList();
        return ApiResult.success(PageResult.of(users, result.getTotal(), page, size));
    }
}
