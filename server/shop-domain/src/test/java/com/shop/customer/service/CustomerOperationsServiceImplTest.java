package com.shop.customer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.response.PageResult;
import com.shop.coupon.mapper.UserCouponMapper;
import com.shop.customer.dto.CustomerOperationRequest;
import com.shop.customer.dto.CustomerOperationVO;
import com.shop.customer.entity.CustomerMetricSnapshot;
import com.shop.customer.entity.CustomerSegment;
import com.shop.customer.entity.CustomerSegmentMember;
import com.shop.customer.entity.CustomerTag;
import com.shop.customer.entity.CustomerTagBinding;
import com.shop.customer.mapper.CustomerMetricSnapshotMapper;
import com.shop.customer.mapper.CustomerSegmentMapper;
import com.shop.customer.mapper.CustomerSegmentMemberMapper;
import com.shop.customer.mapper.CustomerTagBindingMapper;
import com.shop.customer.mapper.CustomerTagMapper;
import com.shop.customer.service.impl.CustomerOperationsServiceImpl;
import com.shop.engagement.mapper.UserProductFavoriteMapper;
import com.shop.engagement.mapper.UserProductHistoryMapper;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.order.mapper.OrderMapper;
import com.shop.points.mapper.MemberProfileMapper;
import com.shop.points.mapper.PointsAccountMapper;
import com.shop.user.entity.User;
import com.shop.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerOperationsServiceImplTest {
    @Mock private CustomerMetricSnapshotMapper metricMapper;
    @Mock private CustomerTagMapper tagMapper;
    @Mock private CustomerTagBindingMapper bindingMapper;
    @Mock private CustomerSegmentMapper segmentMapper;
    @Mock private CustomerSegmentMemberMapper segmentMemberMapper;
    @Mock private UserMapper userMapper;
    @Mock private MerchantMapper merchantMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private MemberProfileMapper memberProfileMapper;
    @Mock private PointsAccountMapper pointsAccountMapper;
    @Mock private UserCouponMapper userCouponMapper;
    @Mock private UserProductFavoriteMapper favoriteMapper;
    @Mock private UserProductHistoryMapper historyMapper;
    private CustomerOperationsService service;

    @BeforeEach
    void setUp() {
        service = new CustomerOperationsServiceImpl(metricMapper, tagMapper, bindingMapper, segmentMapper,
                segmentMemberMapper, userMapper, merchantMapper, orderMapper, memberProfileMapper,
                pointsAccountMapper, userCouponMapper, favoriteMapper, historyMapper, new ObjectMapper());
    }

    @Test
    void dynamicSegmentRequiresAllAndConditions() throws Exception {
        CustomerSegment segment = segment("AND", List.of(condition("paidOrderCount", "GTE", "2"), condition("memberLevel", "GTE", "2")));
        when(segmentMapper.selectById(1L)).thenReturn(segment);
        when(userMapper.selectList(any())).thenReturn(List.of());
        CustomerMetricSnapshot qualified = metric(101L, 2, 2);
        CustomerMetricSnapshot rejected = metric(102L, 2, 1);
        when(metricMapper.selectList(any())).thenReturn(List.of(qualified, rejected), List.of(qualified));
        when(bindingMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.selectBatchIds(List.of(101L))).thenReturn(List.of(user(101L)));
        when(tagMapper.selectList(any())).thenReturn(List.of());

        PageResult<CustomerOperationVO.UserSummary> result = service.segmentUsers(10L, 1L, 1, 10);

        assertEquals(1, result.getTotal());
        assertEquals(101L, result.getList().get(0).getUserId());
    }

    @Test
    void manualTagBindingWritesOnlyForCurrentMerchantUser() {
        CustomerTag tag = new CustomerTag(); tag.setId(8L); tag.setMerchantId(10L); tag.setTagType("MANUAL");
        when(tagMapper.selectById(8L)).thenReturn(tag);
        when(userMapper.selectById(101L)).thenReturn(user(101L));
        when(bindingMapper.selectOne(any())).thenReturn(null);
        CustomerOperationRequest.TagBinding request = new CustomerOperationRequest.TagBinding();
        request.setUserIds(List.of(101L)); request.setStatus(1);

        service.bindTag(10L, 8L, request);

        ArgumentCaptor<CustomerTagBinding> captor = ArgumentCaptor.forClass(CustomerTagBinding.class);
        verify(bindingMapper).insert(captor.capture());
        assertEquals(10L, captor.getValue().getMerchantId());
        assertEquals(101L, captor.getValue().getUserId());
        assertEquals(8L, captor.getValue().getTagId());
        assertEquals(1, captor.getValue().getStatus());
    }

    @Test
    void systemTagCannotBeManuallyBound() {
        CustomerTag tag = new CustomerTag(); tag.setId(9L); tag.setMerchantId(10L); tag.setTagType("SYSTEM");
        when(tagMapper.selectById(9L)).thenReturn(tag);
        CustomerOperationRequest.TagBinding request = new CustomerOperationRequest.TagBinding();
        request.setUserIds(List.of(101L)); request.setStatus(1);

        assertThrows(BusinessException.class, () -> service.bindTag(10L, 9L, request));
        verifyNoInteractions(userMapper);
    }

    @Test
    void staticSegmentSnapshotsMatchedUsersAtSaveTime() {
        CustomerOperationRequest.SegmentSave request = new CustomerOperationRequest.SegmentSave();
        request.setName("高复购会员"); request.setDescription("测试静态分群"); request.setSegmentType("STATIC"); request.setStatus(1);
        CustomerOperationRequest.SegmentConditionGroup group = new CustomerOperationRequest.SegmentConditionGroup();
        group.setLogic("AND"); group.setConditions(List.of(condition("paidOrderCount", "GTE", "2"))); request.setCondition(group);
        doAnswer(invocation -> { ((CustomerSegment) invocation.getArgument(0)).setId(7L); return 1; }).when(segmentMapper).insert(any());
        when(userMapper.selectList(any())).thenReturn(List.of());
        when(metricMapper.selectList(any())).thenReturn(List.of(metric(101L, 2, 1)));
        when(bindingMapper.selectList(any())).thenReturn(List.of());

        Long segmentId = service.saveSegment(10L, null, request);

        assertEquals(7L, segmentId);
        ArgumentCaptor<CustomerSegmentMember> memberCaptor = ArgumentCaptor.forClass(CustomerSegmentMember.class);
        verify(segmentMemberMapper).insert(memberCaptor.capture());
        assertEquals(7L, memberCaptor.getValue().getSegmentId());
        assertEquals(101L, memberCaptor.getValue().getUserId());
    }

    private CustomerSegment segment(String logic, List<CustomerOperationRequest.SegmentCondition> conditions) throws Exception {
        CustomerOperationRequest.SegmentConditionGroup group = new CustomerOperationRequest.SegmentConditionGroup();
        group.setLogic(logic); group.setConditions(conditions);
        CustomerSegment value = new CustomerSegment(); value.setId(1L); value.setMerchantId(10L); value.setSegmentType("DYNAMIC"); value.setConditionJson(new ObjectMapper().writeValueAsString(group)); value.setStatus(1); return value;
    }

    private CustomerOperationRequest.SegmentCondition condition(String field, String operator, String value) { CustomerOperationRequest.SegmentCondition result = new CustomerOperationRequest.SegmentCondition(); result.setField(field); result.setOperator(operator); result.setValue(value); return result; }
    private CustomerMetricSnapshot metric(Long userId, int orderCount, int memberLevel) { CustomerMetricSnapshot value = new CustomerMetricSnapshot(); value.setMerchantId(10L); value.setUserId(userId); value.setPaidOrderCount(orderCount); value.setMemberLevel(memberLevel); value.setTotalPaidAmount(BigDecimal.TEN); value.setPointsBalance(0); value.setUnusedCouponCount(0); value.setFavoriteCount(0); value.setHistoryCount(0); value.setCalculatedAt(LocalDateTime.now()); return value; }
    private User user(Long id) { User value = new User(); value.setId(id); value.setMerchantId(10L); value.setNickname("用户" + id); return value; }
}
