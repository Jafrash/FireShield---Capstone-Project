package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.CreatePropertyRequest;
import org.hartford.fireinsurance.dto.UpdatePropertyRequest;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.CustomerRepository;
import org.hartford.fireinsurance.repository.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PropertyServiceTest {

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private PropertyService service;

    private Customer customer;
    private Property property;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setUsername("customer1");

        customer = new Customer();
        customer.setCustomerId(1L);
        customer.setUser(user);

        property = new Property();
        property.setPropertyId(1L);
        property.setCustomer(customer);
        property.setPropertyType("Residential");
        property.setAddress("123 Main St");
        property.setAreaSqft(2000.0);
        property.setConstructionType("Concrete");
        property.setRiskScore(0.0);
    }

    @Test
    void testCreateProperty_Success() {
        CreatePropertyRequest request = new CreatePropertyRequest();
        request.setPropertyType("Residential");
        request.setAddress("123 Main St");
        request.setAreaSqft(2000.0);
        request.setConstructionType("Concrete");

        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(propertyRepository.save(any(Property.class))).thenReturn(property);

        Property result = service.createProperty("customer1", request);

        assertNotNull(result);
        assertEquals("Residential", result.getPropertyType());
        assertEquals("123 Main St", result.getAddress());
        assertEquals(2000.0, result.getAreaSqft());
        assertEquals("Concrete", result.getConstructionType());
        assertEquals(0.0, result.getRiskScore());
        verify(customerService).getCustomerByUsername("customer1");
        verify(propertyRepository).save(any(Property.class));
    }

    @Test
    void testCreateProperty_DefaultRiskScore() {
        CreatePropertyRequest request = new CreatePropertyRequest();
        request.setPropertyType("Commercial");
        request.setAddress("456 Business Ave");
        request.setAreaSqft(3000.0);
        request.setConstructionType("Steel");

        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> {
            Property saved = invocation.getArgument(0);
            saved.setPropertyId(2L);
            return saved;
        });

        Property result = service.createProperty("customer1", request);

        assertNotNull(result);
        assertEquals(0.0, result.getRiskScore());
        verify(propertyRepository).save(any(Property.class));
    }

    @Test
    void testGetPropertiesByUsername_Success() {
        Property property2 = new Property();
        property2.setPropertyId(2L);
        property2.setCustomer(customer);

        List<Property> properties = Arrays.asList(property, property2);

        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(propertyRepository.findByCustomer(customer)).thenReturn(properties);

        List<Property> result = service.getPropertiesByUsername("customer1");

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(customerService).getCustomerByUsername("customer1");
        verify(propertyRepository).findByCustomer(customer);
    }

    @Test
    void testGetPropertiesByUsername_EmptyList() {
        when(customerService.getCustomerByUsername("customer1")).thenReturn(customer);
        when(propertyRepository.findByCustomer(customer)).thenReturn(Arrays.asList());

        List<Property> result = service.getPropertiesByUsername("customer1");

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(propertyRepository).findByCustomer(customer);
    }

    @Test
    void testUpdateProperty_Success() {
        UpdatePropertyRequest request = new UpdatePropertyRequest();
        request.setPropertyType("Commercial");
        request.setAddress("789 New St");
        request.setAreaSqft(2500.0);
        request.setConstructionType("Brick");
        request.setRiskScore(5.0);

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenReturn(property);

        Property result = service.updateProperty("customer1", 1L, request);

        assertNotNull(result);
        assertEquals("Commercial", result.getPropertyType());
        assertEquals("789 New St", result.getAddress());
        assertEquals(2500.0, result.getAreaSqft());
        assertEquals("Brick", result.getConstructionType());
        assertEquals(5.0, result.getRiskScore());
        verify(propertyRepository).save(any(Property.class));
    }

    @Test
    void testUpdateProperty_PartialUpdate() {
        UpdatePropertyRequest request = new UpdatePropertyRequest();
        request.setAddress("Updated Address");

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenReturn(property);

        Property result = service.updateProperty("customer1", 1L, request);

        assertNotNull(result);
        assertEquals("Updated Address", result.getAddress());
        assertEquals("Residential", result.getPropertyType());
        verify(propertyRepository).save(any(Property.class));
    }

    @Test
    void testUpdateProperty_UnauthorizedAccess() {
        UpdatePropertyRequest request = new UpdatePropertyRequest();
        request.setAddress("New Address");

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("customer2");

        Customer otherCustomer = new Customer();
        otherCustomer.setCustomerId(2L);
        otherCustomer.setUser(otherUser);

        property.setCustomer(otherCustomer);

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.updateProperty("customer1", 1L, request);
        });

        assertTrue(exception.getMessage().contains("Unauthorized"));
        verify(propertyRepository, never()).save(any());
    }

    @Test
    void testUpdateProperty_NullFields() {
        UpdatePropertyRequest request = new UpdatePropertyRequest();

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        when(propertyRepository.save(any(Property.class))).thenReturn(property);

        Property result = service.updateProperty("customer1", 1L, request);

        assertNotNull(result);
        assertEquals("Residential", result.getPropertyType());
        assertEquals("123 Main St", result.getAddress());
        verify(propertyRepository).save(any(Property.class));
    }

    @Test
    void testDeleteProperty_Success() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));
        doNothing().when(propertyRepository).delete(property);

        service.deleteProperty("customer1", 1L);

        verify(propertyRepository).delete(property);
    }

    @Test
    void testDeleteProperty_UnauthorizedAccess() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("customer2");

        Customer otherCustomer = new Customer();
        otherCustomer.setCustomerId(2L);
        otherCustomer.setUser(otherUser);

        property.setCustomer(otherCustomer);

        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.deleteProperty("customer1", 1L);
        });

        assertTrue(exception.getMessage().contains("Unauthorized"));
        verify(propertyRepository, never()).delete(any());
    }

    @Test
    void testGetAllProperties_Success() {
        Property property2 = new Property();
        property2.setPropertyId(2L);

        List<Property> properties = Arrays.asList(property, property2);

        when(propertyRepository.findAll()).thenReturn(properties);

        List<Property> result = service.getAllProperties();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(propertyRepository).findAll();
    }

    @Test
    void testGetPropertyById_Success() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.of(property));

        Property result = service.getPropertyById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPropertyId());
        verify(propertyRepository).findById(1L);
    }

    @Test
    void testGetPropertyById_NotFound() {
        when(propertyRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.getPropertyById(1L);
        });

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testRegisterProperty_Success() {
        when(propertyRepository.save(property)).thenReturn(property);

        Property result = service.registerProperty(property);

        assertNotNull(result);
        assertEquals(1L, result.getPropertyId());
        verify(propertyRepository).save(property);
    }

    @Test
    void testGetCustomerProperties_Success() {
        Property property2 = new Property();
        property2.setPropertyId(2L);
        property2.setCustomer(customer);

        List<Property> properties = Arrays.asList(property, property2);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(propertyRepository.findByCustomer(customer)).thenReturn(properties);

        List<Property> result = service.getCustomerProperties(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(customerRepository).findById(1L);
        verify(propertyRepository).findByCustomer(customer);
    }

    @Test
    void testGetCustomerProperties_CustomerNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.getCustomerProperties(1L);
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(propertyRepository, never()).findByCustomer(any());
    }

    @Test
    void testAddProperty_Success() {
        Property newProperty = new Property();
        newProperty.setPropertyType("Commercial");
        newProperty.setAddress("456 Oak Ave");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(propertyRepository.save(any(Property.class))).thenAnswer(invocation -> {
            Property saved = invocation.getArgument(0);
            saved.setPropertyId(2L);
            return saved;
        });

        Property result = service.addProperty(1L, newProperty);

        assertNotNull(result);
        assertEquals(customer, result.getCustomer());
        assertEquals(2L, result.getPropertyId());
        verify(customerRepository).findById(1L);
        verify(propertyRepository).save(any(Property.class));
    }

    @Test
    void testAddProperty_CustomerNotFound() {
        Property newProperty = new Property();

        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.addProperty(1L, newProperty);
        });

        assertTrue(exception.getMessage().contains("Customer not found"));
        verify(propertyRepository, never()).save(any());
    }
}
