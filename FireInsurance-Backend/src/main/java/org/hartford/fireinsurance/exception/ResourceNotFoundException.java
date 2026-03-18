package org.hartford.fireinsurance.exception;

/**
 * Exception thrown when a requested resource is not found in the database.
 * This will result in a 404 NOT FOUND HTTP response.
 * 
 * Examples:
 * - User not found
 * - Customer not found
 * - Policy not found
 * - Quote not found
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with ID: %d", resourceName, id));
    }
    
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: %s", resourceName, fieldName, fieldValue));
    }
}
