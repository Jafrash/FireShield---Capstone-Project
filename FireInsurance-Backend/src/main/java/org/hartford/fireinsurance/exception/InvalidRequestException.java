package org.hartford.fireinsurance.exception;

/**
 * Exception thrown when the request data is invalid or incomplete.
 * This will result in a 400 BAD REQUEST HTTP response.
 * 
 * Examples:
 * - Missing required fields
 * - Invalid email format
 * - Invalid phone number
 * - Invalid date range
 * - Negative values where positive expected
 */
public class InvalidRequestException extends RuntimeException {
    
    public InvalidRequestException(String message) {
        super(message);
    }
    
    public InvalidRequestException(String fieldName, String reason) {
        super(String.format("Invalid %s: %s", fieldName, reason));
    }
}
