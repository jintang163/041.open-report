package com.openreport.engine.pivot.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 交叉报表配置实体
 */
@Data
public class PivotTableConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long dataSetId;

    private List<PivotField> rowFields;

    private List<PivotField> columnFields;

    private List<PivotField> valueFields;

    private Boolean showSubtotal;

    private Boolean showGrandTotal;

    private String subtotalPosition;
}
