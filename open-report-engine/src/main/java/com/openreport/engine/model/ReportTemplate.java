package com.openreport.engine.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ReportTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    private String templateId;

    private String templateName;

    private String templateCode;

    private List<ReportParameter> parameters;

    private List<DataSetBind> dataSets;

    private List<ReportCell> cells;

    private Map<String, ChartConfig> charts;

    private Map<String, Object> styles;

    private Map<String, Object> extra;
}
