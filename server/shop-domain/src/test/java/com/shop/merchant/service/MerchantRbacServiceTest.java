package com.shop.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.merchant.dto.CreateMerchantRequest;
import com.shop.merchant.dto.MerchantRoleSaveRequest;
import com.shop.merchant.dto.MerchantUserCreateRequest;
import com.shop.merchant.entity.MerchantRole;
import com.shop.merchant.entity.MerchantUser;
import com.shop.merchant.mapper.MerchantRoleMapper;
import com.shop.merchant.mapper.MerchantUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class MerchantRbacServiceTest {

    @Autowired
    private MerchantManagementService merchantManagementService;

    @Autowired
    private MerchantRbacService merchantRbacService;

    @Autowired
    private MerchantUserMapper merchantUserMapper;

    @Autowired
    private MerchantRoleMapper merchantRoleMapper;

    @Test
    void initializesOwnerAndSupportsMultipleRoles() {
        String suffix = String.valueOf(System.nanoTime());
        CreateMerchantRequest merchant = new CreateMerchantRequest();
        merchant.setName("RBAC 测试商户");
        merchant.setUsername("rbac_owner_" + suffix.substring(suffix.length() - 8));
        merchant.setPassword("init123456");
        Long merchantId = merchantManagementService.createMerchant(merchant, 1L);

        MerchantUser owner = merchantUserMapper.selectOne(new LambdaQueryWrapper<MerchantUser>()
                .eq(MerchantUser::getMerchantId, merchantId));
        assertTrue(merchantRbacService.hasPermission(owner.getId(), merchantId, "merchant:rbac:manage"));
        assertTrue(merchantRbacService.hasPermission(owner.getId(), merchantId, "merchant:product:delete"));

        MerchantRoleSaveRequest roleRequest = new MerchantRoleSaveRequest();
        roleRequest.setCode("catalog_reader_" + suffix.substring(suffix.length() - 6));
        roleRequest.setName("商品查看员");
        roleRequest.setPermissionCodes(List.of("merchant:product:view"));
        Long roleId = merchantRbacService.createRole(merchantId, roleRequest);

        MerchantRoleSaveRequest inventoryRoleRequest = new MerchantRoleSaveRequest();
        inventoryRoleRequest.setCode("inventory_reader_" + suffix.substring(suffix.length() - 6));
        inventoryRoleRequest.setName("库存查看员");
        inventoryRoleRequest.setPermissionCodes(List.of("merchant:inventory:view"));
        Long inventoryRoleId = merchantRbacService.createRole(merchantId, inventoryRoleRequest);

        MerchantUserCreateRequest userRequest = new MerchantUserCreateRequest();
        userRequest.setUsername("rbac_user_" + suffix.substring(suffix.length() - 8));
        userRequest.setPassword("init123456");
        userRequest.setRoleIds(List.of(roleId, inventoryRoleId));
        merchantRbacService.createUser(merchantId, userRequest);

        MerchantUser member = merchantUserMapper.selectOne(new LambdaQueryWrapper<MerchantUser>()
                .eq(MerchantUser::getUsername, userRequest.getUsername()));
        assertTrue(merchantRbacService.hasPermission(member.getId(), merchantId, "merchant:product:view"));
        assertTrue(merchantRbacService.hasPermission(member.getId(), merchantId, "merchant:inventory:view"));
        assertFalse(merchantRbacService.hasPermission(member.getId(), merchantId, "merchant:product:update"));
        assertFalse(merchantRbacService.hasPermission(member.getId(), merchantId + 1, "merchant:product:view"));

        MerchantRole inventoryRole = merchantRoleMapper.selectById(inventoryRoleId);
        inventoryRole.setStatus(0);
        merchantRoleMapper.updateById(inventoryRole);
        assertFalse(merchantRbacService.hasPermission(member.getId(), merchantId, "merchant:inventory:view"));
    }
}
