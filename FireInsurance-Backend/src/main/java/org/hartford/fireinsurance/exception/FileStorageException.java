package org.hartford.fireinsurance.exception;

/**
 * Exception thrown when file storage operations fail.
 * This will result in a 500 INTERNAL SERVER ERROR HTTP response.
 * 
 * Examples:
 * - Failed to upload document
 * - Failed to download document
 * - File not found on server
 * - Invalid file format
 * - File size exceeds limit
 * - Disk space full
 */
public class FileStorageException extends RuntimeException {
    
    public FileStorageException(String message) {
        super(message);
    }
    
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public FileStorageException(String operation, String fileName, String reason) {
        super(String.format("Failed to %s file '%s': %s", operation, fileName, reason));
    }
}
