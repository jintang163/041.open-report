package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ReportComponent;
import com.openreport.admin.entity.ReportComponentInstall;
import com.openreport.admin.enums.ComponentSourceEnum;
import com.openreport.admin.mapper.ReportComponentInstallMapper;
import com.openreport.admin.mapper.ReportComponentMapper;
import com.openreport.admin.service.ReportComponentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportComponentServiceImpl
        extends ServiceImpl<ReportComponentMapper, ReportComponent>
        implements ReportComponentService {

    @Autowired
    private ReportComponentInstallMapper componentInstallMapper;

    @Override
    public Page<ReportComponent> pageComponents(Integer pageNum, Integer pageSize, String keyword,
                                                 String category, Integer componentType,
                                                 Integer source, String sortBy) {
        Page<ReportComponent> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ReportComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportComponent::getStatus, 1);
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ReportComponent::getComponentName, keyword)
                    .or().like(ReportComponent::getDescription, keyword)
                    .or().like(ReportComponent::getTags, keyword));
        }
        if (StringUtils.isNotBlank(category)) {
            wrapper.eq(ReportComponent::getCategory, category);
        }
        if (componentType != null) {
            wrapper.eq(ReportComponent::getComponentType, componentType);
        }
        if (source != null) {
            wrapper.eq(ReportComponent::getSource, source);
        }
        if ("download".equals(sortBy)) {
            wrapper.orderByDesc(ReportComponent::getDownloadCount);
        } else if ("newest".equals(sortBy)) {
            wrapper.orderByDesc(ReportComponent::getCreateTime);
        } else {
            wrapper.orderByDesc(ReportComponent::getCreateTime);
        }
        return page(page, wrapper);
    }

    @Override
    public ReportComponent getDetail(Long id) {
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportComponent publishComponent(ReportComponent component, Long userId,
                                            String userName, Integer source) {
        LocalDateTime now = LocalDateTime.now();
        if (component.getId() == null) {
            component.setComponentCode("COMP_" + System.currentTimeMillis());
            component.setSource(source != null ? source : ComponentSourceEnum.COMMUNITY.getCode());
            component.setDownloadCount(0);
            component.setStatus(1);
            component.setAuthorId(userId);
            component.setAuthorName(userName);
            component.setCreateTime(now);
            component.setUpdateTime(now);
            component.setDeleted(0);
            save(component);
        } else {
            component.setUpdateTime(now);
            updateById(component);
        }
        return component;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReportComponentInstall installComponent(Long componentId, Long userId, String userName) {
        ReportComponent component = getById(componentId);
        if (component == null) {
            throw new RuntimeException("组件不存在");
        }

        if (isInstalled(componentId, userId)) {
            throw new RuntimeException("组件已安装");
        }

        ReportComponentInstall install = new ReportComponentInstall();
        install.setComponentId(componentId);
        install.setComponentCode(component.getComponentCode());
        install.setUserId(userId);
        install.setUserName(userName);
        install.setInstallTime(LocalDateTime.now());
        componentInstallMapper.insert(install);

        incrementDownloadCount(componentId);

        return install;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean uninstallComponent(Long componentId, Long userId) {
        LambdaQueryWrapper<ReportComponentInstall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportComponentInstall::getComponentId, componentId);
        wrapper.eq(ReportComponentInstall::getUserId, userId);
        ReportComponentInstall install = componentInstallMapper.selectOne(wrapper);
        if (install == null) {
            return false;
        }
        componentInstallMapper.deleteById(install.getId());
        return true;
    }

    @Override
    public List<ReportComponentInstall> listMyInstalls(Long userId) {
        LambdaQueryWrapper<ReportComponentInstall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportComponentInstall::getUserId, userId);
        wrapper.orderByDesc(ReportComponentInstall::getInstallTime);
        return componentInstallMapper.selectList(wrapper);
    }

    @Override
    public boolean isInstalled(Long componentId, Long userId) {
        LambdaQueryWrapper<ReportComponentInstall> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportComponentInstall::getComponentId, componentId);
        wrapper.eq(ReportComponentInstall::getUserId, userId);
        return componentInstallMapper.selectCount(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementDownloadCount(Long id) {
        LambdaUpdateWrapper<ReportComponent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ReportComponent::getId, id)
                .setSql("download_count = download_count + 1");
        update(wrapper);
    }

    @Override
    public List<String> listCategories() {
        LambdaQueryWrapper<ReportComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ReportComponent::getCategory);
        wrapper.eq(ReportComponent::getStatus, 1);
        wrapper.groupBy(ReportComponent::getCategory);
        wrapper.isNotNull(ReportComponent::getCategory);
        List<ReportComponent> list = list(wrapper);
        return list.stream()
                .map(ReportComponent::getCategory)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}
