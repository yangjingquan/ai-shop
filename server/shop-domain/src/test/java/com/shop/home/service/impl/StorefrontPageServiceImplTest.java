package com.shop.home.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.exception.BusinessException;
import com.shop.home.dto.StorefrontDocument;
import com.shop.home.dto.StorefrontPageSaveRequest;
import com.shop.home.entity.StorefrontPage;
import com.shop.home.mapper.StorefrontPageMapper;
import com.shop.home.service.HomeModuleConfigService;
import com.shop.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorefrontPageServiceImplTest {
    @Mock private StorefrontPageMapper pageMapper;
    @Mock private HomeModuleConfigService legacyModuleService;
    @Mock private ProductService productService;
    private StorefrontPageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new StorefrontPageServiceImpl(pageMapper, legacyModuleService, new ObjectMapper(), productService);
    }

    @Test
    void topicsHaveBuiltInTemplatesAndRequireValidDocument() {
        assertEquals(List.of("PRODUCT_FOCUS", "PROMOTION_FIRST", "CONTENT_STORY"),
                service.templates().stream().map(item -> item.getCode()).toList());

        StorefrontPage page = topic(1L, "spring", "DRAFT", "spring", new StorefrontDocument());
        when(pageMapper.selectOne(any(Wrapper.class))).thenReturn(page);
        assertThrows(BusinessException.class, () -> service.publishTopic(2L, 1L));
        verify(pageMapper, never()).updateById(any());
    }

    @Test
    void homepageDraftOnlyGoesLiveWhenPublishedAndModulesStayOrdered() {
        StorefrontDocument firstDraft = homeDocument(module("banner", "BANNER", 20, 1), module("feed", "PRODUCT_FEED", 10, 1));
        StorefrontDocument published = homeDocument(module("banner", "BANNER", 10, 1), module("feed", "PRODUCT_FEED", 20, 1));
        StorefrontPage page = new StorefrontPage(); page.setId(2L); page.setMerchantId(2L); page.setPageType("HOME");
        page.setPageKey("HOME"); page.setStatus("PUBLISHED"); page.setDraftJson(json(published)); page.setPublishedJson(json(published));
        when(pageMapper.selectOne(any(Wrapper.class))).thenReturn(page);

        service.saveHomeDraft(2L, request("首页", firstDraft));
        assertEquals("BANNER", service.publishedHomeModules(2L).get(0).getCode());
        assertNotEquals(page.getDraftJson(), page.getPublishedJson());

        service.publishHome(2L);
        assertEquals(List.of("PRODUCT_FEED", "BANNER"), service.publishedHomeModules(2L).stream().map(item -> item.getCode()).toList());
        verify(pageMapper, times(2)).updateById(page);
    }

    @Test
    void homepageRejectsEmptyAllDisabledOrAmbiguousModules() {
        StorefrontPage page = new StorefrontPage(); page.setId(2L); page.setMerchantId(2L); page.setPageType("HOME");
        page.setPageKey("HOME"); page.setStatus("PUBLISHED"); page.setDraftJson(json(homeDocument(module("banner", "BANNER", 10, 1))));
        when(pageMapper.selectOne(any(Wrapper.class))).thenReturn(page);

        assertThrows(BusinessException.class, () -> service.saveHomeDraft(2L, request("首页", new StorefrontDocument())));
        assertThrows(BusinessException.class, () -> service.saveHomeDraft(2L, request("首页", homeDocument(module("banner", "BANNER", 10, 0)))));
        assertThrows(BusinessException.class, () -> service.saveHomeDraft(2L, request("首页", homeDocument(module("banner", "BANNER", 10, 1), module("category", "CATEGORY", 10, 1)))));
        verify(pageMapper, never()).updateById(any());
    }

    @Test
    void createTopicKeepsNewSlugInDraftUntilExplicitPublish() {
        when(pageMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(pageMapper.insert(any())).thenAnswer(invocation -> {
            ((StorefrontPage) invocation.getArgument(0)).setId(11L);
            return 1;
        });

        var created = service.createTopic(2L, request("spring", documentWithTextBlock()));

        assertEquals("spring", created.getSlug());
        assertEquals("DRAFT", created.getStatus());
        assertNotNull(created.getDraft());
        verify(pageMapper).insert(argThat(page -> page.getSlug() == null && "spring".equals(page.getDraftSlug())));
    }

    @Test
    void editingOfflineTopicDoesNotRepublishOrChangePublicSlug() {
        StorefrontPage page = topic(5L, "old-url", "OFFLINE", "new-url", documentWithTextBlock());
        when(pageMapper.selectOne(any(Wrapper.class))).thenReturn(page);

        service.updateTopic(2L, 5L, request("new-url", documentWithTextBlock()));

        assertEquals("OFFLINE", page.getStatus());
        assertEquals("old-url", page.getSlug());
        assertEquals("new-url", page.getDraftSlug());
        verify(pageMapper).updateById(page);
    }

    @Test
    void savingPublishedTopicMetadataKeepsPublishedSnapshotUntilPublish() {
        StorefrontPage page = topic(5L, "old-url", "PUBLISHED", "old-url", documentWithTextBlock());
        page.setTitle("线上标题"); page.setDraftTitle("线上标题");
        page.setSummary("线上简介"); page.setDraftSummary("线上简介");
        page.setCoverImage("/uploads/old.jpg"); page.setDraftCoverImage("/uploads/old.jpg");
        when(pageMapper.selectOne(any(Wrapper.class))).thenReturn(page);
        when(pageMapper.selectList(any(Wrapper.class))).thenReturn(List.of(page));

        StorefrontPageSaveRequest changed = request("old-url", documentWithTextBlock());
        changed.setTitle("新草稿标题"); changed.setSummary("新草稿简介"); changed.setCoverImage("/uploads/new.jpg");
        var draft = service.updateTopic(2L, 5L, changed);
        var publicPage = service.publicTopic(2L, "old-url");
        var homeCard = service.publishedTopics(2L, List.of(5L)).get(0);

        assertEquals("新草稿标题", draft.getTitle());
        assertEquals("线上标题", publicPage.getTitle());
        assertEquals("线上简介", publicPage.getSummary());
        assertEquals("/uploads/old.jpg", publicPage.getCoverImage());
        assertEquals("线上标题", homeCard.getTitle());
        assertEquals("/uploads/old.jpg", homeCard.getCoverImage());
    }

    @Test
    void publishMakesDraftSlugAndContentPublicAndOfflineHidesIt() {
        StorefrontPage page = topic(5L, "old-url", "OFFLINE", "new-url", documentWithTextBlock());
        when(pageMapper.selectOne(any(Wrapper.class))).thenReturn(page);

        service.publishTopic(2L, 5L);

        assertEquals("PUBLISHED", page.getStatus());
        assertEquals("new-url", page.getSlug());
        assertNotNull(service.publicTopic(2L, "new-url").getPublished());

        service.offlineTopic(2L, 5L);
        assertNull(service.publicTopic(2L, "new-url"));
    }

    @Test
    void anotherMerchantsTopicCannotBeReadOrMutated() {
        when(pageMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.topicEditor(9L, 44L));
        assertThrows(BusinessException.class, () -> service.offlineTopic(9L, 44L));
        verify(pageMapper, never()).updateById(any());
    }

    @Test
    void slugCollisionAndUnsafeImageAreRejected() {
        StorefrontPage existing = topic(1L, "taken", "PUBLISHED", "taken", documentWithTextBlock());
        when(pageMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        assertThrows(BusinessException.class, () -> service.createTopic(2L, request("taken", documentWithTextBlock())));

        StorefrontPageSaveRequest unsafeImage = request("safe-url", documentWithTextBlock());
        unsafeImage.setCoverImage("javascript:alert(1)");
        assertThrows(BusinessException.class, () -> service.createTopic(2L, unsafeImage));
    }

    private StorefrontPageSaveRequest request(String slug, StorefrontDocument document) {
        StorefrontPageSaveRequest request = new StorefrontPageSaveRequest();
        request.setTitle("春季专题"); request.setSlug(slug); request.setDocument(document);
        return request;
    }

    private StorefrontDocument documentWithTextBlock() {
        StorefrontDocument document = new StorefrontDocument();
        StorefrontDocument.Block block = new StorefrontDocument.Block();
        block.setId("intro"); block.setType("TEXT"); block.setTitle("主题"); block.setBody("专题内容");
        document.setBlocks(new ArrayList<>(List.of(block)));
        return document;
    }

    private StorefrontDocument homeDocument(StorefrontDocument.Module... modules) {
        StorefrontDocument document = new StorefrontDocument(); document.setModules(new ArrayList<>(List.of(modules))); return document;
    }

    private StorefrontDocument.Module module(String id, String code, int order, int enabled) {
        StorefrontDocument.Module module = new StorefrontDocument.Module();
        module.setId(id); module.setCode(code); module.setTitle(code); module.setSortOrder(order); module.setEnabled(enabled);
        if ("PRODUCT_FEED".equals(code) || "POPULAR_PRODUCTS".equals(code) || "NEW_ARRIVALS".equals(code)) {
            module.setProductSource("RECENT"); module.setProductLimit(5);
        }
        return module;
    }

    private String json(Object value) {
        try { return new ObjectMapper().writeValueAsString(value); } catch (Exception exception) { throw new RuntimeException(exception); }
    }

    private StorefrontPage topic(Long id, String publicSlug, String status, String draftSlug, StorefrontDocument document) {
        StorefrontPage page = new StorefrontPage();
        page.setId(id); page.setMerchantId(2L); page.setPageType("TOPIC"); page.setPageKey("TOPIC-" + draftSlug);
        page.setSlug(publicSlug); page.setDraftSlug(draftSlug); page.setTitle("专题"); page.setStatus(status);
        page.setDraftTitle("专题"); page.setSummary(""); page.setDraftSummary(""); page.setCoverImage(""); page.setDraftCoverImage("");
        try { page.setDraftJson(new ObjectMapper().writeValueAsString(document)); } catch (Exception exception) { throw new RuntimeException(exception); }
        page.setPublishedJson("PUBLISHED".equals(status) ? page.getDraftJson() : null);
        return page;
    }
}
