package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ChartDashboardItem;
import com.openreport.admin.mapper.ChartDashboardItemMapper;
import com.openreport.admin.service.ChartDashboardItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChartDashboardItemServiceImpl extends ServiceImpl<ChartDashboardItemMapper, ChartDashboardItem> implements ChartDashboardItemService {

    @Override
    public List<ChartDashboardItem> listByDashboardId(Long dashboardId) {
        return list(new LambdaQueryWrapper<ChartDashboardItem>()
                .eq(ChartDashboardItem::getDashboardId, dashboardId)
                .orderByAsc(ChartDashboardItem::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchItems(Long dashboardId, List<ChartDashboardItem> items) {
        remove(new LambdaQueryWrapper<ChartDashboardItem>()
                .eq(ChartDashboardItem::getDashboardId, dashboardId));
        if (items != null && !items.isEmpty()) {
            items.forEach(item -> {
                item.setId(null);
                item.setDashboardId(dashboardId);
            });
            saveBatch(items);
        }
    }
}
