package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.CustomerDTO;
import org.hartford.fireinsurance.dto.UpdateCustomerRequest;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // ================================
    // ADMIN - Get All Customers
    // ================================
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomersDTO());
    }

    // ================================
    // ADMIN - Get Customer By ID
    // ================================
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerDTO(id));
    }

    // ================================
    // CUSTOMER - Get Own Profile
    // ================================
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerDTO> getMyProfile(Authentication authentication) {

        String username = authentication.getName();

        CustomerDTO customer = customerService.getCustomerDTOByUsername(username);

        return ResponseEntity.ok(customer);
    }

    // ================================
    // CUSTOMER - Update Own Profile
    // ================================
    @PutMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CustomerDTO> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateCustomerRequest request) {

        String username = authentication.getName();

        Customer updatedCustomer = customerService.updateCustomerProfile(username, request);
        CustomerDTO response = new CustomerDTO(
                updatedCustomer.getCustomerId(),
                updatedCustomer.getUser().getUsername(),
                updatedCustomer.getUser().getEmail(),
                updatedCustomer.getUser().getPhoneNumber(),
                updatedCustomer.getAddress(),
                updatedCustomer.getCity(),
                updatedCustomer.getState()
        );

        return ResponseEntity.ok(response);
    }

    // ================================
    // ADMIN - Delete Customer
    // ================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteCustomer(@PathVariable Long id) {

        customerService.deleteCustomer(id);

        return ResponseEntity.ok("Customer deleted successfully");
    }
}