package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ChartDashboardItem;
import com.openreport.admin.mapper.ChartDashboardItemMapper;
import com.openreport.admin.service.ChartDashboardItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public List<ChartDashboardItem> saveBatchItems(Long dashboardId, List<ChartDashboardItem> items) {
        List<ChartDashboardItem> existingItems = listByDashboardId(dashboardId);
        Set<Long> existingIds = new HashSet<>();
        existingItems.forEach(item -> existingIds.add(item.getId()));

        Set<Long> incomingIds = new HashSet<>();
        List<ChartDashboardItem> toUpdate = new ArrayList<>();
        List<ChartDashboardItem> toInsert = new ArrayList<>();

        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                ChartDashboardItem item = items.get(i);
                item.setSortOrder(i);
                item.setDashboardId(dashboardId);

                if (item.getId() != null && existingIds.contains(item.getId())) {
                    toUpdate.add(item);
                    incomingIds.add(item.getId());
                } else {
                    item.setId(null);
                    toInsert.add(item);
                }
            }
        }

        Set<Long> toDelete = new HashSet<>(existingIds);
        toDelete.removeAll(incomingIds);
        if (!toDelete.isEmpty()) {
            removeByIds(toDelete);
        }

        if (!toUpdate.isEmpty()) {
            updateBatchById(toUpdate);
        }

        if (!toInsert.isEmpty()) {
            saveBatch(toInsert);
        }

        return listByDashboardId(dashboardId);
    }
}
