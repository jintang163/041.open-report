package com.openreport.engine.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class DataSetBind implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dataSetId;

    private String dataSetName;

    private String dataSourceId;

    private String dataSourceName;

    private String sql;

    private Map<String, Object> parameters;

    private Map<String, Object> config;
}
