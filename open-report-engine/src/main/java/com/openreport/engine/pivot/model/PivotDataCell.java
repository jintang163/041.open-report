package com.openreport.engine.pivot.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据单元格
 */
@Data
public class PivotDataCell implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer rowIndex;

    private Integer colIndex;

    private Object value;

    private String formattedValue;

    private String aggregateFunction;

    private Boolean isSubtotal;

    private Boolean isGrandTotal;
}
