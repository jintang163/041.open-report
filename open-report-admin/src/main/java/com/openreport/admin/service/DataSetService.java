package com.openreport.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.openreport.admin.entity.DataSet;

import java.util.List;
import java.util.Map;

public interface DataSetService extends IService<DataSet> {

    Page<DataSet> pageList(Integer pageNum, Integer pageSize, String setName, Long dsId);

    List<DataSet> listByDsId(Long dsId);

    Map<String, Object> previewData(Long dataSetId, Map<String, Object> params, Integer limit);

    Map<String, Object> previewDataWithCount(Long dataSetId, Map<String, Object> params, Integer limit);

    long countData(Long dataSetId, Map<String, Object> params);

    Map<String, Object> pagePreviewData(Long dataSetId, Map<String, Object> params, int pageNum, int pageSize);

    void streamBatchData(Long dataSetId, Map<String, Object> params, int batchSize,
                     java.util.function.Consumer<java.util.List<Map<String, Object>>> batchCallback);

    List<Map<String, Object>> previewDataList(Long dataSetId, Map<String, Object> params);

    Map<String, Object> parseSql(String sqlText);
}
