package com.shop.customer.service;

import com.shop.common.response.PageResult;
import com.shop.customer.dto.CustomerOperationRequest;
import com.shop.customer.dto.CustomerOperationVO;

import java.util.List;

public interface CustomerOperationsService {
    void refreshAllMetrics();
    void refreshMerchantMetrics(Long merchantId);
    List<CustomerOperationVO.Tag> tags(Long merchantId);
    Long saveTag(Long merchantId, Long tagId, CustomerOperationRequest.TagSave request);
    void bindTag(Long merchantId, Long tagId, CustomerOperationRequest.TagBinding request);
    CustomerOperationVO.UserDetail userDetail(Long merchantId, Long userId);
    PageResult<CustomerOperationVO.UserSummary> segmentUsers(Long merchantId, Long segmentId, int page, int size);
    List<CustomerOperationVO.Segment> segments(Long merchantId);
    Long saveSegment(Long merchantId, Long segmentId, CustomerOperationRequest.SegmentSave request);
    void rebuildSegment(Long merchantId, Long segmentId);
    /**
     * Returns the current audience for a segment using the already refreshed metric snapshot.
     * Callers that need real-time metrics should refresh the merchant before invoking this method.
     */
    List<Long> segmentUserIds(Long merchantId, Long segmentId);
    boolean matchesSegment(Long merchantId, Long segmentId, Long userId);
}
