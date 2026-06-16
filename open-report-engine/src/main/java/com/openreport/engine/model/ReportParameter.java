package com.openreport.engine.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ReportParameter implements Serializable {

    private static final long serialVersionUID = 1L;

    private String paramCode;

    private String paramName;

    private String paramType;

    private Object defaultValue;

    private Boolean required = false;

    private List<Object> options;

    private Map<String, Object> validators;

    private Map<String, Object> extra;
}
