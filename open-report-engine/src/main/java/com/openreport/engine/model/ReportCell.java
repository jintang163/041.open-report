package com.openreport.engine.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ReportCell implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer rowIndex;

    private Integer colIndex;

    private Integer rowSpan = 1;

    private Integer colSpan = 1;

    private String expression;

    private Object value;

    private String dataType;

    private Map<String, Object> styles;

    private Map<String, Object> properties;
}
