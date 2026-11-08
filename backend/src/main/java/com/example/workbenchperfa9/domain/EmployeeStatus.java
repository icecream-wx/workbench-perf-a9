package com.example.workbenchperfa9.domain;

/**
 * 员工状态：在职 / 停用。
 */
public enum EmployeeStatus {
    ACTIVE,
    INACTIVE;

    public static EmployeeStatus fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return ACTIVE;
        }
        for (EmployeeStatus status : values()) {
            if (status.name().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
