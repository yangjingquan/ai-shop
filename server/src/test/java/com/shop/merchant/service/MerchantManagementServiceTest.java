package com.shop.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.merchant.dto.CreateMerchantRequest;
import com.shop.merchant.entity.Merchant;
import com.shop.merchant.entity.MerchantUser;
import com.shop.merchant.mapper.MerchantMapper;
import com.shop.merchant.mapper.MerchantUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class MerchantManagementServiceTest {

    @Autowired
    private MerchantManagementService merchantManagementService;

    @Autowired
    private MerchantMapper merchantMapper;

    @Autowired
    private MerchantUserMapper merchantUserMapper;

    private CreateMerchantRequest sample(String name, String username) {
        CreateMerchantRequest req = new CreateMerchantRequest();
        req.setName(name);
        req.setUsername(username);
        req.setPassword("init123456");
        req.setContactName("联系人");
        req.setContactPhone("13900000000");
        return req;
    }

    @Test
    void createSuccess() {
        Long mid = merchantManagementService.createMerchant(sample("商家A", "test_m_a"), 1L);
        assertNotNull(mid);

        Merchant m = merchantMapper.selectById(mid);
        assertEquals("商家A", m.getName());

        Long userCount = merchantUserMapper.selectCount(
                new LambdaQueryWrapper<MerchantUser>().eq(MerchantUser::getMerchantId, mid)
        );
        assertEquals(1L, userCount, "应同步插入 1 条 merchant_user");
    }

    @Test
    void usernameDuplicateThrows() {
        merchantManagementService.createMerchant(sample("商家1", "dup_user_x"), 1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> merchantManagementService.createMerchant(sample("商家2", "dup_user_x"), 1L));
        assertEquals(ErrorCode.USERNAME_EXISTS.getCode(), ex.getCode());
    }
}
