package com.ead.gearup.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.ead.gearup.dto.appointment.AppointmentSearchResponseDTO;
import com.ead.gearup.dto.customer.CustomerSearchResponseDTO;
import com.ead.gearup.dto.employee.EmployeeSearchResponseDTO;
import com.ead.gearup.dto.task.TaskSearchResponseDTO;
import com.ead.gearup.service.AppointmentService;
import com.ead.gearup.service.CustomerService;
import com.ead.gearup.service.EmployeeService;
import com.ead.gearup.service.TaskService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SearchGraphQLController {

    private final AppointmentService appointmentService;
    private final CustomerService customerService;
    private final EmployeeService employeeService;
    private final TaskService taskService;

    @QueryMapping
    public List<AppointmentSearchResponseDTO> searchAppointmentsByCustomerName(@Argument String name) {
        return appointmentService.searchAppointmentsByCustomerName(name);
    }

    @QueryMapping
    public List<CustomerSearchResponseDTO> searchCustomersByCustomerName(@Argument String name) {
        return customerService.searchCustomersByCustomerName(name);
    }

    @QueryMapping
    public List<EmployeeSearchResponseDTO> searchEmployeesByEmployeeName(@Argument String name) {
        return employeeService.searchEmployeesByEmployeeName(name);
    }

    @QueryMapping
    public List<TaskSearchResponseDTO> searchTasksByTaskName(@Argument String name) {
        return taskService.searchTasksByTaskName(name);
    }

}
