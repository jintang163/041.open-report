package com.openreport.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.openreport.admin.entity.ChartDashboard;
import com.openreport.admin.mapper.ChartDashboardMapper;
import com.openreport.admin.service.ChartDashboardService;
import org.springframework.stereotype.Service;

@Service
public class ChartDashboardServiceImpl extends ServiceImpl<ChartDashboardMapper, ChartDashboard> implements ChartDashboardService {
}
