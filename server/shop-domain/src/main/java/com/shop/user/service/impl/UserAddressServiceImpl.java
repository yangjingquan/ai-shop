package com.shop.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.user.dto.UserAddressRequest;
import com.shop.user.dto.UserAddressVO;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import com.shop.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private static final int MAX_ADDRESS_PER_USER = 20;

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddressVO> list(Long userId) {
        List<UserAddress> rows = userAddressMapper.selectList(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .orderByDesc(UserAddress::getIsDefault)
                        .orderByDesc(UserAddress::getCreatedAt)
        );
        return rows.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public UserAddressVO get(Long userId, Long id) {
        UserAddress addr = loadOwned(userId, id);
        return toVO(addr);
    }

    @Override
    @Transactional
    public Long create(Long userId, UserAddressRequest req) {
        Long count = userAddressMapper.selectCount(
                new LambdaQueryWrapper<UserAddress>().eq(UserAddress::getUserId, userId)
        );
        if (count != null && count >= MAX_ADDRESS_PER_USER) {
            throw new BusinessException(ErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }

        boolean wantDefault = Boolean.TRUE.equals(req.getIsDefault());
        // 若用户当前一条都没有，强制首条为默认
        if (count == null || count == 0) {
            wantDefault = true;
        }
        if (wantDefault) {
            clearDefault(userId);
        }

        UserAddress addr = new UserAddress();
        addr.setUserId(userId);
        addr.setReceiver(req.getReceiver());
        addr.setPhone(req.getPhone());
        addr.setRegion(req.getRegion());
        addr.setDetail(req.getDetail());
        addr.setIsDefault(wantDefault ? 1 : 0);
        userAddressMapper.insert(addr);
        return addr.getId();
    }

    @Override
    @Transactional
    public void update(Long userId, Long id, UserAddressRequest req) {
        UserAddress addr = loadOwned(userId, id);
        boolean wantDefault = Boolean.TRUE.equals(req.getIsDefault());
        if (wantDefault && (addr.getIsDefault() == null || addr.getIsDefault() == 0)) {
            clearDefault(userId);
        }
        addr.setReceiver(req.getReceiver());
        addr.setPhone(req.getPhone());
        addr.setRegion(req.getRegion());
        addr.setDetail(req.getDetail());
        if (wantDefault) {
            addr.setIsDefault(1);
        }
        userAddressMapper.updateById(addr);
    }

    @Override
    public void delete(Long userId, Long id) {
        // 校验归属（loadOwned 内部会抛 ADDRESS_NOT_FOUND）
        loadOwned(userId, id);
        userAddressMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long id) {
        loadOwned(userId, id);
        clearDefault(userId);
        UserAddress upd = new UserAddress();
        upd.setId(id);
        upd.setIsDefault(1);
        userAddressMapper.updateById(upd);
    }

    // -- helpers --

    private UserAddress loadOwned(Long userId, Long id) {
        UserAddress addr = userAddressMapper.selectOne(
                new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getId, id)
                        .eq(UserAddress::getUserId, userId)
        );
        if (addr == null) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return addr;
    }

    private void clearDefault(Long userId) {
        userAddressMapper.update(null,
                new LambdaUpdateWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getIsDefault, 1)
                        .set(UserAddress::getIsDefault, 0)
        );
    }

    private UserAddressVO toVO(UserAddress a) {
        UserAddressVO vo = new UserAddressVO();
        BeanUtils.copyProperties(a, vo);
        vo.setIsDefault(a.getIsDefault() != null && a.getIsDefault() == 1);
        return vo;
    }
}
