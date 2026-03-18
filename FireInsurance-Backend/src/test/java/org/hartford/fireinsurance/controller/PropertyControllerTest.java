package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.CreatePropertyRequest;
import org.hartford.fireinsurance.dto.PropertyResponse;
import org.hartford.fireinsurance.dto.UpdatePropertyRequest;
import org.hartford.fireinsurance.model.Customer;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.service.PropertyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PropertyControllerTest {

    @Mock
    private PropertyService propertyService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PropertyController controller;

    private Property property;
    private Customer customer;
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
    void testAddProperty_Success() {
        CreatePropertyRequest request = new CreatePropertyRequest();
        request.setPropertyType("Residential");
        request.setAddress("123 Main St");
        request.setAreaSqft(2000.0);
        request.setConstructionType("Concrete");

        when(authentication.getName()).thenReturn("customer1");
        when(propertyService.createProperty(eq("customer1"), any(CreatePropertyRequest.class))).thenReturn(property);

        ResponseEntity<PropertyResponse> response = controller.addProperty(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getPropertyId());
        assertEquals("Residential", response.getBody().getPropertyType());
        assertEquals("123 Main St", response.getBody().getAddress());
        assertEquals(2000.0, response.getBody().getAreaSqft());
        assertEquals("Concrete", response.getBody().getConstructionType());
        assertEquals(0.0, response.getBody().getRiskScore());
        verify(propertyService).createProperty(eq("customer1"), any(CreatePropertyRequest.class));
    }

    @Test
    void testGetMyProperties_Success() {
        Property property2 = new Property();
        property2.setPropertyId(2L);
        property2.setCustomer(customer);
        property2.setPropertyType("Commercial");
        property2.setAddress("456 Oak Ave");
        property2.setAreaSqft(3000.0);
        property2.setConstructionType("Steel");
        property2.setRiskScore(5.0);

        List<Property> properties = Arrays.asList(property, property2);

        when(authentication.getName()).thenReturn("customer1");
        when(propertyService.getPropertiesByUsername("customer1")).thenReturn(properties);

        ResponseEntity<List<PropertyResponse>> response = controller.getMyProperties(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(1L, response.getBody().get(0).getPropertyId());
        assertEquals(2L, response.getBody().get(1).getPropertyId());
        verify(propertyService).getPropertiesByUsername("customer1");
    }

    @Test
    void testGetMyProperties_EmptyList() {
        when(authentication.getName()).thenReturn("customer1");
        when(propertyService.getPropertiesByUsername("customer1")).thenReturn(Arrays.asList());

        ResponseEntity<List<PropertyResponse>> response = controller.getMyProperties(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());
        verify(propertyService).getPropertiesByUsername("customer1");
    }

    @Test
    void testUpdateProperty_Success() {
        UpdatePropertyRequest request = new UpdatePropertyRequest();
        request.setPropertyType("Commercial");
        request.setAddress("789 Pine Rd");
        request.setAreaSqft(2500.0);
        request.setConstructionType("Brick");
        request.setRiskScore(3.5);

        Property updatedProperty = new Property();
        updatedProperty.setPropertyId(1L);
        updatedProperty.setCustomer(customer);
        updatedProperty.setPropertyType("Commercial");
        updatedProperty.setAddress("789 Pine Rd");
        updatedProperty.setAreaSqft(2500.0);
        updatedProperty.setConstructionType("Brick");
        updatedProperty.setRiskScore(3.5);

        when(authentication.getName()).thenReturn("customer1");
        when(propertyService.updateProperty(eq("customer1"), eq(1L), any(UpdatePropertyRequest.class)))
                .thenReturn(updatedProperty);

        ResponseEntity<PropertyResponse> response = controller.updateProperty(authentication, 1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getPropertyId());
        assertEquals("Commercial", response.getBody().getPropertyType());
        assertEquals("789 Pine Rd", response.getBody().getAddress());
        assertEquals(2500.0, response.getBody().getAreaSqft());
        assertEquals("Brick", response.getBody().getConstructionType());
        assertEquals(3.5, response.getBody().getRiskScore());
        verify(propertyService).updateProperty(eq("customer1"), eq(1L), any(UpdatePropertyRequest.class));
    }

    @Test
    void testUpdateProperty_PartialUpdate() {
        UpdatePropertyRequest request = new UpdatePropertyRequest();
        request.setAddress("New Address");

        Property updatedProperty = new Property();
        updatedProperty.setPropertyId(1L);
        updatedProperty.setCustomer(customer);
        updatedProperty.setPropertyType("Residential");
        updatedProperty.setAddress("New Address");
        updatedProperty.setAreaSqft(2000.0);
        updatedProperty.setConstructionType("Concrete");
        updatedProperty.setRiskScore(0.0);

        when(authentication.getName()).thenReturn("customer1");
        when(propertyService.updateProperty(eq("customer1"), eq(1L), any(UpdatePropertyRequest.class)))
                .thenReturn(updatedProperty);

        ResponseEntity<PropertyResponse> response = controller.updateProperty(authentication, 1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("New Address", response.getBody().getAddress());
        verify(propertyService).updateProperty(eq("customer1"), eq(1L), any(UpdatePropertyRequest.class));
    }

    @Test
    void testDeleteProperty_Success() {
        when(authentication.getName()).thenReturn("customer1");
        doNothing().when(propertyService).deleteProperty("customer1", 1L);

        ResponseEntity<String> response = controller.deleteProperty(authentication, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Property deleted successfully", response.getBody());
        verify(propertyService).deleteProperty("customer1", 1L);
    }

    @Test
    void testGetAllProperties_Success() {
        Property property2 = new Property();
        property2.setPropertyId(2L);
        property2.setCustomer(customer);
        property2.setPropertyType("Industrial");
        property2.setAddress("999 Factory Ln");
        property2.setAreaSqft(5000.0);
        property2.setConstructionType("Metal");
        property2.setRiskScore(7.0);

        List<Property> properties = Arrays.asList(property, property2);

        when(propertyService.getAllProperties()).thenReturn(properties);

        ResponseEntity<List<PropertyResponse>> response = controller.getAllProperties();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(propertyService).getAllProperties();
    }

    @Test
    void testGetPropertyById_Success() {
        when(propertyService.getPropertyById(1L)).thenReturn(property);

        ResponseEntity<PropertyResponse> response = controller.getPropertyById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getPropertyId());
        assertEquals("Residential", response.getBody().getPropertyType());
        verify(propertyService).getPropertyById(1L);
    }

    @Test
    void testMapToResponse_AllFields() {
        property.setPropertyId(1L);
        property.setPropertyType("Residential");
        property.setAddress("123 Main St");
        property.setAreaSqft(2000.0);
        property.setConstructionType("Concrete");
        property.setRiskScore(4.5);

        when(propertyService.getPropertyById(1L)).thenReturn(property);

        ResponseEntity<PropertyResponse> response = controller.getPropertyById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PropertyResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(1L, body.getPropertyId());
        assertEquals("Residential", body.getPropertyType());
        assertEquals("123 Main St", body.getAddress());
        assertEquals(2000.0, body.getAreaSqft());
        assertEquals("Concrete", body.getConstructionType());
        assertEquals(4.5, body.getRiskScore());
    }

    @Test
    void testMapToResponse_WithNullRiskScore() {
        property.setRiskScore(null);

        when(propertyService.getPropertyById(1L)).thenReturn(property);

        ResponseEntity<PropertyResponse> response = controller.getPropertyById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        PropertyResponse body = response.getBody();
        assertNotNull(body);
        assertNull(body.getRiskScore());
    }

    @Test
    void testAddProperty_WithAllFields() {
        CreatePropertyRequest request = new CreatePropertyRequest(
                "Commercial",
                "456 Business Blvd",
                3500.0,
                "Steel"
        );

        Property newProperty = new Property();
        newProperty.setPropertyId(2L);
        newProperty.setCustomer(customer);
        newProperty.setPropertyType("Commercial");
        newProperty.setAddress("456 Business Blvd");
        newProperty.setAreaSqft(3500.0);
        newProperty.setConstructionType("Steel");
        newProperty.setRiskScore(0.0);

        when(authentication.getName()).thenReturn("customer1");
        when(propertyService.createProperty(eq("customer1"), any(CreatePropertyRequest.class)))
                .thenReturn(newProperty);

        ResponseEntity<PropertyResponse> response = controller.addProperty(authentication, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2L, response.getBody().getPropertyId());
        assertEquals("Commercial", response.getBody().getPropertyType());
        assertEquals("456 Business Blvd", response.getBody().getAddress());
        assertEquals(3500.0, response.getBody().getAreaSqft());
        assertEquals("Steel", response.getBody().getConstructionType());
    }

    @Test
    void testUpdateProperty_WithAllFields() {
        UpdatePropertyRequest request = new UpdatePropertyRequest(
                "Industrial",
                "789 Factory St",
                4000.0,
                "Metal",
                8.5
        );

        Property updatedProperty = new Property();
        updatedProperty.setPropertyId(1L);
        updatedProperty.setCustomer(customer);
        updatedProperty.setPropertyType("Industrial");
        updatedProperty.setAddress("789 Factory St");
        updatedProperty.setAreaSqft(4000.0);
        updatedProperty.setConstructionType("Metal");
        updatedProperty.setRiskScore(8.5);

        when(authentication.getName()).thenReturn("customer1");
        when(propertyService.updateProperty(eq("customer1"), eq(1L), any(UpdatePropertyRequest.class)))
                .thenReturn(updatedProperty);

        ResponseEntity<PropertyResponse> response = controller.updateProperty(authentication, 1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Industrial", response.getBody().getPropertyType());
        assertEquals("789 Factory St", response.getBody().getAddress());
        assertEquals(4000.0, response.getBody().getAreaSqft());
        assertEquals("Metal", response.getBody().getConstructionType());
        assertEquals(8.5, response.getBody().getRiskScore());
    }
}
