package com.shop.merchant.service;

import com.shop.common.security.MerchantPermissionChecker;
import com.shop.merchant.dto.MerchantAuthMeVO;
import com.shop.merchant.dto.MerchantPermissionVO;
import com.shop.merchant.dto.MerchantRoleSaveRequest;
import com.shop.merchant.dto.MerchantRoleVO;
import com.shop.merchant.dto.MerchantUserCreateRequest;
import com.shop.merchant.dto.MerchantUserVO;

import java.util.List;

public interface MerchantRbacService extends MerchantPermissionChecker {
    void initializeMerchant(Long merchantId, Long ownerUserId);

    MerchantAuthMeVO me(Long userId, Long merchantId);

    List<MerchantPermissionVO> listPermissions();

    List<MerchantRoleVO> listRoles(Long merchantId);

    Long createRole(Long merchantId, MerchantRoleSaveRequest request);

    void updateRole(Long merchantId, Long roleId, MerchantRoleSaveRequest request);

    void deleteRole(Long merchantId, Long roleId);

    List<MerchantUserVO> listUsers(Long merchantId);

    Long createUser(Long merchantId, MerchantUserCreateRequest request);

    void setUserStatus(Long merchantId, Long userId, int status, Long operatorId);

    void setUserRoles(Long merchantId, Long userId, List<Long> roleIds, Long operatorId);

    void resetUserPassword(Long merchantId, Long userId, String password);
}
