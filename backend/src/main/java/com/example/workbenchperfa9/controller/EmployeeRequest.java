package com.example.workbenchperfa9.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 员工新增／编辑请求体。
 *
 * <p>所有字符串字段在反序列化阶段统一去除首尾空白，空白字符串按“未填写”处理（落库为 {@code null}）；
 * 因此后续 Bean Validation 校验的是去除空白后的值。请求体中即使携带 {@code id}、{@code createdAt}、
 * {@code updatedAt} 也会因为 DTO 未声明而被忽略。</p>
 */
public class EmployeeRequest {

    @NotBlank(message = "工号不能为空")
    @Size(max = 32, message = "工号长度不能超过32个字符")
    private String empNo;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 64, message = "姓名长度不能超过64个字符")
    private String name;

    @Size(max = 64, message = "部门长度不能超过64个字符")
    private String department;

    @Size(max = 64, message = "职位长度不能超过64个字符")
    private String position;

    @Size(max = 32, message = "手机号长度不能超过32个字符")
    @Pattern(regexp = "[0-9+\\-\\s]*", message = "手机号只能包含数字、+、- 或空格")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128个字符")
    private String email;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "入职日期格式不正确，应为 yyyy-MM-dd")
    private String hireDate;

    @Pattern(regexp = "ACTIVE|INACTIVE", message = "状态只能为 ACTIVE 或 INACTIVE")
    private String status;

    public String getEmpNo() {
        return empNo;
    }

    public void setEmpNo(String empNo) {
        this.empNo = normalize(empNo);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalize(name);
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = normalize(department);
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = normalize(position);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = normalize(phone);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = normalize(email);
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = normalize(hireDate);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = normalize(status);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        int start = 0;
        int end = value.length();
        while (start < end && isBlank(value.charAt(start))) {
            start++;
        }
        while (end > start && isBlank(value.charAt(end - 1))) {
            end--;
        }
        return start == end ? null : value.substring(start, end);
    }

    private static boolean isBlank(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
