package org.hartford.fireinsurance.exception;

/**
 * Exception thrown when a user tries to access a resource they don't have permission for.
 * This will result in a 403 FORBIDDEN HTTP response.
 * 
 * Examples:
 * - Customer trying to access another customer's data
 * - Non-admin trying to access admin-only endpoints
 * - Surveyor trying to modify customer information
 * - Accessing resources outside user's scope
 */
public class UnauthorizedAccessException extends RuntimeException {
    
    public UnauthorizedAccessException(String message) {
        super(message);
    }
    
    public UnauthorizedAccessException(String action, String resource) {
        super(String.format("You are not authorized to %s this %s", action, resource));
    }
}
