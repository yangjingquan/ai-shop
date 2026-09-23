package com.shop.home.service;

import com.shop.home.dto.StorefrontDocument;
import com.shop.home.dto.StorefrontPageSaveRequest;
import com.shop.home.dto.StorefrontPageSummaryVO;
import com.shop.home.dto.StorefrontPageVO;
import com.shop.home.dto.StorefrontTemplateVO;
import com.shop.home.dto.HomeModuleVO;
import java.util.List;

public interface StorefrontPageService {
    StorefrontPageVO homeEditor(Long merchantId);
    void saveHomeDraft(Long merchantId, StorefrontPageSaveRequest request);
    void publishHome(Long merchantId);
    List<StorefrontPageSummaryVO> listTopics(Long merchantId);
    StorefrontPageVO topicEditor(Long merchantId, Long id);
    StorefrontPageVO createTopic(Long merchantId, StorefrontPageSaveRequest request);
    StorefrontPageVO updateTopic(Long merchantId, Long id, StorefrontPageSaveRequest request);
    void publishTopic(Long merchantId, Long id);
    void offlineTopic(Long merchantId, Long id);
    StorefrontPageVO publicTopic(Long merchantId, String slug);
    List<StorefrontTemplateVO> templates();
    List<HomeModuleVO> publishedHomeModules(Long merchantId);
    List<StorefrontPageSummaryVO> publishedTopics(Long merchantId, List<Long> ids);
}
