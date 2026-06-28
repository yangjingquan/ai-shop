package com.shop.banner;

import com.shop.banner.dto.BannerSaveRequest;
import com.shop.banner.dto.BannerVO;
import com.shop.banner.service.BannerService;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.common.response.PageResult;
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
class BannerServiceTest {

    @Autowired
    private BannerService bannerService;

    @Test
    void createAndList() {
        BannerSaveRequest req = new BannerSaveRequest();
        req.setImageUrl("/img/test.jpg");
        req.setLinkType(1);
        req.setLinkValue("3");
        req.setSort(1);
        req.setStatus(1);
        Long id = bannerService.create(req);
        assertNotNull(id);

        PageResult<BannerVO> page = bannerService.page(1, 10);
        assertTrue(page.getTotal() >= 1);
    }

    @Test
    void listActiveOnlyReturnsStatusOne() {
        List<BannerVO> active = bannerService.listActive();
        for (BannerVO b : active) {
            assertEquals(1, b.getStatus());
        }
    }

    @Test
    void updateThenDelete() {
        BannerSaveRequest req = new BannerSaveRequest();
        req.setImageUrl("/img/old.jpg");
        req.setLinkType(0);
        req.setLinkValue("");
        req.setSort(2);
        req.setStatus(1);
        Long id = bannerService.create(req);

        req.setImageUrl("/img/new.jpg");
        bannerService.update(id, req);

        bannerService.delete(id);
        assertThrows(BusinessException.class, () -> bannerService.update(id, req));
    }
}
