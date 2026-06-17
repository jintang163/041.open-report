package com.openreport.admin.dto.writeback;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class DataSubmitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long reportId;

    private Long configId;

    private Map<String, Object> params;

    private List<CellDataChange> changes;
}
