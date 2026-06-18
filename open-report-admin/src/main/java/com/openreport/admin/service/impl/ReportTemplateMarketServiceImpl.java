package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ReportTemplate;
import com.openreport.admin.entity.ReportTemplateMarket;
import com.openreport.admin.enums.MarketVisibilityEnum;
import com.openreport.admin.mapper.ReportTemplateMarketMapper;
import com.openreport.admin.service.ReportTemplateMarketService;
import com.openreport.admin.service.ReportTemplateService;
import com.openreport.common.enums.ReportStatusEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReportTemplateMarketServiceImpl
        extends ServiceImpl<ReportTemplateMarketMapper, ReportTemplateMarket>
        implements ReportTemplateMarketService {

    @Autowired
    private ReportTemplateService reportTemplateService;

    @Override
    public Page<ReportTemplateMarket> pagePublic(Integer pageNum, Integer pageSize, String keyword,
                                                  String category, Integer templateType, String sortBy) {
        Page<ReportTemplateMarket> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportTemplateMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplateMarket::getVisibility, MarketVisibilityEnum.PUBLIC.getCode());
        wrapper.eq(ReportTemplateMarket::getStatus, 1);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ReportTemplateMarket::getTemplateName, keyword)
                    .or().like(ReportTemplateMarket::getDescription, keyword)
                    .or().like(ReportTemplateMarket::getTags, keyword));
        }
        if (StringUtils.isNotBlank(category)) {
            wrapper.eq(ReportTemplateMarket::getCategory, category);
        }
        if (templateType != null) {
            wrapper.eq(ReportTemplateMarket::getTemplateType, templateType);
        }
        if ("install".equals(sortBy)) {
            wrapper.orderByDesc(ReportTemplateMarket::getInstallCount);
        } else if ("like".equals(sortBy)) {
            wrapper.orderByDesc(ReportTemplateMarket::getLikeCount);
        } else if ("newest".equals(sortBy)) {
            wrapper.orderByDesc(ReportTemplateMarket::getCreateTime);
        } else {
            wrapper.orderByDesc(ReportTemplateMarket::getCreateTime);
        }
        return page(page, wrapper);
    }

    @Override
    public Page<ReportTemplateMarket> pageMyUploads(Long userId, Integer pageNum, Integer pageSize,
                                                     String keyword, Integer visibility) {
        Page<ReportTemplateMarket> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportTemplateMarket> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportTemplateMarket::getAuthorId, userId);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(ReportTemplateMarket::getTemplateName, keyword);
        }
        if (visibility != null) {
            wrapper.eq(ReportTemplateMarket::getVisibility, visibility);
        }
        wrapper.orderByDesc(ReportTemplateMarket::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplateMarket publishTemplate(Long templateId, Long userId, String userName,
                                                 Integer visibility, String category, String tags,
                                                 String description, String coverImage) {
        ReportTemplate template = reportTemplateService.getById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }

        LambdaQueryWrapper<ReportTemplateMarket> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ReportTemplateMarket::getSourceTemplateId, templateId);
        checkWrapper.eq(ReportTemplateMarket::getAuthorId, userId);
        ReportTemplateMarket existing = getOne(checkWrapper);

        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setTemplateName(template.getTemplateName());
            existing.setTemplateJson(template.getTemplateJson());
            existing.setDataSetBind(template.getDataSetBind());
            existing.setParamConfig(template.getParamConfig());
            existing.setVisibility(visibility);
            existing.setCategory(category);
            existing.setTags(tags);
            existing.setDescription(StringUtils.isNotBlank(description) ? description : template.getDescription());
            existing.setCoverImage(coverImage);
            existing.setUpdateTime(now);
            updateById(existing);
            return existing;
        }

        ReportTemplateMarket market = new ReportTemplateMarket();
        market.setTemplateName(template.getTemplateName());
        market.setTemplateCode("MKT_" + System.currentTimeMillis());
        market.setTemplateType(template.getTemplateType());
        market.setTemplateJson(template.getTemplateJson());
        market.setDataSetBind(template.getDataSetBind());
        market.setParamConfig(template.getParamConfig());
        market.setDescription(StringUtils.isNotBlank(description) ? description : template.getDescription());
        market.setCoverImage(coverImage);
        market.setVisibility(visibility);
        market.setCategory(category);
        market.setTags(tags);
        market.setVersion("1.0.0");
        market.setInstallCount(0);
        market.setLikeCount(0);
        market.setStatus(1);
        market.setAuthorId(userId);
        market.setAuthorName(userName);
        market.setSourceTemplateId(templateId);
        market.setCreateTime(now);
        market.setUpdateTime(now);
        market.setDeleted(0);

        save(market);
        return market;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportTemplate installTemplate(Long marketId, Long userId, String userName) {
        ReportTemplateMarket market = getById(marketId);
        if (market == null) {
            throw new RuntimeException("市场模板不存在");
        }
        if (!MarketVisibilityEnum.PUBLIC.getCode().equals(market.getVisibility())
                && !market.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权安装私有模板");
        }

        ReportTemplate template = new ReportTemplate();
        template.setTemplateName(market.getTemplateName() + "_安装版");
        template.setTemplateCode("INST_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        template.setTemplateType(market.getTemplateType());
        template.setTemplateJson(market.getTemplateJson());
        template.setDataSetBind(market.getDataSetBind());
        template.setParamConfig(market.getParamConfig());
        template.setDescription("安装自模板市场：" + market.getDescription());
        template.setStatus(ReportStatusEnum.DRAFT.getCode());
        template.setCreateBy(userId);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateBy(userId);
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(0);

        reportTemplateService.save(template);
        incrementInstallCount(marketId);

        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean takeDown(Long marketId, Long userId) {
        ReportTemplateMarket market = getById(marketId);
        if (market == null) {
            throw new RuntimeException("市场模板不存在");
        }
        if (!market.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权下架他人模板");
        }
        market.setStatus(0);
        market.setUpdateTime(LocalDateTime.now());
        return updateById(market);
    }

    @Override
    public ReportTemplateMarket getDetail(Long id) {
        ReportTemplateMarket market = getById(id);
        if (market != null) {
            incrementLikeCount(id);
        }
        return market;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementInstallCount(Long id) {
        LambdaUpdateWrapper<ReportTemplateMarket> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReportTemplateMarket::getId, id)
                .setSql("install_count = install_count + 1");
        update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementLikeCount(Long id) {
        LambdaUpdateWrapper<ReportTemplateMarket> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReportTemplateMarket::getId, id)
                .setSql("like_count = like_count + 1");
        update(wrapper);
    }
}
