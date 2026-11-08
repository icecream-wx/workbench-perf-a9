package com.example.workbenchperfa9.service;

import com.example.workbenchperfa9.controller.EmployeeRequest;
import com.example.workbenchperfa9.domain.Employee;
import com.example.workbenchperfa9.domain.EmployeeStatus;
import com.example.workbenchperfa9.exception.BizException;
import com.example.workbenchperfa9.repository.EmployeeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

@Service
public class EmployeeService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int NOT_FOUND_CODE = 40401;
    public static final int CONFLICT_CODE = 40901;

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public Page<Employee> list(String keyword, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.ASC, "id"));
        String condition = keyword == null ? "" : keyword.trim();
        if (condition.isEmpty()) {
            return employeeRepository.findAll(pageable);
        }
        return employeeRepository.searchByKeyword(condition, pageable);
    }

    @Transactional(readOnly = true)
    public Employee get(long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, NOT_FOUND_CODE, "员工不存在"));
    }

    @Transactional
    public Employee create(EmployeeRequest request) {
        String empNo = request.getEmpNo();
        if (employeeRepository.existsByEmpNoIgnoreCase(empNo)) {
            throw conflict();
        }
        Employee employee = new Employee();
        applyEditableFields(employee, request);
        LocalDateTime now = now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);
        return saveWithUniqueCheck(employee);
    }

    @Transactional
    public Employee update(long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, NOT_FOUND_CODE, "员工不存在"));
        String empNo = request.getEmpNo();
        if (employeeRepository.existsByEmpNoIgnoreCaseAndIdNot(empNo, id)) {
            throw conflict();
        }
        applyEditableFields(employee, request);
        LocalDateTime now = now();
        if (!now.isAfter(employee.getUpdatedAt())) {
            now = employee.getUpdatedAt().plusSeconds(1);
        }
        employee.setUpdatedAt(now);
        return saveWithUniqueCheck(employee);
    }

    private Employee saveWithUniqueCheck(Employee employee) {
        try {
            return employeeRepository.saveAndFlush(employee);
        } catch (DataIntegrityViolationException exception) {
            throw conflict();
        }
    }

    private BizException conflict() {
        return new BizException(HttpStatus.CONFLICT, CONFLICT_CODE, "empNo: 工号已存在");
    }

    private void applyEditableFields(Employee employee, EmployeeRequest request) {
        employee.setEmpNo(request.getEmpNo());
        employee.setName(request.getName());
        employee.setDepartment(request.getDepartment());
        employee.setPosition(request.getPosition());
        employee.setPhone(request.getPhone());
        employee.setEmail(request.getEmail());
        employee.setHireDate(parseHireDate(request.getHireDate()));
        EmployeeStatus status = EmployeeStatus.fromCode(request.getStatus());
        if (status == null) {
            throw new BizException(400, "status: 状态只能为 ACTIVE 或 INACTIVE");
        }
        employee.setStatus(status);
    }

    private LocalDate parseHireDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new BizException(400, "hireDate: 入职日期格式不正确，应为 yyyy-MM-dd");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZONE).truncatedTo(ChronoUnit.SECONDS);
    }
}
