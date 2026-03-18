package org.hartford.fireinsurance.exception;

/**
 * Exception thrown when a business rule is violated.
 * This will result in a 422 UNPROCESSABLE ENTITY HTTP response.
 * 
 * Examples:
 * - Cannot approve a quote that's already rejected
 * - Cannot delete a policy with active claims
 * - Cannot assign a surveyor who is already assigned to maximum cases
 * - Cannot create a quote for an expired policy
 * - Age restrictions not met
 */
public class BusinessLogicException extends RuntimeException {
    
    public BusinessLogicException(String message) {
        super(message);
    }
    
    public BusinessLogicException(String operation, String reason) {
        super(String.format("Cannot %s: %s", operation, reason));
    }
}
