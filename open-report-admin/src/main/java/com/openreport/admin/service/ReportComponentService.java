package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.ReportComponent;
import com.openreport.admin.entity.ReportComponentInstall;

import java.util.List;

public interface ReportComponentService extends IService<ReportComponent> {

    Page<ReportComponent> pageComponents(Integer pageNum, Integer pageSize, String keyword,
                                         String category, Integer componentType,
                                         Integer source, String sortBy);

    ReportComponent getDetail(Long id);

    ReportComponent publishComponent(ReportComponent component, Long userId, String userName, Integer source);

    ReportComponentInstall installComponent(Long componentId, Long userId, String userName);

    boolean uninstallComponent(Long componentId, Long userId);

    List<ReportComponentInstall> listMyInstalls(Long userId);

    boolean isInstalled(Long componentId, Long userId);

    void incrementDownloadCount(Long id);

    List<String> listCategories();
}
