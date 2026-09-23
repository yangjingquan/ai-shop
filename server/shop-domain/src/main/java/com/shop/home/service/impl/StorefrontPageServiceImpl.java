package com.shop.home.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.cache.PublicApiCacheService;
import com.shop.common.exception.BusinessException;
import com.shop.common.exception.ErrorCode;
import com.shop.home.dto.*;
import com.shop.home.entity.StorefrontPage;
import com.shop.home.mapper.StorefrontPageMapper;
import com.shop.home.service.HomeModuleConfigService;
import com.shop.home.service.StorefrontPageService;
import com.shop.product.dto.ProductListVO;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StorefrontPageServiceImpl implements StorefrontPageService {
    private static final Pattern SLUG = Pattern.compile("[a-z0-9](?:[a-z0-9-]{0,78}[a-z0-9])?");
    private static final Set<String> HOME_MODULES = Set.of("BANNER", "CATEGORY", "NEW_ARRIVALS", "POPULAR_PRODUCTS", "PRODUCT_FEED", "MARKETING_ZONE", "TOPIC_ENTRY");
    private static final Set<String> PRODUCT_SOURCES = Set.of("RECENT", "TOP_SALES", "RECOMMEND");
    private static final Set<String> TOPIC_BLOCKS = Set.of("TEXT", "IMAGE_TEXT", "PRODUCTS", "BUTTON");
    private final StorefrontPageMapper pageMapper;
    private final HomeModuleConfigService legacyModuleService;
    private final ObjectMapper objectMapper;
    private final ProductService productService;
    @Autowired(required = false) private PublicApiCacheService publicApiCacheService;

    @Override @Transactional
    public StorefrontPageVO homeEditor(Long merchantId) {
        StorefrontPage page = homePage(merchantId, true);
        return toVO(page);
    }

    @Override @Transactional
    public void saveHomeDraft(Long merchantId, StorefrontPageSaveRequest request) {
        StorefrontPage page = homePage(merchantId, true);
        validateHome(request.getDocument());
        page.setTitle(trim(request.getTitle(), 128, "首页"));
        page.setDraftJson(write(request.getDocument()));
        if ("OFFLINE".equals(page.getStatus())) page.setStatus(page.getPublishedJson() == null ? "DRAFT" : "PUBLISHED");
        pageMapper.updateById(page);
    }

    @Override @Transactional
    public void publishHome(Long merchantId) {
        StorefrontPage page = homePage(merchantId, true);
        StorefrontDocument document = read(page.getDraftJson());
        validateHome(document);
        page.setPublishedJson(write(document));
        page.setStatus("PUBLISHED");
        page.setPublishedAt(java.time.LocalDateTime.now());
        pageMapper.updateById(page);
        evictHome(merchantId);
    }

    @Override
    public List<StorefrontPageSummaryVO> listTopics(Long merchantId) {
        return pageMapper.selectList(new LambdaQueryWrapper<StorefrontPage>().eq(StorefrontPage::getMerchantId, merchantId).eq(StorefrontPage::getPageType, "TOPIC").orderByDesc(StorefrontPage::getUpdatedAt)).stream().map(this::summary).toList();
    }

    @Override
    public StorefrontPageVO topicEditor(Long merchantId, Long id) { return toVO(ownedTopic(merchantId, id)); }

    @Override @Transactional
    public StorefrontPageVO createTopic(Long merchantId, StorefrontPageSaveRequest request) {
        validateTopic(request);
        if (findSlug(merchantId, request.getSlug()) != null || findDraftSlug(merchantId, request.getSlug()) != null) throw bad("专题链接已存在，请更换短链接");
        StorefrontPage page = new StorefrontPage();
        page.setMerchantId(merchantId); page.setPageType("TOPIC"); page.setDraftSlug(normalizeSlug(request.getSlug())); page.setPageKey("TOPIC-" + normalizeSlug(request.getSlug()));
        applyTopic(page, request); page.setTitle(request.getTitle()); page.setSummary(request.getSummary()); page.setCoverImage(request.getCoverImage()); page.setStatus("DRAFT"); page.setDraftJson(write(request.getDocument())); pageMapper.insert(page);
        return toVO(page);
    }

    @Override @Transactional
    public StorefrontPageVO updateTopic(Long merchantId, Long id, StorefrontPageSaveRequest request) {
        validateTopic(request);
        StorefrontPage page = ownedTopic(merchantId, id);
        String slug = normalizeSlug(request.getSlug());
        StorefrontPage collision = findSlug(merchantId, slug);
        StorefrontPage draftCollision = findDraftSlug(merchantId, slug);
        if ((collision != null && !Objects.equals(collision.getId(), id)) || (draftCollision != null && !Objects.equals(draftCollision.getId(), id))) throw bad("专题链接已存在，请更换短链接");
        page.setDraftSlug(slug); page.setPageKey("TOPIC-" + slug); applyTopic(page, request); page.setDraftJson(write(request.getDocument()));
        pageMapper.updateById(page); evictHome(merchantId);
        return toVO(page);
    }

    @Override @Transactional
    public void publishTopic(Long merchantId, Long id) {
        StorefrontPage page = ownedTopic(merchantId, id);
        StorefrontDocument document = read(page.getDraftJson()); validateTopicDocument(document);
        if (document.getBlocks().isEmpty()) throw bad("专题至少添加一个内容模块后才能发布");
        StorefrontPage collision = findSlug(merchantId, page.getDraftSlug());
        if (collision != null && !Objects.equals(collision.getId(), id)) throw bad("专题链接已被其他已发布页面占用，请更换短链接");
        page.setSlug(page.getDraftSlug()); page.setTitle(page.getDraftTitle()); page.setSummary(page.getDraftSummary()); page.setCoverImage(page.getDraftCoverImage()); page.setPublishedJson(write(document)); page.setStatus("PUBLISHED"); page.setPublishedAt(java.time.LocalDateTime.now()); pageMapper.updateById(page); evictHome(merchantId);
    }

    @Override @Transactional
    public void offlineTopic(Long merchantId, Long id) {
        StorefrontPage page = ownedTopic(merchantId, id); page.setStatus("OFFLINE"); pageMapper.updateById(page); evictHome(merchantId);
    }

    @Override
    public StorefrontPageVO publicTopic(Long merchantId, String slug) {
        StorefrontPage page = findSlug(merchantId, normalizeSlug(slug));
        if (page == null || !"PUBLISHED".equals(page.getStatus()) || page.getPublishedJson() == null) return null;
        StorefrontPageVO result = toVO(page);
        result.setTitle(page.getTitle()); result.setSlug(page.getSlug()); result.setSummary(page.getSummary()); result.setCoverImage(page.getCoverImage());
        StorefrontDocument content = result.getPublished();
        if (content != null) content.getBlocks().forEach(block -> { if ("PRODUCTS".equals(block.getType())) block.setProducts(loadProducts(merchantId, block.getProductSource(), block.getProductLimit())); });
        result.setPublished(content);
        return result;
    }

    @Override
    public List<StorefrontTemplateVO> templates() {
        return List.of(template("PRODUCT_FOCUS", "商品主推", "新品与热销商品优先，适合日常经营", homeTemplate(List.of("BANNER", "CATEGORY", "NEW_ARRIVALS", "POPULAR_PRODUCTS", "PRODUCT_FEED", "MARKETING_ZONE"))),
                template("PROMOTION_FIRST", "活动转化", "先呈现营销活动，再承接商品浏览", homeTemplate(List.of("BANNER", "MARKETING_ZONE", "CATEGORY", "POPULAR_PRODUCTS", "PRODUCT_FEED"))),
                template("CONTENT_STORY", "内容专题", "首页突出专题入口与商品精选", homeTemplate(List.of("BANNER", "CATEGORY", "TOPIC_ENTRY", "NEW_ARRIVALS", "PRODUCT_FEED"))));
    }

    @Override
    public List<HomeModuleVO> publishedHomeModules(Long merchantId) {
        StorefrontPage page = homePage(merchantId, false);
        if (page == null || page.getPublishedJson() == null || !"PUBLISHED".equals(page.getStatus())) return legacyModules(merchantId, true);
        StorefrontDocument published = read(page.getPublishedJson());
        List<HomeModuleVO> result = moduleVOs(published.getModules(), true);
        result.stream().filter(item -> "TOPIC_ENTRY".equals(item.getCode())).forEach(item -> {
            List<Long> ids = Optional.ofNullable(published.getModules()).orElse(List.of()).stream().filter(module -> Objects.equals(module.getId(), item.getId())).findFirst().map(StorefrontDocument.Module::getTopicPageIds).orElse(List.of());
            item.setTopicPages(publishedTopics(merchantId, ids));
        });
        return result;
    }

    @Override
    public List<StorefrontPageSummaryVO> publishedTopics(Long merchantId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        Map<Long, StorefrontPageSummaryVO> found = pageMapper.selectList(new LambdaQueryWrapper<StorefrontPage>().eq(StorefrontPage::getMerchantId, merchantId).eq(StorefrontPage::getPageType, "TOPIC").eq(StorefrontPage::getStatus, "PUBLISHED").isNotNull(StorefrontPage::getPublishedJson).in(StorefrontPage::getId, ids)).stream().map(this::publishedSummary).collect(Collectors.toMap(StorefrontPageSummaryVO::getId, item -> item));
        return ids.stream().map(found::get).filter(Objects::nonNull).toList();
    }

    private StorefrontPage homePage(Long merchantId, boolean create) { StorefrontPage page = pageMapper.selectOne(new LambdaQueryWrapper<StorefrontPage>().eq(StorefrontPage::getMerchantId, merchantId).eq(StorefrontPage::getPageType, "HOME").eq(StorefrontPage::getPageKey, "HOME")); if (page != null || !create) return page; StorefrontDocument document = new StorefrontDocument(); document.setModules(legacyModuleService.list(merchantId).stream().map(this::fromLegacy).toList()); page = new StorefrontPage(); page.setMerchantId(merchantId); page.setPageType("HOME"); page.setPageKey("HOME"); page.setTitle("首页"); page.setStatus("PUBLISHED"); page.setDraftJson(write(document)); page.setPublishedJson(write(document)); page.setPublishedAt(java.time.LocalDateTime.now()); try { pageMapper.insert(page); } catch (org.springframework.dao.DuplicateKeyException race) { return pageMapper.selectOne(new LambdaQueryWrapper<StorefrontPage>().eq(StorefrontPage::getMerchantId, merchantId).eq(StorefrontPage::getPageType, "HOME").eq(StorefrontPage::getPageKey, "HOME")); } return page; }
    private List<HomeModuleVO> legacyModules(Long merchantId, boolean enabledOnly) { return legacyModuleService.list(merchantId).stream().filter(item -> !enabledOnly || Integer.valueOf(1).equals(item.getEnabled())).toList(); }
    private StorefrontDocument.Module fromLegacy(HomeModuleVO legacy) { StorefrontDocument.Module m = new StorefrontDocument.Module(); m.setId(UUID.randomUUID().toString()); m.setCode(legacy.getCode()); m.setTitle(legacy.getTitle()); m.setSubtitle(legacy.getSubtitle()); m.setEnabled(legacy.getEnabled()); m.setSortOrder(legacy.getSortOrder()); m.setProductSource(legacy.getProductSource()); m.setProductLimit(legacy.getProductLimit()); return m; }
    private List<HomeModuleVO> moduleVOs(List<StorefrontDocument.Module> modules, boolean enabledOnly) { if (modules == null) return List.of(); return modules.stream().filter(m -> !enabledOnly || Integer.valueOf(1).equals(m.getEnabled())).sorted(Comparator.comparing(m -> Optional.ofNullable(m.getSortOrder()).orElse(0))).map(m -> { HomeModuleVO vo = new HomeModuleVO(); vo.setId(m.getId()); vo.setCode(m.getCode()); vo.setName(moduleName(m.getCode())); vo.setDescription(moduleDescription(m.getCode())); vo.setTitle(m.getTitle()); vo.setSubtitle(m.getSubtitle()); vo.setEnabled(m.getEnabled()); vo.setSortOrder(m.getSortOrder()); vo.setProductSource(m.getProductSource()); vo.setProductLimit(m.getProductLimit()); return vo; }).toList(); }
    private StorefrontPage ownedTopic(Long merchantId, Long id) { StorefrontPage page = pageMapper.selectOne(new LambdaQueryWrapper<StorefrontPage>().eq(StorefrontPage::getId, id).eq(StorefrontPage::getMerchantId, merchantId).eq(StorefrontPage::getPageType, "TOPIC")); if (page == null) throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "专题页不存在"); return page; }
    private StorefrontPage findSlug(Long merchantId, String slug) { return pageMapper.selectOne(new LambdaQueryWrapper<StorefrontPage>().eq(StorefrontPage::getMerchantId, merchantId).eq(StorefrontPage::getPageType, "TOPIC").eq(StorefrontPage::getSlug, slug)); }
    private StorefrontPage findDraftSlug(Long merchantId, String slug) { return pageMapper.selectOne(new LambdaQueryWrapper<StorefrontPage>().eq(StorefrontPage::getMerchantId, merchantId).eq(StorefrontPage::getPageType, "TOPIC").eq(StorefrontPage::getDraftSlug, normalizeSlug(slug))); }
    private void validateHome(StorefrontDocument document) {
        if (document == null || document.getModules() == null || document.getModules().isEmpty() || document.getModules().size() > 24) throw bad("首页模块数量需在 1 至 24 个之间");
        Set<String> ids = new HashSet<>(); Set<Integer> orders = new HashSet<>(); Map<String, Integer> counts = new HashMap<>(); boolean anyEnabled = false;
        for (StorefrontDocument.Module module : document.getModules()) {
            if (module == null || module.getId() == null || module.getId().isBlank() || !ids.add(module.getId())) throw bad("首页模块标识缺失或重复");
            if (module.getCode() == null || !HOME_MODULES.contains(module.getCode())) throw bad("首页模块类型无效");
            counts.merge(module.getCode(), 1, Integer::sum);
            if (Set.of("BANNER", "CATEGORY", "MARKETING_ZONE").contains(module.getCode()) && counts.get(module.getCode()) > 1) throw bad("Banner、分类和营销会场模块只能添加一次");
            int enabled = module.getEnabled() == null ? 1 : module.getEnabled();
            if (enabled < 0 || enabled > 1) throw bad("模块展示状态无效");
            module.setEnabled(enabled); anyEnabled |= enabled == 1;
            module.setSortOrder(module.getSortOrder() == null ? 0 : module.getSortOrder());
            if (!orders.add(module.getSortOrder())) throw bad("首页模块顺序不能重复");
            if (module.getCode().contains("PRODUCT") || "NEW_ARRIVALS".equals(module.getCode())) {
                if (!PRODUCT_SOURCES.contains(module.getProductSource())) throw bad("商品来源无效");
                if (module.getProductLimit() == null || module.getProductLimit() < 1 || module.getProductLimit() > 20) throw bad("商品数量需在 1 至 20 件之间");
            }
            module.setTitle(trim(module.getTitle(), 64, moduleName(module.getCode()))); module.setSubtitle(trim(module.getSubtitle(), 128, ""));
            if ("TOPIC_ENTRY".equals(module.getCode()) && module.getTopicPageIds() != null && module.getTopicPageIds().size() > 20) throw bad("专题入口最多配置 20 个页面");
        }
        if (!anyEnabled) throw bad("首页至少保留一个已启用模块");
    }
    private void validateTopic(StorefrontPageSaveRequest request) { if (request == null) throw bad("专题内容不能为空"); request.setTitle(trim(request.getTitle(), 128, "")); if (request.getTitle().isBlank()) throw bad("专题标题不能为空"); String slug = normalizeSlug(request.getSlug()); if (!SLUG.matcher(slug).matches()) throw bad("专题短链接仅支持小写字母、数字和连字符"); request.setSlug(slug); request.setSummary(trim(request.getSummary(), 256, "")); request.setCoverImage(safeImage(request.getCoverImage())); validateTopicDocument(request.getDocument()); }
    private void validateTopicDocument(StorefrontDocument document) { if (document == null || document.getBlocks() == null || document.getBlocks().size() > 40) throw bad("专题内容模块最多 40 个"); Set<String> ids = new HashSet<>(); for (StorefrontDocument.Block block : document.getBlocks()) { if (block == null || block.getId() == null || block.getId().isBlank() || !ids.add(block.getId())) throw bad("专题内容模块标识缺失或重复"); if (block.getType() == null || !TOPIC_BLOCKS.contains(block.getType())) throw bad("专题内容模块类型无效"); block.setTitle(trim(block.getTitle(), 100, "")); block.setBody(trim(block.getBody(), 4000, "")); block.setImageUrl(safeImage(block.getImageUrl())); block.setButtonText(trim(block.getButtonText(), 24, "查看详情")); block.setProducts(new ArrayList<>()); if ("BUTTON".equals(block.getType())) { if (block.getLinkType() == null || !Set.of("PRODUCTS", "CATEGORY", "COUPON", "GROUP_BUY").contains(block.getLinkType())) throw bad("专题按钮跳转类型无效"); } if ("PRODUCTS".equals(block.getType())) { if (!PRODUCT_SOURCES.contains(block.getProductSource())) throw bad("专题商品来源无效"); if (block.getProductLimit() == null || block.getProductLimit() < 1 || block.getProductLimit() > 20) throw bad("专题商品数量需在 1 至 20 件之间"); } } }
    private void applyTopic(StorefrontPage page, StorefrontPageSaveRequest request) { page.setDraftTitle(request.getTitle()); page.setDraftSummary(request.getSummary() == null ? "" : request.getSummary()); page.setDraftCoverImage(request.getCoverImage() == null ? "" : request.getCoverImage()); }
    private StorefrontPageVO toVO(StorefrontPage page) { StorefrontPageVO vo = new StorefrontPageVO(); vo.setId(page.getId()); vo.setPageType(page.getPageType()); vo.setTitle(page.getDraftTitle() == null ? page.getTitle() : page.getDraftTitle()); vo.setSlug(page.getDraftSlug() == null ? page.getSlug() : page.getDraftSlug()); vo.setSummary(page.getDraftSummary() == null ? page.getSummary() : page.getDraftSummary()); vo.setCoverImage(page.getDraftCoverImage() == null ? page.getCoverImage() : page.getDraftCoverImage()); vo.setStatus(page.getStatus()); vo.setPublishedAt(page.getPublishedAt()); vo.setDraft(read(page.getDraftJson())); vo.setPublished(read(page.getPublishedJson())); vo.setDraftChanged(page.getPublishedJson() == null || !Objects.equals(page.getDraftJson(), page.getPublishedJson()) || !Objects.equals(page.getDraftSlug(), page.getSlug()) || !Objects.equals(page.getDraftTitle(), page.getTitle()) || !Objects.equals(page.getDraftSummary(), page.getSummary()) || !Objects.equals(page.getDraftCoverImage(), page.getCoverImage())); return vo; }
    private StorefrontPageSummaryVO summary(StorefrontPage page) { StorefrontPageSummaryVO vo = new StorefrontPageSummaryVO(); vo.setId(page.getId()); vo.setTitle(page.getDraftTitle() == null ? page.getTitle() : page.getDraftTitle()); vo.setSlug(page.getDraftSlug() == null ? page.getSlug() : page.getDraftSlug()); vo.setSummary(page.getDraftSummary() == null ? page.getSummary() : page.getDraftSummary()); vo.setCoverImage(page.getDraftCoverImage() == null ? page.getCoverImage() : page.getDraftCoverImage()); vo.setStatus(page.getStatus()); vo.setUpdatedAt(page.getUpdatedAt()); vo.setPublishedAt(page.getPublishedAt()); return vo; }
    private StorefrontPageSummaryVO publishedSummary(StorefrontPage page) { StorefrontPageSummaryVO vo = new StorefrontPageSummaryVO(); vo.setId(page.getId()); vo.setTitle(page.getTitle()); vo.setSlug(page.getSlug()); vo.setSummary(page.getSummary()); vo.setCoverImage(page.getCoverImage()); vo.setStatus(page.getStatus()); vo.setUpdatedAt(page.getUpdatedAt()); vo.setPublishedAt(page.getPublishedAt()); return vo; }
    private StorefrontTemplateVO template(String code, String name, String description, StorefrontDocument document) { StorefrontTemplateVO vo = new StorefrontTemplateVO(); vo.setCode(code); vo.setName(name); vo.setDescription(description); vo.setPageType("HOME"); vo.setDocument(document); return vo; }
    private StorefrontDocument homeTemplate(List<String> codes) { StorefrontDocument document = new StorefrontDocument(); List<StorefrontDocument.Module> modules = new ArrayList<>(); int sort = 10; for (String code : codes) { HomeModuleVO def = legacyModuleService.list(0L).stream().filter(x -> x.getCode().equals(code)).findFirst().orElse(null); StorefrontDocument.Module module = new StorefrontDocument.Module(); module.setId(UUID.randomUUID().toString()); module.setCode(code); module.setTitle(def == null ? moduleName(code) : def.getTitle()); module.setSubtitle(def == null ? "" : def.getSubtitle()); module.setEnabled(1); module.setSortOrder(sort); sort += 10; if (def != null) { module.setProductSource(def.getProductSource()); module.setProductLimit(def.getProductLimit()); } if ("TOPIC_ENTRY".equals(code)) { module.setTitle("精选专题"); module.setTopicPageIds(new ArrayList<>()); } modules.add(module); } document.setModules(modules); return document; }
    private List<ProductListVO> loadProducts(Long merchantId, String source, Integer limit) { int size = limit == null ? 8 : Math.min(20, Math.max(1, limit)); return switch (source == null ? "" : source) { case "RECENT" -> productService.publicPage(1, size, merchantId, null, null, null, null).getList(); case "TOP_SALES" -> productService.publicTopSalesPage(1, size, merchantId, null).getList(); case "RECOMMEND" -> productService.publicPage(1, size, merchantId, null, null, 1, null).getList(); default -> List.of(); }; }
    private String moduleName(String code) { return switch (code) { case "BANNER" -> "焦点 Banner"; case "CATEGORY" -> "分类入口"; case "NEW_ARRIVALS" -> "新品速览"; case "POPULAR_PRODUCTS" -> "人气推荐"; case "PRODUCT_FEED" -> "商品精选流"; case "MARKETING_ZONE" -> "营销会场"; case "TOPIC_ENTRY" -> "专题入口"; default -> "页面模块"; }; }
    private String moduleDescription(String code) { return switch (code) { case "BANNER" -> "首页主视觉与运营活动"; case "CATEGORY" -> "快速进入店铺分类"; case "NEW_ARRIVALS" -> "按商品添加时间展示"; case "POPULAR_PRODUCTS" -> "横向展示热销或推荐商品"; case "PRODUCT_FEED" -> "双列连续浏览商品"; case "MARKETING_ZONE" -> "预售、积分、秒杀、团购等活动入口"; case "TOPIC_ENTRY" -> "进入店铺专题内容"; default -> ""; }; }
    private String safeImage(String url) { String value = trim(url, 512, ""); return value.isEmpty() || value.startsWith("/uploads/") || value.startsWith("https://") ? value : throwBadImage(); }
    private String throwBadImage() { throw bad("图片地址仅支持店铺上传资源或 HTTPS 链接"); }
    private String normalizeSlug(String slug) { return slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT); }
    private String trim(String value, int max, String fallback) { String result = value == null ? fallback : value.trim(); return result.length() > max ? result.substring(0, max) : result; }
    private String write(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception ex) { throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(), "页面内容保存失败"); } }
    private StorefrontDocument read(String json) { try { return json == null || json.isBlank() ? new StorefrontDocument() : objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception ex) { throw new BusinessException(ErrorCode.BIZ_ERROR.getCode(), "页面内容无法解析，请重新保存"); } }
    private BusinessException bad(String message) { return new BusinessException(ErrorCode.PARAM_ERROR.getCode(), message); }
    private void evictHome(Long merchantId) { if (publicApiCacheService != null) publicApiCacheService.evictHome(merchantId); }
}
