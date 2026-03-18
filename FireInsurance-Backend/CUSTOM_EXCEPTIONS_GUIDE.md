# Custom Exceptions Guide

## Overview
This document explains all custom exceptions in the FireInsurance backend and how to use them.

---

## 1. ResourceNotFoundException (404 NOT FOUND)

**When to use**: When a requested resource doesn't exist in the database.

**HTTP Status**: 404 NOT FOUND

### Usage Examples:

```java
// Simple message
throw new ResourceNotFoundException("User not found");

// With resource name and ID
throw new ResourceNotFoundException("Customer", 123L);
// Output: "Customer not found with ID: 123"

// With resource name, field name, and value
throw new ResourceNotFoundException("User", "username", "john_doe");
// Output: "User not found with username: john_doe"
```

### Real-world examples:
```java
// In CustomerService
public Customer getCustomer(Long id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
}

// In UserService
public User getUserByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
}
```

---

## 2. DuplicateResourceException (409 CONFLICT)

**When to use**: When trying to create a resource that already exists.

**HTTP Status**: 409 CONFLICT

### Usage Examples:

```java
// Simple message
throw new DuplicateResourceException("Username already exists");

// With resource name, field name, and value
throw new DuplicateResourceException("User", "username", "john_doe");
// Output: "User already exists with username: john_doe"
```

### Real-world examples:
```java
// In CustomerService
public void registerCustomer(CustomerRegistrationRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new DuplicateResourceException("User", "username", request.getUsername());
    }
    
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new DuplicateResourceException("User", "email", request.getEmail());
    }
    
    // Continue with registration...
}

// In SurveyorService
public void registerSurveyor(SurveyorRegistrationRequest request) {
    if (surveyorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
        throw new DuplicateResourceException("Surveyor", "license number", request.getLicenseNumber());
    }
    
    // Continue with registration...
}
```

---

## 3. InvalidRequestException (400 BAD REQUEST)

**When to use**: When request data is invalid or incomplete.

**HTTP Status**: 400 BAD REQUEST

### Usage Examples:

```java
// Simple message
throw new InvalidRequestException("Missing required fields");

// With field name and reason
throw new InvalidRequestException("email", "must be a valid email address");
// Output: "Invalid email: must be a valid email address"
```

### Real-world examples:
```java
// In CustomerService
public void registerCustomer(CustomerRegistrationRequest request) {
    if (request.getUsername() == null || request.getUsername().isEmpty()) {
        throw new InvalidRequestException("username", "cannot be empty");
    }
    
    if (request.getPassword() == null || request.getPassword().length() < 8) {
        throw new InvalidRequestException("password", "must be at least 8 characters");
    }
    
    if (!isValidEmail(request.getEmail())) {
        throw new InvalidRequestException("email", "must be a valid email address");
    }
}

// In QuoteService
public void createQuote(QuoteRequest request) {
    if (request.getPropertyValue() <= 0) {
        throw new InvalidRequestException("property value", "must be greater than zero");
    }
    
    if (request.getStartDate().isAfter(request.getEndDate())) {
        throw new InvalidRequestException("date range", "start date must be before end date");
    }
}
```

---

## 4. UnauthorizedAccessException (403 FORBIDDEN)

**When to use**: When a user tries to access a resource they don't have permission for.

**HTTP Status**: 403 FORBIDDEN

### Usage Examples:

```java
// Simple message
throw new UnauthorizedAccessException("You cannot access this resource");

// With action and resource
throw new UnauthorizedAccessException("delete", "customer");
// Output: "You are not authorized to delete this customer"
```

### Real-world examples:
```java
// In CustomerService
public Customer getCustomer(Long id, String currentUsername) {
    Customer customer = customerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    
    // Check if user is trying to access another customer's data
    if (!customer.getUser().getUsername().equals(currentUsername)) {
        throw new UnauthorizedAccessException("access", "customer profile");
    }
    
    return customer;
}

// In PolicyService
public void deletePolicy(Long policyId, String currentUsername, String userRole) {
    if (!"ADMIN".equals(userRole)) {
        throw new UnauthorizedAccessException("delete", "policy");
    }
    
    // Continue with deletion...
}
```

---

## 5. BusinessLogicException (422 UNPROCESSABLE ENTITY)

**When to use**: When a business rule is violated.

**HTTP Status**: 422 UNPROCESSABLE ENTITY

### Usage Examples:

```java
// Simple message
throw new BusinessLogicException("Cannot approve an already rejected quote");

// With operation and reason
throw new BusinessLogicException("approve quote", "quote has already been rejected");
// Output: "Cannot approve quote: quote has already been rejected"
```

### Real-world examples:
```java
// In QuoteService
public void approveQuote(Long quoteId) {
    Quote quote = quoteRepository.findById(quoteId)
        .orElseThrow(() -> new ResourceNotFoundException("Quote", quoteId));
    
    if ("REJECTED".equals(quote.getStatus())) {
        throw new BusinessLogicException("approve quote", "quote has already been rejected");
    }
    
    if ("APPROVED".equals(quote.getStatus())) {
        throw new BusinessLogicException("approve quote", "quote is already approved");
    }
    
    // Continue with approval...
}

// In PolicyService
public void deletePolicy(Long policyId) {
    Policy policy = policyRepository.findById(policyId)
        .orElseThrow(() -> new ResourceNotFoundException("Policy", policyId));
    
    if (policy.hasActiveClaims()) {
        throw new BusinessLogicException("delete policy", "policy has active claims");
    }
    
    // Continue with deletion...
}

// In SurveyorService
public void assignSurveyor(Long surveyorId, Long claimId) {
    Surveyor surveyor = surveyorRepository.findById(surveyorId)
        .orElseThrow(() -> new ResourceNotFoundException("Surveyor", surveyorId));
    
    if (surveyor.getAssignedCases().size() >= surveyor.getMaxCases()) {
        throw new BusinessLogicException("assign surveyor", "surveyor has reached maximum case limit");
    }
    
    // Continue with assignment...
}
```

---

## 6. FileStorageException (500 INTERNAL SERVER ERROR)

**When to use**: When file storage operations fail.

**HTTP Status**: 500 INTERNAL SERVER ERROR

### Usage Examples:

```java
// Simple message
throw new FileStorageException("Failed to upload file");

// With message and cause
throw new FileStorageException("Failed to upload file", ioException);

// With operation, filename, and reason
throw new FileStorageException("upload", "document.pdf", "file size exceeds limit");
// Output: "Failed to upload file 'document.pdf': file size exceeds limit"
```

### Real-world examples:
```java
// In DocumentService
public String uploadDocument(MultipartFile file) {
    try {
        if (file.isEmpty()) {
            throw new FileStorageException("upload", file.getOriginalFilename(), "file is empty");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException("upload", file.getOriginalFilename(), "file size exceeds 10MB limit");
        }
        
        String fileName = storeFile(file);
        return fileName;
        
    } catch (IOException ex) {
        throw new FileStorageException("Failed to upload file: " + file.getOriginalFilename(), ex);
    }
}

// In DocumentService
public byte[] downloadDocument(String fileName) {
    try {
        Path filePath = fileStorageLocation.resolve(fileName).normalize();
        
        if (!Files.exists(filePath)) {
            throw new FileStorageException("download", fileName, "file not found on server");
        }
        
        return Files.readAllBytes(filePath);
        
    } catch (IOException ex) {
        throw new FileStorageException("Failed to download file: " + fileName, ex);
    }
}
```

---

## Error Response Format

All exceptions return a consistent JSON error response:

```json
{
    "timestamp": "2024-03-12T10:30:45",
    "status": 404,
    "error": "Resource Not Found",
    "message": "Customer not found with ID: 123"
}
```

---

## HTTP Status Code Summary

| Exception | HTTP Status | Code | Use Case |
|-----------|-------------|------|----------|
| ResourceNotFoundException | NOT FOUND | 404 | Resource doesn't exist |
| DuplicateResourceException | CONFLICT | 409 | Resource already exists |
| InvalidRequestException | BAD REQUEST | 400 | Invalid/incomplete data |
| UnauthorizedAccessException | FORBIDDEN | 403 | No permission |
| BusinessLogicException | UNPROCESSABLE ENTITY | 422 | Business rule violated |
| FileStorageException | INTERNAL SERVER ERROR | 500 | File operation failed |
| BadCredentialsException | UNAUTHORIZED | 401 | Wrong credentials |

---

## Migration Guide

### Before (using RuntimeException):
```java
throw new RuntimeException("Customer not found with ID: " + id);
throw new RuntimeException("Username already exists");
throw new RuntimeException("Missing required fields");
```

### After (using custom exceptions):
```java
throw new ResourceNotFoundException("Customer", id);
throw new DuplicateResourceException("User", "username", username);
throw new InvalidRequestException("Missing required fields");
```

---

## Benefits

1. ✅ **Clear Intent** - Exception name tells you what went wrong
2. ✅ **Proper HTTP Status** - Correct status codes for different errors
3. ✅ **Better Testing** - Can test for specific exception types
4. ✅ **Consistent Errors** - All errors follow the same format
5. ✅ **Easy Debugging** - Clear error messages with context
6. ✅ **API Documentation** - Clear error responses for API consumers

---

## Best Practices

1. **Always provide context** - Include resource name, ID, or field name
2. **Use specific exceptions** - Don't use generic RuntimeException
3. **Don't expose sensitive data** - Don't include passwords or tokens in error messages
4. **Keep messages user-friendly** - Write messages that users can understand
5. **Log exceptions** - Use logging framework to track errors
6. **Handle in controllers** - Let GlobalExceptionHandler catch and format errors

---

## Testing Custom Exceptions

```java
@Test
void testCustomerNotFound() {
    // Arrange
    when(customerRepository.findById(999L)).thenReturn(Optional.empty());
    
    // Act & Assert
    assertThrows(ResourceNotFoundException.class, () -> {
        customerService.getCustomer(999L);
    });
}

@Test
void testDuplicateUsername() {
    // Arrange
    when(userRepository.existsByUsername("john")).thenReturn(true);
    
    // Act & Assert
    DuplicateResourceException exception = assertThrows(
        DuplicateResourceException.class, 
        () -> customerService.registerCustomer(request)
    );
    
    assertTrue(exception.getMessage().contains("username"));
}
```

---

## Next Steps

To fully implement these exceptions in your project:

1. ✅ Replace all `throw new RuntimeException(...)` with appropriate custom exceptions
2. ✅ Update service classes to use custom exceptions
3. ✅ Update test cases to expect custom exceptions
4. ✅ Add validation logic that throws InvalidRequestException
5. ✅ Add authorization checks that throw UnauthorizedAccessException
6. ✅ Add business rule validations that throw BusinessLogicException

---

**Created by**: Amazon Q Developer
**Date**: March 12, 2024
**Version**: 1.0
