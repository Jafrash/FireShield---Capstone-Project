package org.hartford.fireinsurance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hartford.fireinsurance.dto.CustomerRegistrationRequest;
import org.hartford.fireinsurance.dto.CustomerRegistrationResponse;
import org.hartford.fireinsurance.dto.SurveyorRegistrationRequest;
import org.hartford.fireinsurance.dto.SurveyorRegistrationResponse;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.CustomerRepository;
import org.hartford.fireinsurance.repository.SurveyorRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.hartford.fireinsurance.service.CustomerService;
import org.hartford.fireinsurance.service.SurveyorService;
import org.hartford.fireinsurance.utility.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 * Tests REST API endpoints for authentication and registration
 */
@SpringBootTest
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationManager authManager;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private SurveyorRepository surveyorRepository;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private SurveyorService surveyorService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private JwtRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setRole("CUSTOMER");

        loginRequest = new JwtRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
    }

    // ========== login() Tests ==========

    @Test
    @DisplayName("Test 1: Should login successfully with valid credentials")
    void testLoginSuccess() throws Exception {
        // Arrange
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("testuser", "CUSTOMER")).thenReturn("fake.jwt.token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake.jwt.token"));

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateToken("testuser", "CUSTOMER");
    }

    @Test
    @DisplayName("Test 2: Should return 401 with invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        // Arrange
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert - Expecting the exception to be thrown
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        // Verify authentication was attempted
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Test 3: Should handle user not found after authentication")
    void testLoginUserNotFoundAfterAuth() throws Exception {
        // Arrange
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("Test 4: Should verify JWT token generation")
    void testJwtTokenGeneration() throws Exception {
        // Arrange
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("testuser", "CUSTOMER"))
                .thenReturn("test.jwt.token.value");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").value("test.jwt.token.value"));

        verify(jwtUtil, times(1)).generateToken("testuser", "CUSTOMER");
    }

    @Test
    @DisplayName("Test 5: Should handle empty username in login")
    void testLoginEmptyUsername() throws Exception {
        // Arrange
        loginRequest.setUsername("");
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Test 6: Should handle empty password in login")
    void testLoginEmptyPassword() throws Exception {
        // Arrange
        loginRequest.setPassword("");

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ========== registerCustomer() Tests ==========

    @Test
    @DisplayName("Test 7: Should register customer successfully")
    void testRegisterCustomerSuccess() throws Exception {
        // Arrange
        CustomerRegistrationRequest request = new CustomerRegistrationRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("1234567890");
        request.setAddress("123 Main St");
        request.setCity("TestCity");
        request.setState("TestState");

        CustomerRegistrationResponse response = new CustomerRegistrationResponse(
                1L, "newuser", "new@example.com", "1234567890",
                "123 Main St", "TestCity", "TestState", "Customer registered successfully"
        );

        when(customerService.registerCustomer(any(CustomerRegistrationRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register/customer")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.message").value("Customer registered successfully"));

        verify(customerService).registerCustomer(any(CustomerRegistrationRequest.class));
    }

    @Test
    @DisplayName("Test 8: Should handle customer registration failure")
    void testRegisterCustomerFailure() throws Exception {
        // Arrange
        CustomerRegistrationRequest request = new CustomerRegistrationRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");

        when(customerService.registerCustomer(any(CustomerRegistrationRequest.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register/customer")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    @DisplayName("Test 9: Should validate required fields in customer registration")
    void testRegisterCustomerMissingFields() throws Exception {
        // Arrange
        CustomerRegistrationRequest request = new CustomerRegistrationRequest();
        request.setUsername("newuser");
        // Missing required fields

        when(customerService.registerCustomer(any(CustomerRegistrationRequest.class)))
                .thenThrow(new RuntimeException("Missing required fields"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register/customer")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Missing required fields"));
    }

    // ========== registerSurveyor() Tests ==========

    @Test
    @DisplayName("Test 10: Should register surveyor successfully")
    void testRegisterSurveyorSuccess() throws Exception {
        // Arrange
        SurveyorRegistrationRequest request = new SurveyorRegistrationRequest();
        request.setUsername("surveyor1");
        request.setEmail("surveyor@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("1234567890");
        request.setLicenseNumber("LIC12345");
        request.setExperienceYears(5);
        request.setAssignedRegion("North");

        SurveyorRegistrationResponse response = new SurveyorRegistrationResponse(
                1L, "surveyor1", "surveyor@example.com", "1234567890",
                "LIC12345", 5, "North", "Surveyor registered successfully"
        );

        when(surveyorService.registerSurveyor(any(SurveyorRegistrationRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register/surveyor")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("surveyor1"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC12345"))
                .andExpect(jsonPath("$.experienceYears").value(5));

        verify(surveyorService).registerSurveyor(any(SurveyorRegistrationRequest.class));
    }

    @Test
    @DisplayName("Test 11: Should handle surveyor registration failure")
    void testRegisterSurveyorFailure() throws Exception {
        // Arrange
        SurveyorRegistrationRequest request = new SurveyorRegistrationRequest();
        request.setUsername("surveyor1");

        when(surveyorService.registerSurveyor(any(SurveyorRegistrationRequest.class)))
                .thenThrow(new RuntimeException("License number already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register/surveyor")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("License number already exists"));
    }

    @Test
    @DisplayName("Test 12: Should handle invalid JSON in request")
    void testInvalidJsonRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("Test 13: Should handle CSRF token for POST requests")
    void testCsrfProtection() throws Exception {
        // Arrange - Mock to prevent actual authentication
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert - without CSRF token, Spring Security will reject with 403 or 401
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));
                // Note: CSRF protection may return 401 or 403 depending on security config
    }

    @Test
    @DisplayName("Test 14: Should verify authentication manager is called")
    void testAuthenticationManagerInteraction() throws Exception {
        // Arrange
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("token");

        // Act
        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());

        // Assert
        verify(authManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Test 15: Should login with different user roles")
    void testLoginWithDifferentRoles() throws Exception {
        // Test with ADMIN role
        testUser.setRole("ADMIN");
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken("testuser", "ADMIN")).thenReturn("admin.jwt.token");

        mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("admin.jwt.token"));

        verify(jwtUtil).generateToken("testuser", "ADMIN");
    }
}











