package com.openreport.engine.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ChartConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private String chartId;

    private String chartType;

    private String title;

    private String dataSetId;

    private String xAxisField;

    private List<String> yAxisFields;

    private Map<String, Object> legend;

    private Map<String, Object> tooltip;

    private Map<String, Object> extra;
}
