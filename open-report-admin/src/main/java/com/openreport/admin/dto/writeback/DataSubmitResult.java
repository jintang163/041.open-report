package com.openreport.admin.dto.writeback;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DataSubmitResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String batchNo;

    private String status;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private Long executeTime;

    private String errorMsg;

    private List<DetailResult> details;

    @Data
    public static class DetailResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer rowIndex;
        private String rowStatus;
        private String status;
        private String errorMsg;
        private String executeSql;
    }
}
