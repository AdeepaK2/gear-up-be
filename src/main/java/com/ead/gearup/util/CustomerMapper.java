package com.ead.gearup.util;

import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    // Map only profile-related fields
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "user", ignore = true) // User will be set manually in the service
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "profileImage", ignore = true)
    Customer toEntity(CustomerRequestDTO dto);

    // Convert Customer -> ResponseDTO
    @Mapping(source = "customerId", target = "customerId")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.name", target = "name")
    @Mapping(source = "user.isActive", target = "isActive")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "address", target = "address")
    @Mapping(source = "city", target = "city")
    @Mapping(source = "country", target = "country")
    @Mapping(source = "postalCode", target = "postalCode")
    CustomerResponseDTO toDto(Customer customer);
}
