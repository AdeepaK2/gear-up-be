package com.ead.gearup.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ead.gearup.dto.customer.CustomerRequestDTO;
import com.ead.gearup.dto.customer.CustomerResponseDTO;
import com.ead.gearup.dto.customer.CustomerUpdateDTO;
import com.ead.gearup.dto.customer.CustomerDashboardDTO;
import com.ead.gearup.dto.customer.CustomerHeaderDTO;
import com.ead.gearup.enums.UserRole;
import com.ead.gearup.exception.CustomerNotFoundException;
import com.ead.gearup.exception.UnauthorizedCustomerAccessException;
import com.ead.gearup.model.Customer;
import com.ead.gearup.model.User;
import com.ead.gearup.repository.CustomerRepository;
import com.ead.gearup.repository.UserRepository;
import com.ead.gearup.service.CustomerService;
import com.ead.gearup.service.EmailService;
import com.ead.gearup.service.auth.CurrentUserService;
import com.ead.gearup.util.CustomerMapper;

@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CustomerService customerService;

    private Customer testCustomer;
    private User testUser;
    private CustomerResponseDTO testCustomerResponseDTO;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test Customer");
        testUser.setRole(UserRole.PUBLIC);
        testUser.setIsActive(true);

        // Setup test customer
        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);
        testCustomer.setUser(testUser);
        testCustomer.setPhoneNumber("1234567890");
        testCustomer.setAddress("123 Test St");
        testCustomer.setCity("Test City");
        testCustomer.setCountry("Test Country");
        testCustomer.setPostalCode("12345");

        // Setup test response DTO
        testCustomerResponseDTO = new CustomerResponseDTO();
        testCustomerResponseDTO.setCustomerId(1L);
        testCustomerResponseDTO.setEmail("test@example.com");
        testCustomerResponseDTO.setName("Test Customer");
    }

    // ========== getAll() Tests ==========
    @Test
    void testGetAllCustomers_Success() {
        // Arrange
        List<Customer> customers = Arrays.asList(testCustomer);
        when(customerRepository.findAll()).thenReturn(customers);
        when(customerMapper.toDto(any(Customer.class))).thenReturn(testCustomerResponseDTO);

        // Act
        List<CustomerResponseDTO> result = customerService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).getEmail());
        verify(customerRepository, times(1)).findAll();
        verify(customerMapper, times(1)).toDto(any(Customer.class));
    }

    @Test
    void testGetAllCustomers_EmptyList() {
        // Arrange
        when(customerRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<CustomerResponseDTO> result = customerService.getAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(customerRepository, times(1)).findAll();
    }

    // ========== getById() Tests ==========
    @Test
    void testGetById_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerMapper.toDto(testCustomer)).thenReturn(testCustomerResponseDTO);

        // Act
        CustomerResponseDTO result = customerService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getCustomerId());
        assertEquals("test@example.com", result.getEmail());
        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    void testGetById_NotFound() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> customerService.getById(999L));
        verify(customerRepository, times(1)).findById(999L);
    }

    @Test
    void testGetById_InvalidId_Null() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> customerService.getById(null));
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void testGetById_InvalidId_Zero() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> customerService.getById(0L));
        verify(customerRepository, never()).findById(any());
    }

    @Test
    void testGetById_InvalidId_Negative() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> customerService.getById(-1L));
        verify(customerRepository, never()).findById(any());
    }

    // ========== getCustomer() Tests ==========
    @Test
    void testGetCustomer_Success() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerMapper.toDto(testCustomer)).thenReturn(testCustomerResponseDTO);

        // Act
        CustomerResponseDTO result = customerService.getCustomer();

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getCustomerId());
        verify(currentUserService, times(1)).getCurrentEntityId();
        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    void testGetCustomer_NotFound() {
        // Arrange
        when(currentUserService.getCurrentEntityId()).thenReturn(999L);
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> customerService.getCustomer());
    }

    // ========== create() Tests ==========
    @Test
    void testCreate_Success() {
        // Arrange
        CustomerRequestDTO requestDTO = new CustomerRequestDTO();
        requestDTO.setPhoneNumber("1234567890");

        when(currentUserService.getCurrentUser()).thenReturn(testUser);
        when(customerMapper.toEntity(requestDTO)).thenReturn(testCustomer);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);
        when(customerMapper.toDto(testCustomer)).thenReturn(testCustomerResponseDTO);

        // Act
        CustomerResponseDTO result = customerService.create(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository, times(1)).save(testUser);
        verify(customerRepository, times(1)).save(any(Customer.class));
        assertEquals(UserRole.CUSTOMER, testUser.getRole());
    }

    @Test
    void testCreate_NoAuthenticatedUser() {
        // Arrange
        CustomerRequestDTO requestDTO = new CustomerRequestDTO();
        when(currentUserService.getCurrentUser()).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> customerService.create(requestDTO));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void testCreate_UserNotPublicRole() {
        // Arrange
        testUser.setRole(UserRole.ADMIN);
        CustomerRequestDTO requestDTO = new CustomerRequestDTO();
        when(currentUserService.getCurrentUser()).thenReturn(testUser);

        // Act & Assert
        assertThrows(UnauthorizedCustomerAccessException.class, () -> customerService.create(requestDTO));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void testCreate_MappingFails() {
        // Arrange
        CustomerRequestDTO requestDTO = new CustomerRequestDTO();
        when(currentUserService.getCurrentUser()).thenReturn(testUser);
        when(customerMapper.toEntity(requestDTO)).thenReturn(null);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> customerService.create(requestDTO));
        verify(customerRepository, never()).save(any());
    }

    // ========== update() Tests ==========
    @Test
    void testUpdate_Success_AllFields() {
        // Arrange
        CustomerUpdateDTO updateDTO = new CustomerUpdateDTO();
        updateDTO.setName("Updated Name");
        updateDTO.setPhoneNumber("9876543210");
        updateDTO.setAddress("456 New St");
        updateDTO.setCity("New City");
        updateDTO.setCountry("New Country");
        updateDTO.setPostalCode("54321");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(currentUserService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(customerRepository.save(testCustomer)).thenReturn(testCustomer);
        when(customerMapper.toDto(testCustomer)).thenReturn(testCustomerResponseDTO);

        // Act
        CustomerResponseDTO result = customerService.update(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", testUser.getName());
        assertEquals("9876543210", testCustomer.getPhoneNumber());
        assertEquals("456 New St", testCustomer.getAddress());
        verify(customerRepository, times(1)).save(testCustomer);
    }

    @Test
    void testUpdate_PartialUpdate_OnlyName() {
        // Arrange
        CustomerUpdateDTO updateDTO = new CustomerUpdateDTO();
        updateDTO.setName("Updated Name");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(currentUserService.getCurrentUser()).thenReturn(testUser);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(customerRepository.save(testCustomer)).thenReturn(testCustomer);
        when(customerMapper.toDto(testCustomer)).thenReturn(testCustomerResponseDTO);

        // Act
        CustomerResponseDTO result = customerService.update(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", testUser.getName());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testUpdate_CustomerNotFound() {
        // Arrange
        CustomerUpdateDTO updateDTO = new CustomerUpdateDTO();
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> customerService.update(999L, updateDTO));
        verify(customerRepository, never()).save(any());
    }

    @Test
    void testUpdate_InvalidId() {
        // Arrange
        CustomerUpdateDTO updateDTO = new CustomerUpdateDTO();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> customerService.update(null, updateDTO));
        assertThrows(IllegalArgumentException.class, () -> customerService.update(0L, updateDTO));
        verify(customerRepository, never()).save(any());
    }

    // ========== delete() Tests ==========
    @Test
    void testDelete_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        customerService.delete(1L);

        // Assert
        assertEquals(UserRole.PUBLIC, testUser.getRole());
        assertNull(testCustomer.getUser());
        verify(userRepository, times(1)).save(testUser);
        verify(customerRepository, times(1)).delete(testCustomer);
    }

    @Test
    void testDelete_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> customerService.delete(999L));
        verify(customerRepository, never()).delete(any());
    }

    @Test
    void testDelete_InvalidId() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> customerService.delete(null));
        assertThrows(IllegalArgumentException.class, () -> customerService.delete(-1L));
        verify(customerRepository, never()).delete(any());
    }

    // ========== getHeaderInfo() Tests ==========
    @Test
    void testGetHeaderInfo_Success() {
        // Arrange
        testCustomer.setProfileImage("profile.jpg");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act
        CustomerHeaderDTO result = customerService.getHeaderInfo(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Test Customer", result.getName());
        assertEquals("profile.jpg", result.getProfileImage());
        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    void testGetHeaderInfo_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> customerService.getHeaderInfo(999L));
    }

    // ========== getDashboard() Tests ==========
    @Test
    void testGetDashboard_Success() {
        // Arrange
        testCustomer.setAppointments(Arrays.asList());
        testCustomer.setVehicles(Arrays.asList());
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act
        CustomerDashboardDTO result = customerService.getDashboard(1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getProfile());
        assertNotNull(result.getSummary());
        assertNotNull(result.getRecentActivities());
        assertNotNull(result.getVehicles());
        assertEquals("Test Customer", result.getProfile().getName());
        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    void testGetDashboard_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> customerService.getDashboard(999L));
    }

    // ========== getCustomerIdByEmail() Tests ==========
    @Test
    void testGetCustomerIdByEmail_Success() {
        // Arrange
        when(customerRepository.findByUserEmail("test@example.com")).thenReturn(Optional.of(testCustomer));

        // Act
        Long result = customerService.getCustomerIdByEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(1L, result);
        verify(customerRepository, times(1)).findByUserEmail("test@example.com");
    }

    @Test
    void testGetCustomerIdByEmail_NotFound() {
        // Arrange
        when(customerRepository.findByUserEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, 
            () -> customerService.getCustomerIdByEmail("unknown@example.com"));
    }

    // ========== deactivateCustomer() Tests ==========
    @Test
    void testDeactivateCustomer_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(userRepository.save(testUser)).thenReturn(testUser);
        doNothing().when(emailService).sendCustomerDeactivationEmail(anyString(), anyString(), anyString());

        // Act
        customerService.deactivateCustomer(1L, "Policy violation");

        // Assert
        assertFalse(testUser.getIsActive());
        verify(userRepository, times(1)).save(testUser);
        verify(emailService, times(1)).sendCustomerDeactivationEmail(
            eq("test@example.com"), 
            eq("Test Customer"), 
            eq("Policy violation")
        );
    }

    @Test
    void testDeactivateCustomer_WithDefaultReason() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(userRepository.save(testUser)).thenReturn(testUser);
        doNothing().when(emailService).sendCustomerDeactivationEmail(anyString(), anyString(), anyString());

        // Act
        customerService.deactivateCustomer(1L, null);

        // Assert
        assertFalse(testUser.getIsActive());
        verify(emailService, times(1)).sendCustomerDeactivationEmail(
            anyString(), 
            anyString(), 
            eq("Administrative review or policy violation")
        );
    }

    @Test
    void testDeactivateCustomer_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, 
            () -> customerService.deactivateCustomer(999L, "Test reason"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testDeactivateCustomer_NoLinkedUser() {
        // Arrange
        testCustomer.setUser(null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act & Assert
        assertThrows(IllegalStateException.class, 
            () -> customerService.deactivateCustomer(1L, "Test reason"));
        verify(userRepository, never()).save(any());
    }

    // ========== reactivateCustomer() Tests ==========
    @Test
    void testReactivateCustomer_Success() {
        // Arrange
        testUser.setIsActive(false);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // Act
        customerService.reactivateCustomer(1L);

        // Assert
        assertTrue(testUser.getIsActive());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testReactivateCustomer_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, 
            () -> customerService.reactivateCustomer(999L));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testReactivateCustomer_NoLinkedUser() {
        // Arrange
        testCustomer.setUser(null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act & Assert
        assertThrows(IllegalStateException.class, 
            () -> customerService.reactivateCustomer(1L));
        verify(userRepository, never()).save(any());
    }
}
