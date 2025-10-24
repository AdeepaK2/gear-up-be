package com.ead.gearup.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.dto.response.ApiResponseDTO;
import com.ead.gearup.model.Task;
import com.ead.gearup.service.EmployeeService;
import com.ead.gearup.service.TaskService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/employees")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TaskService taskService;

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.PUBLIC})
    @PostMapping
    public ResponseEntity<ApiResponseDTO<EmployeeResponseDTO>> createEmployee(
            @Valid @RequestBody CreateEmployeeDTO createEmployeeDTO, HttpServletRequest request) {

        EmployeeResponseDTO createdEmployee = employeeService.createEmployee(createEmployeeDTO);

        ApiResponseDTO<EmployeeResponseDTO> response = ApiResponseDTO.<EmployeeResponseDTO>builder()
                .status("success")
                .message("Employee created successfully")
                .data(createdEmployee)
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.CUSTOMER,
    // UserRole.PUBLIC})
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<EmployeeResponseDTO>>> getAllEmployees(HttpServletRequest request) {

        List<EmployeeResponseDTO> employees = employeeService.getAllEmployees();

        ApiResponseDTO<List<EmployeeResponseDTO>> response = ApiResponseDTO.<List<EmployeeResponseDTO>>builder()
                .status("success")
                .message("Employees retrieved successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(employees)
                .build();

        return ResponseEntity.ok(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.CUSTOMER,
    // UserRole.PUBLIC})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<EmployeeResponseDTO>> getEmployeeById(@PathVariable Long id,
            HttpServletRequest request) {

        EmployeeResponseDTO employee = employeeService.getEmployeeById(id);

        ApiResponseDTO<EmployeeResponseDTO> response = ApiResponseDTO.<EmployeeResponseDTO>builder()
                .status("success")
                .message("Employee retrieved successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(employee)
                .build();

        return ResponseEntity.ok(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.PUBLIC})
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<EmployeeResponseDTO>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeDTO updateEmployeeDTO,
            HttpServletRequest request) {

        EmployeeResponseDTO updatedEmployee = employeeService.updateEmployee(id, updateEmployeeDTO);

        ApiResponseDTO<EmployeeResponseDTO> response = ApiResponseDTO.<EmployeeResponseDTO>builder()
                .status("success")
                .message("Employee updated successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(updatedEmployee)
                .build();

        return ResponseEntity.ok(response);
    }

    // @RequiresRole({UserRole.ADMIN, UserRole.EMPLOYEE, UserRole.PUBLIC})
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteEmployee(@PathVariable Long id, HttpServletRequest request) {

        employeeService.deleteEmployee(id);

        ApiResponseDTO<Void> response = ApiResponseDTO.<Void>builder()
                .status("success")
                .message("Employee deleted successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    // Dashboard
    @GetMapping("/task-summary")
    public ResponseEntity<ApiResponseDTO<Map<String, Long>>> getTaskSummaryForEmployee(HttpServletRequest request) {
        Map<String, Long> taskSummary = taskService.getTaskSummaryForEmployee();

        ApiResponseDTO<Map<String, Long>> response = ApiResponseDTO.<Map<String, Long>>builder()
                .status("success")
                .message("Task summary retrieved successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(taskSummary)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseDTO<EmployeeResponseDTO>> getCurrentEmployee(HttpServletRequest request) {
        EmployeeResponseDTO currentEmployee = employeeService.getCurrentEmployee();

        ApiResponseDTO<EmployeeResponseDTO> response = ApiResponseDTO.<EmployeeResponseDTO>builder()
                .status("success")
                .message("Current employee retrieved successfully")
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .data(currentEmployee)
                .build();

        return ResponseEntity.ok(response);
    }

}
