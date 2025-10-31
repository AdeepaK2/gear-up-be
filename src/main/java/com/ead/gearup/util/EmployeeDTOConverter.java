package com.ead.gearup.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ead.gearup.dto.employee.CreateEmployeeDTO;
import com.ead.gearup.dto.employee.EmployeeResponseDTO;
import com.ead.gearup.dto.employee.UpdateEmployeeDTO;
import com.ead.gearup.model.Employee;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.UserRepository;

@Component
public class EmployeeDTOConverter {

    @Autowired
    private UserRepository userRepository;

    // Convert CreateEmployeeDTO to Employee entity
    public Employee convertToEntity(CreateEmployeeDTO dto) {
        Employee employee = new Employee();

        employee.setSpecialization(dto.getSpecialization());
        employee.setHireDate(dto.getHireDate());
        employee.setProfileImage(dto.getProfileImage());
        employee.setPhoneNumber(dto.getPhoneNumber());

        return employee;
    }

    // Convert Employee entity to EmployeeResponseDTO
    public EmployeeResponseDTO convertToResponseDto(Employee employee) {
        EmployeeResponseDTO dto = new EmployeeResponseDTO();
        dto.setEmployeeId(employee.getEmployeeId());

        dto.setEmail(employee.getUser().getEmail());
        dto.setName(employee.getUser().getName());
        dto.setSpecialization(employee.getSpecialization());
        dto.setHireDate(employee.getHireDate());
        dto.setCreatedAt(employee.getCreatedAt());
        dto.setUpdatedAt(employee.getUpdatedAt());
        dto.setProfileImage(employee.getProfileImage());
        dto.setPhoneNumber(employee.getPhoneNumber());

        return dto;
    }

    // Update employee entity from UpdateEmployeeDTO
    public void updateEntityFromDto(Employee employee, UpdateEmployeeDTO dto) {
        User user = employee.getUser();
        boolean userUpdated = false;

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            user.setName(dto.getName());
            userUpdated = true;
        }

        // Save user if it was updated
        if (userUpdated) {
            userRepository.save(user);
        }

        if (dto.getSpecialization() != null && !dto.getSpecialization().trim().isEmpty()) {
            employee.setSpecialization(dto.getSpecialization());
        }
        if (dto.getHireDate() != null) {
            employee.setHireDate(dto.getHireDate());
        }
        if (dto.getProfileImage() != null && !dto.getProfileImage().trim().isEmpty()) {
            employee.setProfileImage(dto.getProfileImage());
        }
        if (dto.getPhoneNumber() != null && !dto.getPhoneNumber().trim().isEmpty()) {
            employee.setPhoneNumber(dto.getPhoneNumber());
        }
    }
}
