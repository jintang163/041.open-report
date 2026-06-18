package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.entity.ReportTemplateMarket;

public interface ReportTemplateMarketService extends IService<ReportTemplateMarket> {

    Page<ReportTemplateMarket> pagePublic(Integer pageNum, Integer pageSize, String keyword,
                                          String category, Integer templateType, String sortBy);

    Page<ReportTemplateMarket> pageMyUploads(Long userId, Integer pageNum, Integer pageSize,
                                             String keyword, Integer visibility);

    ReportTemplateMarket publishTemplate(Long templateId, Long userId, String userName,
                                         Integer visibility, String category, String tags,
                                         String description, String coverImage);

    ReportTemplate installTemplate(Long marketId, Long userId, String userName);

    boolean takeDown(Long marketId, Long userId);

    ReportTemplateMarket getDetail(Long id);

    void incrementInstallCount(Long id);

    void incrementLikeCount(Long id);
}
