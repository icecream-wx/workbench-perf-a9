package com.example.workbenchperfa9.controller;

import com.example.workbenchperfa9.domain.Employee;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String empNo,
        String name,
        String department,
        String position,
        String phone,
        String email,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate hireDate,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmpNo(),
                employee.getName(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getPhone(),
                employee.getEmail(),
                employee.getHireDate(),
                employee.getStatus() == null ? null : employee.getStatus().name(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }
}
