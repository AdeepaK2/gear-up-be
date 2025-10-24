package com.ead.gearup.service;

import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.dto.customer.CustomerSearchResponseDTO;
import com.ead.gearup.dto.customer.CustomerUpdateDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.exception.UnauthorizedCustomerAccessException;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.CustomerMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final CustomerMapper customerMapper;
    private final CurrentUserService currentUserService;

    public List<CustomerResponseDTO> getAll() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toDto)
                .collect(Collectors.toList());
    }

    public CustomerResponseDTO getById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        return customerRepository.findById(id)
                .map(customerMapper::toDto)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
    }

    @Transactional
    public CustomerResponseDTO create(CustomerRequestDTO dto) {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }

        if (currentUser.getRole() != UserRole.PUBLIC) {
            throw new UnauthorizedCustomerAccessException(
                    "Only public users can create a customer profile");
        }

        currentUser.setRole(UserRole.CUSTOMER);
        userRepository.save(currentUser);

        Customer customer = customerMapper.toEntity(dto);
        if (customer == null) {
            throw new IllegalStateException("Failed to map customer request");
        }

        customer.setUser(currentUser);

        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponseDTO update(Long id, @Valid CustomerUpdateDTO dto) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

        if (dto.getName() != null) {
            User currentUser = currentUserService.getCurrentUser();
            currentUser.setName(dto.getName());
            userRepository.save(currentUser);
        }

        if (dto.getPhoneNumber() != null) {
            customer.setPhoneNumber(dto.getPhoneNumber());
        }

        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid customer ID");
        }

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));

        // Handle linked User
        User linkedUser = customer.getUser();
        if (linkedUser != null) {
            linkedUser.setRole(UserRole.PUBLIC);
            customer.setUser(null);
            userRepository.save(linkedUser);
        }

        customerRepository.delete(customer);
    }

    public List<CustomerSearchResponseDTO> searchCustomersByCustomerName(String name) {
        return customerRepository.findCustomerSearchResultsNative(name)
                .stream()
                .map(p -> new CustomerSearchResponseDTO(
                        p.getCustomerId(),
                        new UserResponseDTO(
                                p.getName(),
                                p.getEmail()
                        ),
                        p.getPhoneNumber()))
                .collect(Collectors.toList());
    }

}
