package com.openreport.admin.dto.writeback;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class CellDataChange implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer rowIndex;

    private String rowStatus;

    private Map<String, Object> oldData;

    private Map<String, Object> newData;

    private Map<String, String> cellValues;
}
