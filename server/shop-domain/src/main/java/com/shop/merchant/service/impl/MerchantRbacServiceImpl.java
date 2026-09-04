package com.shop.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.merchant.dto.MerchantAuthMeVO;
import com.shop.merchant.dto.MerchantPermissionVO;
import com.shop.merchant.dto.MerchantRoleSaveRequest;
import com.shop.merchant.dto.MerchantRoleVO;
import com.shop.merchant.dto.MerchantUserCreateRequest;
import com.shop.merchant.dto.MerchantUserVO;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.entity.MerchantPermission;
import com.shop.merchant.entity.MerchantRole;
import com.shop.merchant.entity.MerchantRolePermission;
import com.shop.merchant.entity.MerchantUser;
import com.shop.merchant.entity.MerchantUserRole;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.merchant.mapper.MerchantPermissionMapper;
import com.shop.merchant.mapper.MerchantRoleMapper;
import com.shop.merchant.mapper.MerchantRolePermissionMapper;
import com.shop.merchant.mapper.MerchantUserMapper;
import com.shop.merchant.mapper.MerchantUserRoleMapper;
import com.shop.merchant.security.PasswordPolicy;
import com.shop.merchant.service.MerchantRbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantRbacServiceImpl implements MerchantRbacService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final MerchantMapper merchantMapper;
    private final MerchantPermissionMapper permissionMapper;
    private final MerchantRoleMapper roleMapper;
    private final MerchantRolePermissionMapper rolePermissionMapper;
    private final MerchantUserMapper userMapper;
    private final MerchantUserRoleMapper userRoleMapper;

    @Override
    @Transactional
    public void initializeMerchant(Long merchantId, Long ownerUserId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        MerchantRole owner = findOrCreateBuiltinRole(merchantId, "owner", "店主", "拥有商户后台全部权限", 10);
        MerchantRole operator = findOrCreateBuiltinRole(merchantId, "operator", "运营人员", "负责商品、分类、Banner、订单和退款处理", 20);
        MerchantRole warehouse = findOrCreateBuiltinRole(merchantId, "warehouse", "仓库人员", "负责库存和订单发货", 30);
        MerchantRole customerService = findOrCreateBuiltinRole(merchantId, "customer_service", "客服人员", "负责订单查询和退款处理", 40);
        replaceRolePermissions(owner.getId(), permissionMapper.selectList(null).stream()
                .map(MerchantPermission::getId).toList());
        replaceRolePermissions(operator.getId(), permissionIds(List.of(
                "merchant:dashboard:view", "merchant:profile:view", "merchant:category:view",
                "merchant:category:create", "merchant:category:update", "merchant:category:status",
                "merchant:category:delete", "merchant:category:import", "merchant:product:view",
                "merchant:product:create", "merchant:product:update", "merchant:product:status",
                "merchant:product:delete", "merchant:product:audit", "merchant:banner:view", "merchant:banner:create",
                "merchant:banner:update", "merchant:banner:delete", "merchant:order:view",
                "merchant:order:detail", "merchant:order:ship", "merchant:order:logistics:view",
                "merchant:order:logistics:refresh", "merchant:refund:view", "merchant:refund:approve",
                "merchant:refund:return-received", "merchant:refund:retry", "merchant:file:upload",
                "merchant:file:delete", "merchant:marketing:view", "merchant:marketing:feature:update",
                "merchant:coupon:view", "merchant:coupon:create", "merchant:coupon:update", "merchant:coupon:status")));
        replaceRolePermissions(warehouse.getId(), permissionIds(List.of(
                "merchant:dashboard:view", "merchant:product:view", "merchant:inventory:view",
                "merchant:inventory:adjust", "merchant:inventory:transaction:view", "merchant:order:view",
                "merchant:order:detail", "merchant:order:ship", "merchant:order:logistics:view",
                "merchant:order:logistics:refresh", "merchant:file:upload", "merchant:file:delete")));
        replaceRolePermissions(customerService.getId(), permissionIds(List.of(
                "merchant:dashboard:view", "merchant:order:view", "merchant:order:detail",
                "merchant:order:logistics:view", "merchant:order:logistics:refresh", "merchant:refund:view",
                "merchant:refund:approve", "merchant:refund:return-received", "merchant:refund:retry")));
        bindUserRoles(ownerUserId, List.of(owner.getId()));
    }

    @Override
    public MerchantAuthMeVO me(Long userId, Long merchantId) {
        MerchantUser user = requireUser(merchantId, userId);
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        MerchantAuthMeVO vo = new MerchantAuthMeVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setMerchantId(merchantId);
        vo.setMerchantName(merchant.getName());
        vo.setRoles(roleVOs(merchantId, roleIds(userId)));
        vo.setPermissions(new ArrayList<>(permissionCodes(userId, merchantId)));
        return vo;
    }

    @Override
    public List<MerchantPermissionVO> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<MerchantPermission>()
                        .orderByAsc(MerchantPermission::getSort)
                        .orderByAsc(MerchantPermission::getId))
                .stream().map(this::toPermissionVO).toList();
    }

    @Override
    public List<MerchantRoleVO> listRoles(Long merchantId) {
        requireMerchant(merchantId);
        return roleMapper.selectList(new LambdaQueryWrapper<MerchantRole>()
                        .eq(MerchantRole::getMerchantId, merchantId)
                        .orderByAsc(MerchantRole::getSort).orderByAsc(MerchantRole::getId))
                .stream().map(role -> toRoleVO(role, true)).toList();
    }

    @Override
    @Transactional
    public Long createRole(Long merchantId, MerchantRoleSaveRequest request) {
        requireMerchant(merchantId);
        String code = request.getCode().trim();
        if (roleMapper.selectCount(new LambdaQueryWrapper<MerchantRole>()
                .eq(MerchantRole::getMerchantId, merchantId).eq(MerchantRole::getCode, code)) > 0) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "角色编码已存在");
        }
        MerchantRole role = new MerchantRole();
        role.setMerchantId(merchantId);
        role.setCode(code);
        role.setName(request.getName().trim());
        role.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        role.setBuiltin(0);
        role.setStatus(1);
        roleMapper.insert(role);
        replaceRolePermissions(role.getId(), permissionIds(request.getPermissionCodes()));
        return role.getId();
    }

    @Override
    @Transactional
    public void updateRole(Long merchantId, Long roleId, MerchantRoleSaveRequest request) {
        MerchantRole role = requireRole(merchantId, roleId);
        if ("owner".equals(role.getCode())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "店主角色不可修改");
        }
        role.setName(request.getName().trim());
        role.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        roleMapper.updateById(role);
        replaceRolePermissions(roleId, permissionIds(request.getPermissionCodes()));
    }

    @Override
    @Transactional
    public void deleteRole(Long merchantId, Long roleId) {
        MerchantRole role = requireRole(merchantId, roleId);
        if (Integer.valueOf(1).equals(role.getBuiltin())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "系统角色不可删除");
        }
        if (userRoleMapper.selectCount(new LambdaQueryWrapper<MerchantUserRole>()
                .eq(MerchantUserRole::getRoleId, roleId)) > 0) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "角色已分配给账号，不能删除");
        }
        roleMapper.deleteById(roleId);
    }

    @Override
    public List<MerchantUserVO> listUsers(Long merchantId) {
        requireMerchant(merchantId);
        return userMapper.selectList(new LambdaQueryWrapper<MerchantUser>()
                        .eq(MerchantUser::getMerchantId, merchantId)
                        .orderByAsc(MerchantUser::getCreatedAt))
                .stream().map(user -> toUserVO(user, merchantId)).toList();
    }

    @Override
    @Transactional
    public Long createUser(Long merchantId, MerchantUserCreateRequest request) {
        requireMerchant(merchantId);
        PasswordPolicy.validate(request.getPassword());
        if (userMapper.selectCount(new LambdaQueryWrapper<MerchantUser>()
                .eq(MerchantUser::getUsername, request.getUsername().trim())) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        validateRoleIds(merchantId, request.getRoleIds());
        MerchantUser user = new MerchantUser();
        user.setMerchantId(merchantId);
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(ENCODER.encode(request.getPassword()));
        user.setRole("merchant");
        user.setStatus(1);
        user.setTokenVersion(0);
        userMapper.insert(user);
        bindUserRoles(user.getId(), request.getRoleIds());
        return user.getId();
    }

    @Override
    @Transactional
    public void setUserStatus(Long merchantId, Long userId, int status, Long operatorId) {
        MerchantUser user = requireUser(merchantId, userId);
        if (userId.equals(operatorId)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "不能禁用当前登录账号");
        }
        if (status == 0 && hasOwnerRole(userId) && activeOwnerCount(merchantId, userId) == 0) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "不能禁用最后一个店主");
        }
        user.setStatus(status == 1 ? 1 : 0);
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void setUserRoles(Long merchantId, Long userId, List<Long> roleIds, Long operatorId) {
        requireUser(merchantId, userId);
        validateRoleIds(merchantId, roleIds);
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "账号至少需要一个角色");
        }
        if (userId.equals(operatorId) && !roleIds.contains(ownerRoleId(merchantId)) && hasOwnerRole(userId)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "当前登录店主不能移除自己的店主角色");
        }
        if (hasOwnerRole(userId) && !roleIds.contains(ownerRoleId(merchantId)) && activeOwnerCount(merchantId, userId) == 0) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "不能移除最后一个店主");
        }
        bindUserRoles(userId, roleIds);
        MerchantUser user = userMapper.selectById(userId);
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void resetUserPassword(Long merchantId, Long userId, String password) {
        PasswordPolicy.validate(password);
        MerchantUser user = requireUser(merchantId, userId);
        user.setPasswordHash(ENCODER.encode(password));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
    }

    @Override
    public boolean hasPermission(Long userId, Long merchantId, String permission) {
        if (userId == null || merchantId == null || permission == null) return false;
        try {
            requireUser(merchantId, userId);
            return permissionCodes(userId, merchantId).contains(permission);
        } catch (BusinessException ex) {
            return false;
        }
    }

    private Set<String> permissionCodes(Long userId, Long merchantId) {
        List<Long> assignedRoleIds = roleIds(userId);
        if (assignedRoleIds.isEmpty()) return Set.of();
        List<Long> roleIds = roleMapper.selectList(new LambdaQueryWrapper<MerchantRole>()
                        .eq(MerchantRole::getMerchantId, merchantId)
                        .eq(MerchantRole::getStatus, 1)
                        .in(MerchantRole::getId, assignedRoleIds))
                .stream().map(MerchantRole::getId).toList();
        if (roleIds.isEmpty()) return Set.of();
        List<Long> permissionIds = rolePermissionMapper.selectList(new LambdaQueryWrapper<MerchantRolePermission>()
                        .in(MerchantRolePermission::getRoleId, roleIds))
                .stream().map(MerchantRolePermission::getPermissionId).distinct().toList();
        if (permissionIds.isEmpty()) return Set.of();
        return permissionMapper.selectBatchIds(permissionIds).stream()
                .map(MerchantPermission::getCode).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> roleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<MerchantUserRole>()
                        .eq(MerchantUserRole::getUserId, userId)).stream()
                .map(MerchantUserRole::getRoleId).toList();
    }

    private MerchantRoleVO toRoleVO(MerchantRole role, boolean withPermissions) {
        MerchantRoleVO vo = new MerchantRoleVO();
        BeanUtils.copyProperties(role, vo);
        if (withPermissions) {
            List<Long> ids = rolePermissionMapper.selectList(new LambdaQueryWrapper<MerchantRolePermission>()
                            .eq(MerchantRolePermission::getRoleId, role.getId())).stream()
                    .map(MerchantRolePermission::getPermissionId).toList();
            vo.setPermissionCodes(ids.isEmpty() ? List.of() : permissionMapper.selectBatchIds(ids).stream()
                    .map(MerchantPermission::getCode).toList());
        }
        vo.setUserCount(userRoleMapper.selectCount(new LambdaQueryWrapper<MerchantUserRole>()
                .eq(MerchantUserRole::getRoleId, role.getId())).intValue());
        return vo;
    }

    private MerchantUserVO toUserVO(MerchantUser user, Long merchantId) {
        MerchantUserVO vo = new MerchantUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setRoles(roleVOs(merchantId, roleIds(user.getId())));
        return vo;
    }

    private List<MerchantRoleVO> roleVOs(Long merchantId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return List.of();
        return roleMapper.selectList(new LambdaQueryWrapper<MerchantRole>()
                        .eq(MerchantRole::getMerchantId, merchantId).in(MerchantRole::getId, roleIds))
                .stream().map(role -> toRoleVO(role, false)).toList();
    }

    private MerchantPermissionVO toPermissionVO(MerchantPermission permission) {
        MerchantPermissionVO vo = new MerchantPermissionVO();
        BeanUtils.copyProperties(permission, vo);
        return vo;
    }

    private MerchantRole findOrCreateBuiltinRole(Long merchantId, String code, String name,
                                                   String description, int sort) {
        MerchantRole role = roleMapper.selectOne(new LambdaQueryWrapper<MerchantRole>()
                .eq(MerchantRole::getMerchantId, merchantId).eq(MerchantRole::getCode, code).last("LIMIT 1"));
        if (role != null) return role;
        role = new MerchantRole();
        role.setMerchantId(merchantId);
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setBuiltin(1);
        role.setStatus(1);
        role.setSort(sort);
        roleMapper.insert(role);
        return role;
    }

    private void replaceRolePermissions(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<MerchantRolePermission>()
                .eq(MerchantRolePermission::getRoleId, roleId));
        if (permissionIds == null) return;
        for (Long permissionId : new LinkedHashSet<>(permissionIds)) {
            MerchantRolePermission relation = new MerchantRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        }
    }

    private void bindUserRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<MerchantUserRole>()
                .eq(MerchantUserRole::getUserId, userId));
        for (Long roleId : new LinkedHashSet<>(roleIds)) {
            MerchantUserRole relation = new MerchantUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private List<Long> permissionIds(List<String> codes) {
        if (codes == null || codes.isEmpty()) return List.of();
        List<MerchantPermission> permissions = permissionMapper.selectList(new LambdaQueryWrapper<MerchantPermission>()
                .in(MerchantPermission::getCode, codes));
        if (permissions.size() != new LinkedHashSet<>(codes).size()) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "存在无效权限");
        }
        return permissions.stream().map(MerchantPermission::getId).toList();
    }

    private void validateRoleIds(Long merchantId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        long count = roleMapper.selectCount(new LambdaQueryWrapper<MerchantRole>()
                .eq(MerchantRole::getMerchantId, merchantId).in(MerchantRole::getId, ids));
        if (count != new LinkedHashSet<>(ids).size()) {
            throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "存在不属于当前商户的角色");
        }
    }

    private Merchant requireMerchant(Long merchantId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null) throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        return merchant;
    }

    private MerchantRole requireRole(Long merchantId, Long roleId) {
        MerchantRole role = roleMapper.selectOne(new LambdaQueryWrapper<MerchantRole>()
                .eq(MerchantRole::getMerchantId, merchantId).eq(MerchantRole::getId, roleId).last("LIMIT 1"));
        if (role == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "角色不存在");
        return role;
    }

    private MerchantUser requireUser(Long merchantId, Long userId) {
        MerchantUser user = userMapper.selectOne(new LambdaQueryWrapper<MerchantUser>()
                .eq(MerchantUser::getMerchantId, merchantId).eq(MerchantUser::getId, userId).last("LIMIT 1"));
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return user;
    }

    private Long ownerRoleId(Long merchantId) {
        MerchantRole owner = roleMapper.selectOne(new LambdaQueryWrapper<MerchantRole>()
                .eq(MerchantRole::getMerchantId, merchantId).eq(MerchantRole::getCode, "owner").last("LIMIT 1"));
        return owner == null ? -1L : owner.getId();
    }

    private boolean hasOwnerRole(Long userId) {
        return userRoleMapper.selectCount(new LambdaQueryWrapper<MerchantUserRole>()
                .eq(MerchantUserRole::getUserId, userId).eq(MerchantUserRole::getRoleId, ownerRoleIdForUser(userId))) > 0;
    }

    private Long ownerRoleIdForUser(Long userId) {
        MerchantUser user = userMapper.selectById(userId);
        if (user == null) return -1L;
        return ownerRoleId(user.getMerchantId());
    }

    private int activeOwnerCount(Long merchantId, Long excludingUserId) {
        Long ownerId = ownerRoleId(merchantId);
        if (ownerId < 0) return 0;
        List<Long> ownerUserIds = userRoleMapper.selectList(new LambdaQueryWrapper<MerchantUserRole>()
                        .eq(MerchantUserRole::getRoleId, ownerId)).stream()
                .map(MerchantUserRole::getUserId).toList();
        if (ownerUserIds.isEmpty()) return 0;
        return userMapper.selectCount(new LambdaQueryWrapper<MerchantUser>()
                .eq(MerchantUser::getMerchantId, merchantId).eq(MerchantUser::getStatus, 1)
                .in(MerchantUser::getId, ownerUserIds).ne(excludingUserId != null, MerchantUser::getId, excludingUserId)).intValue();
    }
}
