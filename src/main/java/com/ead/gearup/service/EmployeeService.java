package com.ead.gearup.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.EmployeeSearchResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.EmployeeNotFoundException;
import com.ead.gearup.exception.UnauthorizedCustomerAccessException;
import com.ead.gearup.exception.UserNotFoundException;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.AppointmentRepository;
import com.ead.gearup.repository.EmployeeRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.EmployeeDTOConverter;
import com.ead.gearup.validation.RequiresRole;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private EmployeeDTOConverter converter;

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Transactional
    public EmployeeResponseDTO createEmployee(CreateEmployeeDTO createEmployeeDTO) {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUser == null) {
            throw new UserNotFoundException("No authenticated user found");
        }

        if (currentUser.getRole() != UserRole.PUBLIC) {
            throw new UnauthorizedCustomerAccessException(
                    "Only public users can create an employee profile");
        }

        currentUser.setRole(UserRole.EMPLOYEE);
        userRepository.save(currentUser);

        // Convert DTO -> Employee Entity directly
        Employee employee = converter.convertToEntity(createEmployeeDTO);

        employee.setUser(currentUser);

        // Save Employee
        Employee savedEmployee = employeeRepository.save(employee);

        // Convert to Response DTO
        return converter.convertToResponseDto(savedEmployee);
    }

    public List<EmployeeResponseDTO> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        return employees.stream()
                .map(employee -> converter.convertToResponseDto(employee))
                .collect(Collectors.toList());
    }

    public EmployeeResponseDTO getEmployeeById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
        return converter.convertToResponseDto(employee);
    }

    @Transactional
    public EmployeeResponseDTO updateEmployee(Long id, UpdateEmployeeDTO updateEmployeeDTO) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }

        Employee existingEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));

        converter.updateEntityFromDto(existingEmployee, updateEmployeeDTO);

        Employee savedEmployee = employeeRepository.save(existingEmployee);
        return converter.convertToResponseDto(savedEmployee);
    }

    public Map<String, Object> checkEmployeeDependencies(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }

        // Verify employee exists
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));

        Map<String, Object> dependencies = new HashMap<>();

        // Check appointments assigned to this employee
        long appointmentCount = appointmentRepository.findAll().stream()
                .filter(apt -> apt.getEmployee() != null && apt.getEmployee().getEmployeeId().equals(employeeId))
                .count();

        dependencies.put("appointmentCount", appointmentCount);
        dependencies.put("hasAppointments", appointmentCount > 0);
        dependencies.put("canDelete", appointmentCount == 0);

        if (appointmentCount > 0) {
            dependencies.put("warningMessage",
                "This employee is assigned to " + appointmentCount + " appointment(s). " +
                "Please reassign or complete these appointments before deleting the employee.");
        }

        return dependencies;
    }

    @Transactional
    public void deleteEmployee(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("Invalid employee ID");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + employeeId));

        // Check dependencies before deletion
        Map<String, Object> dependencies = checkEmployeeDependencies(employeeId);
        boolean canDelete = (boolean) dependencies.get("canDelete");

        if (!canDelete) {
            throw new IllegalStateException(
                "Cannot delete employee. " + dependencies.get("warningMessage"));
        }

        // Handle linked User
        User linkedUser = employee.getUser();
        if (linkedUser != null) {
            linkedUser.setRole(UserRole.PUBLIC);
            employee.setUser(null);
            userRepository.save(linkedUser);
        }

        // Delete the employee safely
        employeeRepository.delete(employee);
    }

    @RequiresRole({UserRole.EMPLOYEE})
    public EmployeeResponseDTO getCurrentEmployee(){
        Long employeeId = currentUserService.getCurrentEntityId();
        return getEmployeeById(employeeId);
    }

    public List<EmployeeSearchResponseDTO> searchEmployeesByEmployeeName(String name) {
        return employeeRepository.findEmployeeSearchResultsNative(name)
                .stream()
                .map(p -> new EmployeeSearchResponseDTO(
                        p.getEmployeeId(),
                        new UserResponseDTO(
                                p.getName(),
                                p.getEmail()),
                        p.getSpecialization(),
                        p.getHireDate()))
                .collect(Collectors.toList());
    }
}
