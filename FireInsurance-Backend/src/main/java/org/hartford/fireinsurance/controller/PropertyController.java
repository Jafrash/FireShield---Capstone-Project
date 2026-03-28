package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.CreatePropertyRequest;
import org.hartford.fireinsurance.dto.PropertyResponse;
import org.hartford.fireinsurance.dto.UpdatePropertyRequest;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.service.PropertyService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;



@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // =============================
    // CUSTOMER - Add property
    // =============================
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PropertyResponse> addProperty(
            Authentication authentication,
            @RequestBody CreatePropertyRequest request) {

        String username = authentication.getName();

        Property property = propertyService.createProperty(username, request);

        return ResponseEntity.ok(mapToResponse(property));
    }

    // =============================
    // CUSTOMER - Get my properties
    // =============================
    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<PropertyResponse>> getMyProperties(
            Authentication authentication) {

        String username = authentication.getName();

        List<PropertyResponse> response =
                propertyService.getPropertiesByUsername(username)
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =============================
    // CUSTOMER - Update property
    // =============================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PropertyResponse> updateProperty(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody UpdatePropertyRequest request) {

        String username = authentication.getName();

        Property updated = propertyService.updateProperty(username, id, request);

        return ResponseEntity.ok(mapToResponse(updated));
    }

    // =============================
    // CUSTOMER - Delete property
    // =============================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> deleteProperty(
            Authentication authentication,
            @PathVariable Long id) {

        String username = authentication.getName();

        propertyService.deleteProperty(username, id);
        return ResponseEntity.ok("Property deleted successfully");
    }

    // =============================
    // ADMIN - Get all properties
    // =============================
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PropertyResponse>> getAllProperties() {

        List<PropertyResponse> response =
                propertyService.getAllProperties()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =============================
    // ADMIN - Get property by ID
    // =============================
    @GetMapping("/{id}/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PropertyResponse> getPropertyById(
            @PathVariable Long id) {

        Property property = propertyService.getPropertyById(id);
        return ResponseEntity.ok(mapToResponse(property));
    }

    // =============================
    // Mapper
    // =============================
    private PropertyResponse mapToResponse(Property property) {
        PropertyResponse response = new PropertyResponse();
        response.setPropertyId(property.getPropertyId());
        response.setPropertyType(property.getPropertyType());
        response.setAddress(property.getAddress());
        response.setAreaSqft(property.getAreaSqft());
        response.setConstructionType(property.getConstructionType());
        response.setRiskScore(property.getRiskScore());
        response.setLatitude(property.getLatitude());
        response.setLongitude(property.getLongitude());
        response.setZipCode(property.getZipCode());
        return response;
    }
}
