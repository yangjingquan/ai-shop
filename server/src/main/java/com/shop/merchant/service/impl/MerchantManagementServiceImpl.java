package com.shop.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
import com.shop.merchant.dto.CreateMerchantRequest;
import com.shop.merchant.dto.MerchantSelfVO;
import com.shop.merchant.dto.MerchantVO;
import com.shop.merchant.dto.UpdateMerchantRequest;
import com.shop.merchant.dto.UpdateMerchantSelfRequest;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.entity.MerchantUser;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.merchant.mapper.MerchantUserMapper;
import com.shop.merchant.service.MerchantManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantManagementServiceImpl implements MerchantManagementService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final MerchantMapper merchantMapper;
    private final MerchantUserMapper merchantUserMapper;

    @Override
    @Transactional
    public Long createMerchant(CreateMerchantRequest req, Long adminUserId) {
        // 校验用户名唯一
        Long existing = merchantUserMapper.selectCount(
                new LambdaQueryWrapper<MerchantUser>().eq(MerchantUser::getUsername, req.getUsername())
        );
        if (existing != null && existing > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 插 merchant
        Merchant m = new Merchant();
        m.setName(req.getName());
        m.setLogo(req.getLogo());
        m.setDescription(req.getDescription());
        m.setAddress(req.getAddress());
        m.setContactName(req.getContactName());
        m.setContactPhone(req.getContactPhone());
        m.setWxAppId(req.getWxAppId() == null ? "" : req.getWxAppId());
        m.setWxSecret(req.getWxSecret() == null ? "" : req.getWxSecret());
        m.setStatus(1);
        m.setCreatedByAdminId(adminUserId);
        merchantMapper.insert(m);
        m.setMerchantCode(generateMerchantCode(m.getId()));
        merchantMapper.updateById(m);

        // 插首个 merchant_user
        MerchantUser mu = new MerchantUser();
        mu.setMerchantId(m.getId());
        mu.setUsername(req.getUsername());
        mu.setPasswordHash(ENCODER.encode(req.getPassword()));
        mu.setRole("merchant");
        merchantUserMapper.insert(mu);

        return m.getId();
    }

    @Override
    public PageResult<MerchantVO> listMerchants(int page, int size, String keyword) {
        Page<Merchant> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .orderByDesc(Merchant::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Merchant::getName, keyword);
        }
        Page<Merchant> result = merchantMapper.selectPage(pageReq, wrapper);

        // 批量取首个 merchant_user 的 username
        List<Long> ids = result.getRecords().stream().map(Merchant::getId).toList();
        Map<Long, String> usernameMap = new HashMap<>();
        if (!ids.isEmpty()) {
            List<MerchantUser> users = merchantUserMapper.selectList(
                    new LambdaQueryWrapper<MerchantUser>().in(MerchantUser::getMerchantId, ids)
            );
            // 取每个 merchantId 下最早创建的 username
            for (MerchantUser u : users) {
                usernameMap.putIfAbsent(u.getMerchantId(), u.getUsername());
            }
        }

        List<MerchantVO> vos = result.getRecords().stream().map(m -> {
            MerchantVO vo = toVO(m);
            vo.setUsername(usernameMap.get(m.getId()));
            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(vos, result.getTotal(), page, size);
    }

    @Override
    public MerchantVO getMerchant(Long id) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        MerchantVO vo = toVO(m);
        MerchantUser u = merchantUserMapper.selectOne(
                new LambdaQueryWrapper<MerchantUser>()
                        .eq(MerchantUser::getMerchantId, id)
                        .orderByAsc(MerchantUser::getCreatedAt)
                        .last("LIMIT 1")
        );
        if (u != null) {
            vo.setUsername(u.getUsername());
        }
        return vo;
    }

    @Override
    public void updateMerchant(Long id, UpdateMerchantRequest req) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        if (req.getName() != null) m.setName(req.getName());
        if (req.getLogo() != null) m.setLogo(req.getLogo());
        if (req.getDescription() != null) m.setDescription(req.getDescription());
        if (req.getAddress() != null) m.setAddress(req.getAddress());
        if (req.getContactName() != null) m.setContactName(req.getContactName());
        if (req.getContactPhone() != null) m.setContactPhone(req.getContactPhone());
        if (req.getWxAppId() != null) m.setWxAppId(req.getWxAppId());
        if (req.getWxSecret() != null && !req.getWxSecret().isBlank()) m.setWxSecret(req.getWxSecret());
        merchantMapper.updateById(m);
    }

    private MerchantVO toVO(Merchant m) {
        MerchantVO vo = new MerchantVO();
        BeanUtils.copyProperties(m, vo);
        vo.setWxSecretConfigured(m.getWxSecret() != null && !m.getWxSecret().isBlank());
        return vo;
    }

    private String generateMerchantCode(Long merchantId) {
        if (merchantId == null || merchantId <= 0 || merchantId > 999999) {
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "商户ID超出6位商户代码生成范围");
        }
        return String.format("%06d", merchantId);
    }

    @Override
    public void setStatus(Long id, int status) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        m.setStatus(status);
        merchantMapper.updateById(m);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        MerchantUser mu = merchantUserMapper.selectOne(
                new LambdaQueryWrapper<MerchantUser>().eq(MerchantUser::getMerchantId, id));
        if (mu == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        mu.setPasswordHash(ENCODER.encode(newPassword));
        merchantUserMapper.updateById(mu);
    }

    @Override
    public MerchantSelfVO getSelf(Long merchantId) {
        Merchant m = merchantMapper.selectById(merchantId);
        if (m == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        MerchantSelfVO vo = new MerchantSelfVO();
        BeanUtils.copyProperties(m, vo);
        return vo;
    }

    @Override
    public void updateSelf(Long merchantId, UpdateMerchantSelfRequest req) {
        Merchant m = merchantMapper.selectById(merchantId);
        if (m == null) {
            throw new BusinessException(ErrorCode.MERCHANT_NOT_FOUND);
        }
        if (req.getLogo() != null) m.setLogo(req.getLogo());
        if (req.getDescription() != null) m.setDescription(req.getDescription());
        if (req.getAddress() != null) m.setAddress(req.getAddress());
        if (req.getContactName() != null) m.setContactName(req.getContactName());
        if (req.getContactPhone() != null) m.setContactPhone(req.getContactPhone());
        merchantMapper.updateById(m);
    }
}
