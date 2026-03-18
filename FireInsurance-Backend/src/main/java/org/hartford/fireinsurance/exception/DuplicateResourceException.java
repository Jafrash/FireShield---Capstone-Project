package org.hartford.fireinsurance.exception;

/**
 * Exception thrown when trying to create a resource that already exists.
 * This will result in a 409 CONFLICT HTTP response.
 * 
 * Examples:
 * - Username already exists
 * - Email already registered
 * - License number already exists
 * - Policy number already exists
 */
public class DuplicateResourceException extends RuntimeException {
    
    public DuplicateResourceException(String message) {
        super(message);
    }
    
    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s already exists with %s: %s", resourceName, fieldName, fieldValue));
    }
}
