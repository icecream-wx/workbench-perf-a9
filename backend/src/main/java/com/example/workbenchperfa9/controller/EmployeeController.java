package com.example.workbenchperfa9.controller;

import com.example.workbenchperfa9.domain.Employee;
import com.example.workbenchperfa9.exception.ApiResponse;
import com.example.workbenchperfa9.exception.BizException;
import com.example.workbenchperfa9.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public ApiResponse<EmployeePageResponse> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        if (page < 1) {
            throw new BizException(400, "页码不能小于1");
        }
        if (size < 1) {
            throw new BizException(400, "每页条数不能小于1");
        }
        if (size > EmployeeService.MAX_PAGE_SIZE) {
            throw new BizException(400, "每页条数不能超过" + EmployeeService.MAX_PAGE_SIZE);
        }
        Page<Employee> result = employeeService.list(keyword, page, size);
        List<EmployeeResponse> items = result.getContent().stream()
                .map(EmployeeResponse::from)
                .toList();
        return ApiResponse.ok(new EmployeePageResponse(items, result.getTotalElements(), page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> detail(@PathVariable String id) {
        return ApiResponse.ok(EmployeeResponse.from(employeeService.get(parseId(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(@Valid @RequestBody EmployeeRequest request) {
        Employee created = employeeService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(EmployeeResponse.from(created)));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> update(
            @PathVariable String id,
            @Valid @RequestBody EmployeeRequest request) {
        return ApiResponse.ok(EmployeeResponse.from(employeeService.update(parseId(id), request)));
    }

    private long parseId(String rawId) {
        try {
            long id = Long.parseLong(rawId.trim());
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
            // 落到统一 404 语义
        }
        throw new BizException(HttpStatus.NOT_FOUND, EmployeeService.NOT_FOUND_CODE, "员工不存在");
    }
}
