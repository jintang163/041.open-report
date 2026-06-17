package com.openreport.admin.dto.writeback;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean valid = true;

    private List<String> errors = new ArrayList<>();

    private Integer rowIndex;

    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }

    public void merge(ValidationResult other) {
        if (!other.isValid()) {
            this.valid = false;
            this.errors.addAll(other.getErrors());
        }
    }
}
