package com.ead.gearup.dto.customer;

import com.ead.gearup.dto.response.UserResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerSearchResponseDTO {
    private Long customerId;
    private UserResponseDTO user;
    private String phoneNumber;
}
