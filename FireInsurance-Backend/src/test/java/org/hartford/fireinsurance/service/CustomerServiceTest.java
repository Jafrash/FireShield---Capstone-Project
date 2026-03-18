package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.CustomerDTO;
import org.hartford.fireinsurance.dto.CustomerRegistrationRequest;
import org.hartford.fireinsurance.dto.CustomerRegistrationResponse;
import org.hartford.fireinsurance.dto.UpdateCustomerRequest;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.CustomerRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomerService
 * Tests customer registration, CRUD operations, and profile management
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService Unit Tests")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    private User testUser;
    private Customer testCustomer;
    private CustomerRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testcustomer");
        testUser.setEmail("customer@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setPhoneNumber("1234567890");
        testUser.setRole("CUSTOMER");
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());

        // Setup test customer
        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);
        testCustomer.setUser(testUser);
        testCustomer.setAddress("123 Main St");
        testCustomer.setCity("TestCity");
        testCustomer.setState("TestState");

        // Setup registration request
        registrationRequest = new CustomerRegistrationRequest();
        registrationRequest.setUsername("newcustomer");
        registrationRequest.setEmail("new@example.com");
        registrationRequest.setPassword("password123");
        registrationRequest.setPhoneNumber("9876543210");
        registrationRequest.setAddress("456 Oak Ave");
        registrationRequest.setCity("NewCity");
        registrationRequest.setState("NewState");
    }

    // ========== registerCustomer() Tests ==========

    @Test
    @DisplayName("Test 1: Should register customer successfully")
    void testRegisterCustomerSuccess() {
        // Arrange
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // Act
        CustomerRegistrationResponse response = customerService.registerCustomer(registrationRequest);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(1L, response.getCustomerId());
        assertEquals("Customer registered successfully", response.getMessage());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Test 2: Should encode password during registration")
    void testRegisterCustomerPasswordEncoding() {
        // Arrange
        when(passwordEncoder.encode("password123")).thenReturn("secureEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("secureEncodedPassword", user.getPassword());
            return user;
        });
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // Act
        customerService.registerCustomer(registrationRequest);

        // Assert
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("Test 3: Should set customer role correctly during registration")
    void testRegisterCustomerSetsRole() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            assertEquals("CUSTOMER", user.getRole());
            assertTrue(user.getActive());
            return user;
        });
        when(customerRepository.save(any(Customer.class))).thenReturn(testCustomer);

        // Act
        customerService.registerCustomer(registrationRequest);

        // Assert
        verify(userRepository).save(argThat(user ->
            "CUSTOMER".equals(user.getRole()) && user.getActive()
        ));
    }

    // ========== getCustomer() Tests ==========

    @Test
    @DisplayName("Test 4: Should get customer by ID successfully")
    void testGetCustomerSuccess() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act
        Customer result = customerService.getCustomer(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getCustomerId());
        assertEquals("TestCity", result.getCity());
        verify(customerRepository).findById(1L);
    }

    @Test
    @DisplayName("Test 5: Should throw exception when customer not found")
    void testGetCustomerNotFound() {
        // Arrange
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerService.getCustomer(999L);
        });

        assertEquals("Customer not found with ID: 999", exception.getMessage());
    }

    // ========== getCustomerByUsername() Tests ==========

    @Test
    @DisplayName("Test 6: Should get customer by username successfully")
    void testGetCustomerByUsernameSuccess() {
        // Arrange
        when(userRepository.findByUsername("testcustomer")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUser(testUser)).thenReturn(Optional.of(testCustomer));

        // Act
        Customer result = customerService.getCustomerByUsername("testcustomer");

        // Assert
        assertNotNull(result);
        assertEquals("testcustomer", result.getUser().getUsername());
        verify(userRepository).findByUsername("testcustomer");
        verify(customerRepository).findByUser(testUser);
    }

    @Test
    @DisplayName("Test 7: Should throw exception when user not found by username")
    void testGetCustomerByUsernameUserNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerService.getCustomerByUsername("nonexistent");
        });

        assertTrue(exception.getMessage().contains("User not found"));
    }

    // ========== getAllCustomers() Tests ==========

    @Test
    @DisplayName("Test 8: Should get all customers successfully")
    void testGetAllCustomersSuccess() {
        // Arrange
        Customer customer2 = new Customer();
        customer2.setCustomerId(2L);
        List<Customer> customers = Arrays.asList(testCustomer, customer2);
        when(customerRepository.findAll()).thenReturn(customers);

        // Act
        List<Customer> result = customerService.getAllCustomers();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(customerRepository).findAll();
    }

    // ========== updateCustomer() Tests ==========

    @Test
    @DisplayName("Test 9: Should update customer successfully")
    void testUpdateCustomerSuccess() {
        // Arrange
        Customer updatedData = new Customer();
        updatedData.setAddress("789 New St");
        updatedData.setCity("UpdatedCity");
        updatedData.setState("UpdatedState");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = customerService.updateCustomer(1L, updatedData);

        // Assert
        assertNotNull(result);
        assertEquals("789 New St", result.getAddress());
        assertEquals("UpdatedCity", result.getCity());
        assertEquals("UpdatedState", result.getState());
        verify(customerRepository).save(testCustomer);
    }

    // ========== updateCustomerProfile() Tests ==========

    @Test
    @DisplayName("Test 10: Should update customer profile with all fields")
    void testUpdateCustomerProfileAllFields() {
        // Arrange
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest();
        updateRequest.setAddress("999 Update Rd");
        updateRequest.setCity("ProfileCity");
        updateRequest.setState("ProfileState");
        updateRequest.setPhoneNumber("5555555555");

        when(userRepository.findByUsername("testcustomer")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUser(testUser)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = customerService.updateCustomerProfile("testcustomer", updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("999 Update Rd", result.getAddress());
        assertEquals("ProfileCity", result.getCity());
        assertEquals("ProfileState", result.getState());
        assertEquals("5555555555", result.getUser().getPhoneNumber());
    }

    @Test
    @DisplayName("Test 11: Should update only provided fields in profile")
    void testUpdateCustomerProfilePartialUpdate() {
        // Arrange
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest();
        updateRequest.setCity("OnlyCity");
        // Other fields are null

        when(userRepository.findByUsername("testcustomer")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUser(testUser)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Customer result = customerService.updateCustomerProfile("testcustomer", updateRequest);

        // Assert
        assertEquals("OnlyCity", result.getCity());
        assertEquals("123 Main St", result.getAddress()); // Original address unchanged
        assertEquals("TestState", result.getState()); // Original state unchanged
    }

    // ========== deleteCustomer() Tests ==========

    @Test
    @DisplayName("Test 12: Should delete customer successfully")
    void testDeleteCustomerSuccess() {
        // Arrange
        when(customerRepository.existsById(1L)).thenReturn(true);
        doNothing().when(customerRepository).deleteById(1L);

        // Act
        customerService.deleteCustomer(1L);

        // Assert
        verify(customerRepository).existsById(1L);
        verify(customerRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Test 13: Should throw exception when deleting non-existent customer")
    void testDeleteCustomerNotFound() {
        // Arrange
        when(customerRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            customerService.deleteCustomer(999L);
        });

        assertEquals("Customer not found with ID: 999", exception.getMessage());
        verify(customerRepository, never()).deleteById(anyLong());
    }

    // ========== DTO Conversion Tests ==========

    @Test
    @DisplayName("Test 14: Should convert customer to DTO successfully")
    void testGetCustomerDTOSuccess() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(testCustomer));

        // Act
        CustomerDTO result = customerService.getCustomerDTO(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getCustomerId());
        assertEquals("testcustomer", result.getUsername());
        assertEquals("customer@example.com", result.getEmail());
        assertEquals("TestCity", result.getCity());
    }

    @Test
    @DisplayName("Test 15: Should get all customers as DTOs")
    void testGetAllCustomersDTOSuccess() {
        // Arrange
        Customer customer2 = new Customer();
        customer2.setCustomerId(2L);
        User user2 = new User();
        user2.setUsername("customer2");
        user2.setEmail("customer2@example.com");
        user2.setPhoneNumber("9999999999");
        customer2.setUser(user2);

        List<Customer> customers = Arrays.asList(testCustomer, customer2);
        when(customerRepository.findAll()).thenReturn(customers);

        // Act
        List<CustomerDTO> result = customerService.getAllCustomersDTO();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("testcustomer", result.get(0).getUsername());
        assertEquals("customer2", result.get(1).getUsername());
    }

    @Test
    @DisplayName("Test 16: Should get customer DTO by username")
    void testGetCustomerDTOByUsernameSuccess() {
        // Arrange
        when(userRepository.findByUsername("testcustomer")).thenReturn(Optional.of(testUser));
        when(customerRepository.findByUser(testUser)).thenReturn(Optional.of(testCustomer));

        // Act
        CustomerDTO result = customerService.getCustomerDTOByUsername("testcustomer");

        // Assert
        assertNotNull(result);
        assertEquals("testcustomer", result.getUsername());
        assertEquals("customer@example.com", result.getEmail());
    }
}

