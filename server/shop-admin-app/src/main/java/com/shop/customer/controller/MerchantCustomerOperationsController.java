package com.shop.customer.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.response.PageResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.customer.dto.CustomerOperationRequest;
import com.shop.customer.dto.CustomerOperationVO;
import com.shop.customer.service.CustomerOperationsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/merchant/customer-operations")
@RequiredArgsConstructor
public class MerchantCustomerOperationsController {
    private final CustomerOperationsService customerOperationsService;

    private Long merchant() { CurrentUser user = CurrentUserHolder.get(); return user == null ? null : user.getMerchantId(); }

    @GetMapping("/tags") @RequirePermission("merchant:customer:view")
    public ApiResult<List<CustomerOperationVO.Tag>> tags() { return ApiResult.success(customerOperationsService.tags(merchant())); }

    @PostMapping("/tags") @RequirePermission("merchant:customer:manage")
    public ApiResult<Long> createTag(@RequestBody @Valid CustomerOperationRequest.TagSave request) { return ApiResult.success(customerOperationsService.saveTag(merchant(), null, request)); }

    @PutMapping("/tags/{tagId}") @RequirePermission("merchant:customer:manage")
    public ApiResult<Long> updateTag(@PathVariable Long tagId, @RequestBody @Valid CustomerOperationRequest.TagSave request) { return ApiResult.success(customerOperationsService.saveTag(merchant(), tagId, request)); }

    @PostMapping("/tags/{tagId}/bindings") @RequirePermission("merchant:customer:manage")
    public ApiResult<Void> bindTag(@PathVariable Long tagId, @RequestBody @Valid CustomerOperationRequest.TagBinding request) { customerOperationsService.bindTag(merchant(), tagId, request); return ApiResult.success(); }

    @GetMapping("/users/{userId}") @RequirePermission("merchant:customer:view")
    public ApiResult<CustomerOperationVO.UserDetail> user(@PathVariable Long userId) { return ApiResult.success(customerOperationsService.userDetail(merchant(), userId)); }

    @GetMapping("/segments") @RequirePermission("merchant:customer:view")
    public ApiResult<List<CustomerOperationVO.Segment>> segments() { return ApiResult.success(customerOperationsService.segments(merchant())); }

    @PostMapping("/segments") @RequirePermission("merchant:customer:manage")
    public ApiResult<Long> createSegment(@RequestBody @Valid CustomerOperationRequest.SegmentSave request) { return ApiResult.success(customerOperationsService.saveSegment(merchant(), null, request)); }

    @PutMapping("/segments/{segmentId}") @RequirePermission("merchant:customer:manage")
    public ApiResult<Long> updateSegment(@PathVariable Long segmentId, @RequestBody @Valid CustomerOperationRequest.SegmentSave request) { return ApiResult.success(customerOperationsService.saveSegment(merchant(), segmentId, request)); }

    @PostMapping("/segments/{segmentId}/rebuild") @RequirePermission("merchant:customer:manage")
    public ApiResult<Void> rebuildSegment(@PathVariable Long segmentId) { customerOperationsService.rebuildSegment(merchant(), segmentId); return ApiResult.success(); }

    @GetMapping("/segments/{segmentId}/users") @RequirePermission("merchant:customer:view")
    public ApiResult<PageResult<CustomerOperationVO.UserSummary>> segmentUsers(@PathVariable Long segmentId, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) { return ApiResult.success(customerOperationsService.segmentUsers(merchant(), segmentId, page, size)); }
}
