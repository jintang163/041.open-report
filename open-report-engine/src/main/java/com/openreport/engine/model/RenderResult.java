package com.openreport.engine.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class RenderResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportId;

    private String htmlContent;

    private Map<String, Object> chartOptions;

    private Map<String, List<Map<String, Object>>> dataSets;

    private List<List<ReportCell>> cellMatrix;

    private Map<String, Object> meta;

    private Long costTime;
}
