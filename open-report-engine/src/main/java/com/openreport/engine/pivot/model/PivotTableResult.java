package com.openreport.engine.pivot.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 交叉报表结果实体
 */
@Data
public class PivotTableResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<List<PivotHeaderCell>> rowHeaders;

    private List<List<PivotHeaderCell>> columnHeaders;

    private List<List<PivotDataCell>> dataCells;

    private Map<String, Object> summary;

    private List<String> drillDownFields;
}
