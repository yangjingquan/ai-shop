package com.shop.merchant.controller;

import com.shop.common.response.ApiResult;
import com.shop.common.security.CurrentUser;
import com.shop.common.security.CurrentUserHolder;
import com.shop.common.security.RequirePermission;
import com.shop.merchant.dto.MerchantPermissionVO;
import com.shop.merchant.dto.MerchantRoleSaveRequest;
import com.shop.merchant.dto.MerchantRoleVO;
import com.shop.merchant.dto.MerchantUserCreateRequest;
import com.shop.merchant.dto.MerchantUserVO;
import com.shop.merchant.service.MerchantRbacService;
import com.shop.merchant.security.PasswordCipher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant/rbac")
@RequiredArgsConstructor
public class MerchantRbacController {

    private final MerchantRbacService service;
    private final PasswordCipher passwordCipher;

    @RequirePermission("merchant:rbac:manage")
    @GetMapping("/permissions")
    public ApiResult<List<MerchantPermissionVO>> permissions() {
        return ApiResult.success(service.listPermissions());
    }

    @RequirePermission("merchant:rbac:manage")
    @GetMapping("/roles")
    public ApiResult<List<MerchantRoleVO>> roles() {
        return ApiResult.success(service.listRoles(merchantId()));
    }

    @RequirePermission("merchant:rbac:manage")
    @PostMapping("/roles")
    public ApiResult<Map<String, Long>> createRole(@Valid @RequestBody MerchantRoleSaveRequest request) {
        return ApiResult.success(Map.of("id", service.createRole(merchantId(), request)));
    }

    @RequirePermission("merchant:rbac:manage")
    @PutMapping("/roles/{id}")
    public ApiResult<Void> updateRole(@PathVariable Long id,
                                      @Valid @RequestBody MerchantRoleSaveRequest request) {
        service.updateRole(merchantId(), id, request);
        return ApiResult.success();
    }

    @RequirePermission("merchant:rbac:manage")
    @DeleteMapping("/roles/{id}")
    public ApiResult<Void> deleteRole(@PathVariable Long id) {
        service.deleteRole(merchantId(), id);
        return ApiResult.success();
    }

    @RequirePermission("merchant:rbac:manage")
    @GetMapping("/users")
    public ApiResult<List<MerchantUserVO>> users() {
        return ApiResult.success(service.listUsers(merchantId()));
    }

    @RequirePermission("merchant:rbac:manage")
    @PostMapping("/users")
    public ApiResult<Map<String, Long>> createUser(@Valid @RequestBody MerchantUserCreateRequest request) {
        request.setPassword(passwordCipher.decrypt(request.getPassword()));
        return ApiResult.success(Map.of("id", service.createUser(merchantId(), request)));
    }

    @Data
    public static class StatusRequest {
        @Min(0)
        @Max(1)
        private int status;
    }

    @RequirePermission("merchant:rbac:manage")
    @PutMapping("/users/{id}/status")
    public ApiResult<Void> setUserStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        service.setUserStatus(merchantId(), id, request.getStatus(), userId());
        return ApiResult.success();
    }

    @Data
    public static class RolesRequest {
        private List<Long> roleIds;
    }

    @RequirePermission("merchant:rbac:manage")
    @PutMapping("/users/{id}/roles")
    public ApiResult<Void> setUserRoles(@PathVariable Long id, @Valid @RequestBody RolesRequest request) {
        service.setUserRoles(merchantId(), id, request.getRoleIds(), userId());
        return ApiResult.success();
    }

    @Data
    public static class PasswordRequest {
        @NotBlank
        @Size(max = 1024, message = "密码参数过长")
        private String password;
    }

    @RequirePermission("merchant:rbac:manage")
    @PutMapping("/users/{id}/password")
    public ApiResult<Void> resetUserPassword(@PathVariable Long id,
                                             @Valid @RequestBody PasswordRequest request) {
        service.resetUserPassword(merchantId(), id, passwordCipher.decrypt(request.getPassword()));
        return ApiResult.success();
    }

    private Long merchantId() {
        return currentUser().getMerchantId();
    }

    private Long userId() {
        return currentUser().getUserId();
    }

    private CurrentUser currentUser() {
        CurrentUser user = CurrentUserHolder.get();
        if (user == null || user.getMerchantId() == null) {
            throw new com.shop.common.exception.BusinessException(com.shop.common.exception.ErrorCode.FORBIDDEN);
        }
        return user;
    }
}
