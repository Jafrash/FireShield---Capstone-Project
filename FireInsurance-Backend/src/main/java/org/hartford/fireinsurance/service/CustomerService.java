package org.hartford.fireinsurance.service;


import org.hartford.fireinsurance.controller.CustomerForgotPasswordRequest;
import org.hartford.fireinsurance.dto.CustomerDTO;
import org.hartford.fireinsurance.dto.CustomerRegistrationRequest;
import org.hartford.fireinsurance.dto.CustomerRegistrationResponse;
import org.hartford.fireinsurance.dto.UpdateCustomerRequest;
import org.hartford.fireinsurance.exception.ResourceNotFoundException;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.CustomerRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerService(CustomerRepository customerRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

    }

    public CustomerRegistrationResponse registerCustomer(CustomerRegistrationRequest request) {
        // Create User entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole("CUSTOMER");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Create Customer entity
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());

        // Save entities
        userRepository.save(user);
        customer.setUser(user);
        Customer savedCustomer = customerRepository.save(customer);

        // Build and return response DTO
        return new CustomerRegistrationResponse(
            savedCustomer.getCustomerId(),
            savedCustomer.getUser().getUsername(),
            savedCustomer.getUser().getEmail(),
            savedCustomer.getUser().getPhoneNumber(),
            savedCustomer.getAddress(),
            savedCustomer.getCity(),
            savedCustomer.getState(),
            "Customer registered successfully"
        );
    }

    public Customer createCustomer(Customer customer) {
        User user = customer.getUser();
        user.setRole("CUSTOMER");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        customer.setUser(user);

        return customerRepository.save(customer);
    }



    public Customer getCustomer(Long id) {

        return customerRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found with ID: " + id));
    }

    public Customer getCustomerByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
        return customerRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found for user: " + username));
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer updateCustomer(Long id, Customer customer) {
        Customer existingCustomer = getCustomer(id);
        existingCustomer.setAddress(customer.getAddress());
        existingCustomer.setCity(customer.getCity());
        existingCustomer.setState(customer.getState());
        return customerRepository.save(existingCustomer);
    }

    /**
     * Update customer profile using DTO (for /customers/me endpoint)
     */
    public Customer updateCustomerProfile(String username, UpdateCustomerRequest request) {
        Customer customer = getCustomerByUsername(username);

        // Update customer fields
        if (request.getAddress() != null) {
            customer.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            customer.setCity(request.getCity());
        }
        if (request.getState() != null) {
            customer.setState(request.getState());
        }

        // Update user phone number
        if (request.getPhoneNumber() != null) {
            customer.getUser().setPhoneNumber(request.getPhoneNumber());
        }

        return customerRepository.save(customer);
    }

    public void resetCustomerPassword(CustomerForgotPasswordRequest request) {
        if (request == null
                || isBlank(request.getUsername())
                || isBlank(request.getEmail())
                || isBlank(request.getPhoneNumber())
                || isBlank(request.getNewPassword())) {
            throw new IllegalArgumentException("All customer verification details are required.");
        }

        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String phoneNumber = request.getPhoneNumber().trim();
        String newPassword = request.getNewPassword().trim();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Unable to reset password with the provided account details."));

        if (!"CUSTOMER".equalsIgnoreCase(user.getRole())
                || user.getEmail() == null
                || !email.equals(user.getEmail().trim().toLowerCase(Locale.ROOT))
                || user.getPhoneNumber() == null
                || !phoneNumber.equals(user.getPhoneNumber().trim())) {
            throw new RuntimeException("Unable to reset password with the provided account details.");
        }

        Customer customer = customerRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Unable to reset password with the provided account details."));

        if (customer.getCustomerId() == null) {
            throw new RuntimeException("Unable to reset password with the provided account details.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }

    // ========== DTO CONVERSION METHODS ==========

    /**
     * Convert Customer entity to CustomerDTO (without sensitive data)
     */
    private CustomerDTO convertToDTO(Customer customer) {
        return new CustomerDTO(
            customer.getCustomerId(),
            customer.getUser().getUsername(),
            customer.getUser().getEmail(),
            customer.getUser().getPhoneNumber(),
            customer.getAddress(),
            customer.getCity(),
            customer.getState()
        );
    }

    /**
     * Get customer by ID and return as DTO
     */
    public CustomerDTO getCustomerDTO(Long id) {
        Customer customer = getCustomer(id);
        return convertToDTO(customer);
    }

    /**
     * Get customer by username and return as DTO
     */
    public CustomerDTO getCustomerDTOByUsername(String username) {
        Customer customer = getCustomerByUsername(username);
        return convertToDTO(customer);
    }

    /**
     * Get all customers as DTOs
     */
    public List<CustomerDTO> getAllCustomersDTO() {
        return customerRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update customer and return as DTO
     */
    public CustomerDTO updateCustomerDTO(Long id, Customer customer) {
        Customer updated = updateCustomer(id, customer);
        return convertToDTO(updated);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
