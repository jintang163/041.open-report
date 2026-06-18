package com.openreport.engine.pivot.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 交叉报表字段实体
 */
@Data
public class PivotField implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fieldName;

    private String displayName;

    private FieldType fieldType;

    private String aggregateFunction;

    private Integer sortOrder;
}
