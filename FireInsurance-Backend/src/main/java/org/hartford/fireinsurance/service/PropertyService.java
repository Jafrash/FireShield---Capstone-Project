package org.hartford.fireinsurance.service;


import org.hartford.fireinsurance.dto.CreatePropertyRequest;
import org.hartford.fireinsurance.dto.UpdatePropertyRequest;
import org.hartford.fireinsurance.exception.ResourceNotFoundException;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.repository.CustomerRepository;
import org.hartford.fireinsurance.repository.PropertyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    public PropertyService(PropertyRepository propertyRepository,
                          CustomerRepository customerRepository,
                          CustomerService customerService) {
        this.propertyRepository = propertyRepository;
        this.customerRepository = customerRepository;
        this.customerService = customerService;
    }

    /**
     * Create a new property for a customer
     */
    public Property createProperty(String username, CreatePropertyRequest request) {
        Customer customer = customerService.getCustomerByUsername(username);

        Property property = new Property();
        property.setCustomer(customer);
        property.setPropertyType(request.getPropertyType());
        property.setAddress(request.getAddress());
        property.setAreaSqft(request.getAreaSqft());
        property.setConstructionType(request.getConstructionType());
        property.setLatitude(request.getLatitude());
        property.setLongitude(request.getLongitude());
        property.setZipCode(request.getZipCode());
        property.setRiskScore(0.0); // default risk score

        return propertyRepository.save(property);
    }

    /**
     * Get all properties for a specific customer by username
     */
    public List<Property> getPropertiesByUsername(String username) {
        Customer customer = customerService.getCustomerByUsername(username);
        return propertyRepository.findByCustomer(customer);
    }

    /**
     * Update a property (with ownership check)
     */
    public Property updateProperty(String username, Long id, UpdatePropertyRequest request) {
        Property property = getPropertyById(id);

        // Security check: Ensure the property belongs to the authenticated customer
        if (!property.getCustomer().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access: Property does not belong to this customer");
        }

        // Update property fields (null-safe)
        if (request.getPropertyType() != null) {
            property.setPropertyType(request.getPropertyType());
        }
        if (request.getAddress() != null) {
            property.setAddress(request.getAddress());
        }
        if (request.getAreaSqft() != null) {
            property.setAreaSqft(request.getAreaSqft());
        }
        if (request.getConstructionType() != null) {
            property.setConstructionType(request.getConstructionType());
        }
        if (request.getRiskScore() != null) {
            property.setRiskScore(request.getRiskScore());
        }
        if (request.getLatitude() != null) {
            property.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            property.setLongitude(request.getLongitude());
        }
        if (request.getZipCode() != null) {
            property.setZipCode(request.getZipCode());
        }

        return propertyRepository.save(property);
    }

    /**
     * Delete a property (with ownership check)
     */
    public void deleteProperty(String username, Long id) {
        Property property = getPropertyById(id);

        // Security check: Ensure the property belongs to the authenticated customer
        if (!property.getCustomer().getUser().getUsername().equals(username)) {
            throw new RuntimeException("Unauthorized access: Property does not belong to this customer");
        }

        propertyRepository.delete(property);
    }

    /**
     * Get all properties (Admin only)
     */
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    /**
     * Get property by ID
     */
    public Property getPropertyById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with ID: " + id));
    }

    // ========== OLD METHODS (for backward compatibility) ==========

    public Property registerProperty(Property property) {
        return propertyRepository.save(property);
    }

    public List<Property> getCustomerProperties(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        return propertyRepository.findByCustomer(customer);
    }

    public Property addProperty(Long customerId, Property property) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        property.setCustomer(customer);
        return propertyRepository.save(property);
    }
}

