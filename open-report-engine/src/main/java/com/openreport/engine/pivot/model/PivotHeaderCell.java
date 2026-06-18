package com.openreport.engine.pivot.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 多层表头单元格
 */
@Data
public class PivotHeaderCell implements Serializable {

    private static final long serialVersionUID = 1L;

    private Object value;

    private Integer rowSpan;

    private Integer colSpan;

    private Integer level;

    private Boolean isLeaf;

    private String fieldName;

    private Object fieldValue;
}
