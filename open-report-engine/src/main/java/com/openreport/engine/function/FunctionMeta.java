package com.openreport.engine.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionMeta implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String label;

    private String category;

    private String description;

    private List<FunctionParam> params;

    private String returnType;

    private String example;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionParam implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String type;
        private Boolean required;
        private String description;
    }
}
