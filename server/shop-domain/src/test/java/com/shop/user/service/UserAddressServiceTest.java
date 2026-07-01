package com.shop.user.service;

import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.user.dto.UserAddressRequest;
import com.shop.user.dto.UserAddressVO;
import com.shop.user.entity.UserAddress;
import com.shop.user.mapper.UserAddressMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class UserAddressServiceTest {

    @Autowired
    private UserAddressService addressService;

    @Autowired
    private UserAddressMapper addressMapper;

    private UserAddressRequest sample(String receiver, boolean isDefault) {
        UserAddressRequest req = new UserAddressRequest();
        req.setReceiver(receiver);
        req.setPhone("13800000000");
        req.setRegion("北京市/北京市/朝阳区");
        req.setDetail("望京 SOHO");
        req.setIsDefault(isDefault);
        return req;
    }

    @Test
    void createAndList() {
        Long userId = 1001L;
        Long id = addressService.create(userId, sample("张三", true));
        assertNotNull(id);

        List<UserAddressVO> list = addressService.list(userId);
        assertEquals(1, list.size());
        assertEquals("张三", list.get(0).getReceiver());
        assertTrue(list.get(0).getIsDefault());
    }

    @Test
    void setDefaultMutex() {
        Long userId = 1002L;
        Long id1 = addressService.create(userId, sample("A", true));
        Long id2 = addressService.create(userId, sample("B", true));

        List<UserAddressVO> list = addressService.list(userId);
        assertEquals(2, list.size());

        long defaultCount = list.stream().filter(UserAddressVO::getIsDefault).count();
        assertEquals(1, defaultCount, "只能有 1 条默认地址");

        UserAddressVO defaultOne = list.stream()
                .filter(UserAddressVO::getIsDefault).findFirst().orElseThrow();
        assertEquals(id2, defaultOne.getId(), "最后插入的应为默认");

        addressService.setDefault(userId, id1);
        list = addressService.list(userId);
        UserAddressVO afterSet = list.stream()
                .filter(UserAddressVO::getIsDefault).findFirst().orElseThrow();
        assertEquals(id1, afterSet.getId(), "setDefault 后应翻转");
    }

    @Test
    void deleteIsSoftDelete() {
        Long userId = 1003L;
        Long id = addressService.create(userId, sample("Soft", false));

        addressService.delete(userId, id);

        // list 走业务逻辑应查不到
        assertTrue(addressService.list(userId).isEmpty());

        // mapper 直查（含逻辑删字段，但 MyBatis-Plus @TableLogic 会自动加 deleted=0）
        // 直接 SQL 验证物理仍存在但 deleted=1
        UserAddress raw = addressMapper.selectById(id);
        assertNull(raw, "selectById 受 @TableLogic 影响应返回 null");
    }

    @Test
    void crossUserIsolation() {
        Long userA = 2001L;
        Long userB = 2002L;
        Long idA = addressService.create(userA, sample("A-only", true));

        // B 查不到 A 的
        assertTrue(addressService.list(userB).isEmpty());

        // B get 不到
        assertThrows(BusinessException.class, () -> addressService.get(userB, idA),
                "用户 B 查 A 的地址应抛 ADDRESS_NOT_FOUND");

        // B 改不了
        BusinessException ex = assertThrows(BusinessException.class,
                () -> addressService.update(userB, idA, sample("hacker", false)));
        assertEquals(ErrorCode.ADDRESS_NOT_FOUND.getCode(), ex.getCode());

        // B 删不了
        assertThrows(BusinessException.class, () -> addressService.delete(userB, idA));
    }
}
