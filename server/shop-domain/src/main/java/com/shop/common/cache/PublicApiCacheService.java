package com.shop.common.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.response.PageResult;
import com.shop.banner.dto.BannerVO;
import com.shop.home.dto.HomeVO;
import com.shop.order.dto.OrderListVO;
import com.shop.product.dto.MerchantCategoryVO;
import com.shop.product.dto.ProductListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;

/**
 * Public read-model cache. Versions make all pages for one merchant/user stale at once,
 * without issuing a costly Redis KEYS/SCAN deletion on every write.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PublicApiCacheService {
    private static final Duration HOME_TTL = Duration.ofSeconds(60);
    private static final Duration BANNER_TTL = Duration.ofSeconds(60);
    private static final Duration CATEGORY_TTL = Duration.ofMinutes(5);
    private static final Duration PRODUCT_PAGE_TTL = Duration.ofSeconds(60);
    private static final Duration ORDER_PAGE_TTL = Duration.ofSeconds(20);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public HomeVO home(Long merchantId, Supplier<HomeVO> loader) {
        String merchant = scope(merchantId);
        String key = "cache:public:home:v" + version("cache:public:home:global:version")
                + ":" + version("cache:public:home:version:" + merchant) + ":" + merchant;
        return getOrLoad(key, HOME_TTL, new TypeReference<>() {}, loader);
    }

    public List<BannerVO> banners(Long merchantId, Supplier<List<BannerVO>> loader) {
        String merchant = scope(merchantId);
        String key = "cache:public:banner:v" + version("cache:public:banner:global:version")
                + ":" + version("cache:public:banner:version:" + merchant) + ":" + merchant;
        return getOrLoad(key, BANNER_TTL, new TypeReference<>() {}, loader);
    }

    public List<MerchantCategoryVO> categoryTree(Long merchantId, Supplier<List<MerchantCategoryVO>> loader) {
        String merchant = scope(merchantId);
        String key = "cache:public:category-tree:v" + version("cache:public:category-tree:version:" + merchant)
                + ":" + merchant;
        return getOrLoad(key, CATEGORY_TTL, new TypeReference<>() {}, loader);
    }

    public PageResult<ProductListVO> productPage(Long merchantId, int page, int size, Long categoryId,
                                                   String keyword, Integer isRecommend, Integer isGroupBuy,
                                                   Supplier<PageResult<ProductListVO>> loader) {
        String merchant = scope(merchantId);
        String params = page + ":" + size + ":" + nullable(categoryId) + ":" + normalized(keyword)
                + ":" + nullable(isRecommend) + ":" + nullable(isGroupBuy);
        String key = "cache:public:product-page:v" + version("cache:public:product-page:version:" + merchant)
                + ":" + merchant + ":" + encode(params);
        return getOrLoad(key, PRODUCT_PAGE_TTL, new TypeReference<>() {}, loader);
    }

    public PageResult<OrderListVO> orderPage(Long userId, int page, int size, Integer status,
                                               Supplier<PageResult<OrderListVO>> loader) {
        String user = scope(userId);
        String key = "cache:wx:order-page:v" + version("cache:wx:order-page:version:" + user)
                + ":" + user + ":" + page + ":" + size + ":" + nullable(status);
        return getOrLoad(key, ORDER_PAGE_TTL, new TypeReference<>() {}, loader);
    }

    public PageResult<OrderListVO> merchantOrderPage(Long merchantId, int page, int size, Integer status,
                                                       String scope, Supplier<PageResult<OrderListVO>> loader) {
        String merchant = scope(merchantId);
        String key = "cache:merchant:order-page:v" + version("cache:merchant:order-page:version:" + merchant)
                + ":" + merchant + ":" + encode(page + ":" + size + ":" + nullable(status) + ":" + normalized(scope));
        return getOrLoad(key, ORDER_PAGE_TTL, new TypeReference<>() {}, loader);
    }

    public PageResult<OrderListVO> adminOrderPage(int page, int size, Integer status, Long merchantId,
                                                    String orderNo, Object createdFrom, Object createdTo,
                                                    Supplier<PageResult<OrderListVO>> loader) {
        String key = "cache:admin:order-page:v" + version("cache:admin:order-page:version") + ":"
                + encode(page + ":" + size + ":" + nullable(status) + ":" + nullable(merchantId) + ":"
                + normalized(orderNo) + ":" + nullable(createdFrom) + ":" + nullable(createdTo));
        return getOrLoad(key, ORDER_PAGE_TTL, new TypeReference<>() {}, loader);
    }

    public void evictHome(Long merchantId) {
        afterCommit(() -> bump("cache:public:home:version:" + scope(merchantId)));
    }

    public void evictAllHomes() {
        afterCommit(() -> bump("cache:public:home:global:version"));
    }

    public void evictBanners(Long merchantId) {
        afterCommit(() -> bump("cache:public:banner:version:" + scope(merchantId)));
    }

    public void evictAllBanners() {
        afterCommit(() -> bump("cache:public:banner:global:version"));
    }

    public void evictCategoriesAndProducts(Long merchantId) {
        afterCommit(() -> {
            String merchant = scope(merchantId);
            bump("cache:public:category-tree:version:" + merchant);
            bump("cache:public:product-page:version:" + merchant);
            bump("cache:public:home:version:" + merchant);
        });
    }

    public void evictProductsAndHome(Long merchantId) {
        afterCommit(() -> {
            String merchant = scope(merchantId);
            bump("cache:public:product-page:version:" + merchant);
            bump("cache:public:home:version:" + merchant);
        });
    }

    public void evictOrderPages(Long userId) {
        if (userId != null) {
            afterCommit(() -> bump("cache:wx:order-page:version:" + userId));
        }
    }

    public void evictOrderPages(Long userId, Long merchantId) {
        afterCommit(() -> {
            if (userId != null) {
                bump("cache:wx:order-page:version:" + userId);
            }
            if (merchantId != null) {
                bump("cache:merchant:order-page:version:" + merchantId);
            }
            bump("cache:admin:order-page:version");
        });
    }

    private <T> T getOrLoad(String key, Duration ttl, TypeReference<T> type, Supplier<T> loader) {
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, type);
            }
        } catch (Exception ex) {
            log.warn("读取公开接口缓存失败，降级查询数据库 key={}", key, ex);
        }
        T value = loader.get();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.warn("写入公开接口缓存失败 key={}", key, ex);
        }
        return value;
    }

    private String version(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? "0" : value;
        } catch (RuntimeException ex) {
            log.warn("读取缓存版本失败，跳过缓存 key={}", key, ex);
            return "0";
        }
    }

    private void bump(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (RuntimeException ex) {
            log.warn("更新缓存版本失败 key={}", key, ex);
        }
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
            return;
        }
        action.run();
    }

    private String scope(Long id) { return id == null ? "global" : String.valueOf(id); }
    private String nullable(Object value) { return value == null ? "_" : String.valueOf(value); }
    private String normalized(String value) { return value == null ? "_" : value.trim(); }
    private String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
