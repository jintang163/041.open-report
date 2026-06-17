package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ChartDashboardItem;

import java.util.List;

public interface ChartDashboardItemService extends IService<ChartDashboardItem> {

    List<ChartDashboardItem> listByDashboardId(Long dashboardId);

    List<ChartDashboardItem> saveBatchItems(Long dashboardId, List<ChartDashboardItem> items);
}
