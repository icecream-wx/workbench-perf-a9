package com.example.workbenchperfa9;

import com.example.workbenchperfa9.domain.Employee;
import com.example.workbenchperfa9.domain.EmployeeStatus;
import com.example.workbenchperfa9.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void cleanDatabase() {
        employeeRepository.deleteAll();
    }

    @Test
    void createEmployeeReturns201AndIsReadable() throws Exception {
        Call call = post(validPayload("EMP001"));

        assertEquals(201, call.status());
        assertEquals(0, call.code());
        assertEquals("ok", call.message());
        JsonNode data = call.data();
        assertTrue(data.path("id").asLong() > 0);
        assertEquals("EMP001", data.path("empNo").asText());
        assertEquals("张三", data.path("name").asText());
        assertEquals("研发部", data.path("department").asText());
        assertEquals("工程师", data.path("position").asText());
        assertEquals("13800138000", data.path("phone").asText());
        assertEquals("zhangsan@example.com", data.path("email").asText());
        assertEquals("2024-01-31", data.path("hireDate").asText());
        assertEquals("ACTIVE", data.path("status").asText());
        assertTrue(data.path("createdAt").asText().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
        assertEquals(data.path("createdAt").asText(), data.path("updatedAt").asText());
        assertEquals(1, employeeRepository.count());

        Call detail = get("/api/employees/" + data.path("id").asLong());
        assertEquals(200, detail.status());
        assertEquals(0, detail.code());
        assertEquals(data.path("empNo").asText(), detail.data().path("empNo").asText());
        assertEquals(data.path("hireDate").asText(), detail.data().path("hireDate").asText());
        assertEquals(data.path("createdAt").asText(), detail.data().path("createdAt").asText());
    }

    @Test
    void createEmployeeTrimsValuesAndDefaultsStatus() throws Exception {
        Map<String, Object> payload = validPayload("  EMP002  ");
        payload.put("name", "  李四  ");
        payload.put("department", "   ");
        payload.put("position", "");
        payload.put("phone", "   ");
        payload.put("email", "");
        payload.put("hireDate", "  ");
        payload.remove("status");

        Call call = post(payload);

        assertEquals(201, call.status());
        assertEquals("EMP002", call.data().path("empNo").asText());
        assertEquals("李四", call.data().path("name").asText());
        assertTrue(call.data().path("department").isNull());
        assertTrue(call.data().path("position").isNull());
        assertTrue(call.data().path("phone").isNull());
        assertTrue(call.data().path("email").isNull());
        assertTrue(call.data().path("hireDate").isNull());
        assertEquals("ACTIVE", call.data().path("status").asText());
    }

    @Test
    void createEmployeeAcceptsInactiveStatusAndIgnoresServerManagedFields() throws Exception {
        Map<String, Object> payload = validPayload("EMP003");
        payload.put("status", "INACTIVE");
        payload.put("id", 999);
        payload.put("createdAt", "2000-01-01T00:00:00");
        payload.put("updatedAt", "2000-01-01T00:00:00");

        Call call = post(payload);

        assertEquals(201, call.status());
        assertEquals("INACTIVE", call.data().path("status").asText());
        assertTrue(call.data().path("id").asLong() > 0);
        assertTrue(call.data().path("id").asLong() != 999);
        assertFalse(call.data().path("createdAt").asText().startsWith("2000-01-01"));
    }

    @Test
    void createEmployeeRejectsDuplicateEmpNo() throws Exception {
        assertEquals(201, post(validPayload("EMP001")).status());

        Call duplicate = post(validPayload("  emp001  "));

        assertEquals(409, duplicate.status());
        assertEquals(40901, duplicate.code());
        assertTrue(duplicate.message().contains("empNo"));
        assertTrue(duplicate.message().contains("工号已存在"));
        assertEquals(1, employeeRepository.count());
    }

    @Test
    void createEmployeeRejectsMissingOrBlankEmpNoAndName() throws Exception {
        Map<String, Object> missingEmpNo = validPayload("EMP001");
        missingEmpNo.remove("empNo");
        Call empNoCall = post(missingEmpNo);
        assertEquals(400, empNoCall.status());
        assertEquals(400, empNoCall.code());
        assertTrue(empNoCall.message().contains("empNo"), empNoCall.message());

        Map<String, Object> blankEmpNo = validPayload("   ");
        assertRejected(blankEmpNo, "empNo");

        Map<String, Object> blankName = validPayload("EMP001");
        blankName.put("name", "   ");
        assertRejected(blankName, "name");

        Map<String, Object> missingName = validPayload("EMP001");
        missingName.remove("name");
        Call nameCall = post(missingName);
        assertEquals(400, nameCall.status());
        assertTrue(nameCall.message().contains("name"), nameCall.message());

        assertEquals(0, employeeRepository.count());
    }

    @Test
    void createEmployeeReportsAllFieldErrors() throws Exception {
        Map<String, Object> payload = validPayload("EMP001");
        payload.put("empNo", "");
        payload.put("name", "");
        payload.put("email", "abc");
        payload.put("phone", "abc123");
        payload.put("hireDate", "2024/01/01");
        payload.put("status", "OTHER");

        Call call = post(payload);

        assertEquals(400, call.status());
        String message = call.message();
        assertTrue(message.contains("empNo"), message);
        assertTrue(message.contains("name"), message);
        assertTrue(message.contains("email"), message);
        assertTrue(message.contains("phone"), message);
        assertTrue(message.contains("hireDate"), message);
        assertTrue(message.contains("status"), message);
        assertEquals(0, employeeRepository.count());
    }

    @Test
    void createEmployeeValidatesFieldRules() throws Exception {
        Map<String, Object> longEmpNo = validPayload("E".repeat(33));
        assertRejected(longEmpNo, "empNo");

        Map<String, Object> longName = validPayload("EMP001");
        longName.put("name", "名".repeat(65));
        assertRejected(longName, "name");

        Map<String, Object> longPhone = validPayload("EMP001");
        longPhone.put("phone", "1".repeat(33));
        assertRejected(longPhone, "phone");

        Map<String, Object> longDepartment = validPayload("EMP001");
        longDepartment.put("department", "部".repeat(65));
        assertRejected(longDepartment, "department");

        Map<String, Object> longPosition = validPayload("EMP001");
        longPosition.put("position", "职".repeat(65));
        assertRejected(longPosition, "position");

        Map<String, Object> longEmail = validPayload("EMP001");
        longEmail.put("email", "a".repeat(120) + "@example.com");
        assertRejected(longEmail, "email");

        assertEquals(0, employeeRepository.count());
    }

    @Test
    void createEmployeeRejectsInvalidDateFormat() throws Exception {
        Map<String, Object> payload = validPayload("EMP001");
        payload.put("hireDate", "2024/01/01");

        Call call = post(payload);

        assertEquals(400, call.status());
        assertTrue(call.message().contains("hireDate"), call.message());
        assertEquals(0, employeeRepository.count());
    }

    @Test
    void createEmployeeAcceptsIsoDate() throws Exception {
        Map<String, Object> payload = validPayload("EMP001");
        payload.put("hireDate", "2024-01-01");

        Call call = post(payload);

        assertEquals(201, call.status());
        assertEquals("2024-01-01", call.data().path("hireDate").asText());
    }

    @Test
    void updateEmployeeRefreshesFieldsAndTimestamps() throws Exception {
        Call created = post(validPayload("EMP001"));
        long id = created.data().path("id").asLong();
        String createdAt = created.data().path("createdAt").asText();
        String updatedAt = created.data().path("updatedAt").asText();

        Map<String, Object> payload = validPayload("EMP001");
        payload.put("name", "张三丰");
        payload.put("department", "平台部");
        payload.put("status", "INACTIVE");
        Call updated = put(id, payload);

        assertEquals(200, updated.status());
        assertEquals(id, updated.data().path("id").asLong());
        assertEquals("张三丰", updated.data().path("name").asText());
        assertEquals("平台部", updated.data().path("department").asText());
        assertEquals("INACTIVE", updated.data().path("status").asText());
        assertEquals("EMP001", updated.data().path("empNo").asText());
        assertEquals(createdAt, updated.data().path("createdAt").asText());

        LocalDateTime before = LocalDateTime.parse(updatedAt);
        LocalDateTime after = LocalDateTime.parse(updated.data().path("updatedAt").asText());
        assertTrue(after.isAfter(before), "updatedAt should move forward");

        Call detail = get("/api/employees/" + id);
        assertEquals("张三丰", detail.data().path("name").asText());
        assertEquals(updated.data().path("updatedAt").asText(), detail.data().path("updatedAt").asText());
        assertEquals(1, employeeRepository.count());
    }

    @Test
    void updateEmployeeIsFullOverwriteAndKeepsCreatedAt() throws Exception {
        Map<String, Object> payload = validPayload("EMP001");
        Call created = post(payload);
        long id = created.data().path("id").asLong();
        String createdAt = created.data().path("createdAt").asText();

        Map<String, Object> minimal = new LinkedHashMap<>();
        minimal.put("empNo", "EMP001");
        minimal.put("name", "仅姓名");
        Call updated = put(id, minimal);

        assertEquals(200, updated.status());
        assertTrue(updated.data().path("department").isNull());
        assertTrue(updated.data().path("position").isNull());
        assertTrue(updated.data().path("phone").isNull());
        assertTrue(updated.data().path("email").isNull());
        assertTrue(updated.data().path("hireDate").isNull());
        assertEquals("ACTIVE", updated.data().path("status").asText());
        assertEquals(createdAt, updated.data().path("createdAt").asText());
    }

    @Test
    void updateEmployeeKeepsOwnEmpNoAndRejectsOthers() throws Exception {
        long first = post(validPayload("EMP001")).data().path("id").asLong();
        long second = post(validPayload("EMP002")).data().path("id").asLong();

        Map<String, Object> own = validPayload("EMP001");
        own.put("department", "平台部");
        Call kept = put(first, own);
        assertEquals(200, kept.status());
        assertEquals("EMP001", kept.data().path("empNo").asText());

        Call conflict = put(first, validPayload("EMP002"));
        assertEquals(409, conflict.status());
        assertEquals(40901, conflict.code());
        assertTrue(conflict.message().contains("empNo"));
        assertEquals("EMP001", get("/api/employees/" + first).data().path("empNo").asText());
        assertEquals("EMP002", get("/api/employees/" + second).data().path("empNo").asText());
        assertEquals(2, employeeRepository.count());
    }

    @Test
    void updateMissingEmployeeReturns404() throws Exception {
        Call call = put(999999L, validPayload("EMP999"));

        assertEquals(404, call.status());
        assertEquals(40401, call.code());
        assertTrue(call.message().contains("员工不存在"));
        assertEquals(0, employeeRepository.count());
    }

    @Test
    void detailMissingOrInvalidIdReturns404() throws Exception {
        assertEquals(404, get("/api/employees/999999").status());
        assertEquals(404, get("/api/employees/abc").status());
        assertEquals(404, get("/api/employees/-1").status());
    }

    @Test
    void listPaginatesWithStableIdOrderAndTotal() throws Exception {
        saveEmployees(3);

        Call firstPage = get("/api/employees?page=1&size=2");
        assertEquals(200, firstPage.status());
        assertEquals(0, firstPage.code());
        assertEquals(2, firstPage.data().path("items").size());
        assertEquals(3, firstPage.data().path("total").asInt());
        assertEquals(1, firstPage.data().path("page").asInt());
        assertEquals(2, firstPage.data().path("size").asInt());
        assertEquals("E001", firstPage.data().path("items").get(0).path("empNo").asText());
        assertEquals("E002", firstPage.data().path("items").get(1).path("empNo").asText());

        Call secondPage = get("/api/employees?page=2&size=2");
        assertEquals(1, secondPage.data().path("items").size());
        assertEquals(3, secondPage.data().path("total").asInt());
        assertEquals("E003", secondPage.data().path("items").get(0).path("empNo").asText());

        Call beyond = get("/api/employees?page=9&size=2");
        assertEquals(200, beyond.status());
        assertEquals(0, beyond.data().path("items").size());
        assertEquals(3, beyond.data().path("total").asInt());
    }

    @Test
    void listFiltersByKeyword() throws Exception {
        saveEmployee("E1001", "张三丰");
        saveEmployee("E2002", "李四");

        Call byNo = get("/api/employees?keyword=e1001");
        assertEquals(200, byNo.status());
        assertEquals(1, byNo.data().path("total").asInt());
        assertEquals("E1001", byNo.data().path("items").get(0).path("empNo").asText());

        Call byName = get("/api/employees?keyword=李");
        assertEquals(1, byName.data().path("total").asInt());
        assertEquals("E2002", byName.data().path("items").get(0).path("empNo").asText());

        Call blankKeyword = perform(MockMvcRequestBuilders.get("/api/employees").param("keyword", "   "));
        assertEquals(200, blankKeyword.status());
        assertEquals(2, blankKeyword.data().path("total").asInt());

        Call noMatch = get("/api/employees?keyword=不存在");
        assertEquals(200, noMatch.status());
        assertEquals(0, noMatch.data().path("total").asInt());
        assertEquals(0, noMatch.data().path("items").size());
    }

    @Test
    void listRejectsInvalidPagingParameters() throws Exception {
        assertEquals(400, get("/api/employees?page=0").status());
        assertEquals(400, get("/api/employees?size=0").status());
        assertEquals(400, get("/api/employees?size=101").status());
        assertEquals(400, get("/api/employees?page=-1").status());
    }

    @Test
    void listReturnsEmptyPageWhenNoData() throws Exception {
        Call call = get("/api/employees?page=1&size=10");

        assertEquals(200, call.status());
        assertEquals(0, call.data().path("total").asInt());
        assertEquals(0, call.data().path("items").size());
    }

    @Test
    void databaseUniqueConstraintRejectsDuplicateEmpNo() {
        employeeRepository.saveAndFlush(newEmployee("DUP001", "库约束一", EmployeeStatus.ACTIVE));

        DataIntegrityViolationException exception = assertThrows(DataIntegrityViolationException.class,
                () -> employeeRepository.saveAndFlush(newEmployee("DUP001", "库约束二", EmployeeStatus.ACTIVE)));

        assertNotNull(exception);
        assertEquals(1, employeeRepository.count());
    }

    private void assertRejected(Map<String, Object> payload, String expectedField) throws Exception {
        Call call = post(payload);
        assertEquals(400, call.status(), "field=" + expectedField);
        assertEquals(400, call.code(), "field=" + expectedField);
        assertTrue(call.message().contains(expectedField),
                "expected message to contain [" + expectedField + "] but was [" + call.message() + "]");
    }

    private void saveEmployees(int total) {
        List<Employee> employees = new ArrayList<>();
        for (int index = 1; index <= total; index++) {
            employees.add(newEmployee(String.format("E%03d", index), "员工" + index, EmployeeStatus.ACTIVE));
        }
        employeeRepository.saveAll(employees);
    }

    private void saveEmployee(String empNo, String name) {
        employeeRepository.save(newEmployee(empNo, name, EmployeeStatus.ACTIVE));
    }

    private Employee newEmployee(String empNo, String name, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setEmpNo(empNo);
        employee.setName(name);
        employee.setStatus(status);
        employee.setHireDate(LocalDate.of(2024, 1, 31));
        employee.setCreatedAt(LocalDateTime.of(2024, 1, 1, 9, 0, 0));
        employee.setUpdatedAt(LocalDateTime.of(2024, 1, 1, 9, 0, 0));
        return employee;
    }

    private Map<String, Object> validPayload(String empNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("empNo", empNo);
        payload.put("name", "张三");
        payload.put("department", "研发部");
        payload.put("position", "工程师");
        payload.put("phone", "13800138000");
        payload.put("email", "zhangsan@example.com");
        payload.put("hireDate", "2024-01-31");
        payload.put("status", "ACTIVE");
        return payload;
    }

    private Call post(Object payload) throws Exception {
        return perform(MockMvcRequestBuilders.post("/api/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(payload)));
    }

    private Call put(long id, Object payload) throws Exception {
        return perform(MockMvcRequestBuilders.put("/api/employees/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serialize(payload)));
    }

    private Call get(String url) throws Exception {
        return perform(MockMvcRequestBuilders.get(url));
    }

    private Call perform(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode node = body.isEmpty() ? objectMapper.createObjectNode() : objectMapper.readTree(body);
        return new Call(result.getResponse().getStatus(), node);
    }

    private String serialize(Object payload) throws Exception {
        String json = objectMapper.writeValueAsString(payload);
        assertNotNull(json);
        assertFalse(json.isBlank());
        return json;
    }

    private record Call(int status, JsonNode body) {

        int code() {
            return body.path("code").asInt(-1);
        }

        String message() {
            return body.path("message").asText();
        }

        JsonNode data() {
            return body.path("data");
        }
    }
}
