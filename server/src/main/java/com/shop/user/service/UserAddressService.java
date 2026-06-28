package com.shop.user.service;

import com.shop.user.dto.UserAddressRequest;
import com.shop.user.dto.UserAddressVO;

import java.util.List;

public interface UserAddressService {
    List<UserAddressVO> list(Long userId);

    UserAddressVO get(Long userId, Long id);

    Long create(Long userId, UserAddressRequest req);

    void update(Long userId, Long id, UserAddressRequest req);

    void delete(Long userId, Long id);

    void setDefault(Long userId, Long id);
}
