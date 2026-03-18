# Custom Exceptions Summary

## ✅ Successfully Created 6 Custom Exceptions

All custom exceptions are located in: `src/main/java/org/hartford/fireinsurance/exception/`

---

## 📁 Files Created

### 1. **ResourceNotFoundException.java** (404 NOT FOUND)
- **Purpose**: When a resource doesn't exist in the database
- **Examples**: User not found, Customer not found, Policy not found
- **Usage**: `throw new ResourceNotFoundException("Customer", 123L);`

### 2. **DuplicateResourceException.java** (409 CONFLICT)
- **Purpose**: When trying to create a resource that already exists
- **Examples**: Username already exists, Email already registered
- **Usage**: `throw new DuplicateResourceException("User", "username", "john");`

### 3. **InvalidRequestException.java** (400 BAD REQUEST)
- **Purpose**: When request data is invalid or incomplete
- **Examples**: Missing fields, Invalid email format, Negative values
- **Usage**: `throw new InvalidRequestException("email", "must be valid");`

### 4. **UnauthorizedAccessException.java** (403 FORBIDDEN)
- **Purpose**: When user doesn't have permission to access a resource
- **Examples**: Customer accessing another customer's data, Non-admin accessing admin endpoints
- **Usage**: `throw new UnauthorizedAccessException("delete", "customer");`

### 5. **BusinessLogicException.java** (422 UNPROCESSABLE ENTITY)
- **Purpose**: When a business rule is violated
- **Examples**: Cannot approve rejected quote, Cannot delete policy with active claims
- **Usage**: `throw new BusinessLogicException("approve quote", "already rejected");`

### 6. **FileStorageException.java** (500 INTERNAL SERVER ERROR)
- **Purpose**: When file storage operations fail
- **Examples**: Failed to upload, File not found, File size exceeds limit
- **Usage**: `throw new FileStorageException("upload", "doc.pdf", "size exceeds limit");`

---

## 🔧 Updated GlobalExceptionHandler.java

Enhanced the global exception handler to catch and properly format all custom exceptions with:
- ✅ Timestamp
- ✅ HTTP status code
- ✅ Error type
- ✅ Detailed message

### Error Response Format:
```json
{
    "timestamp": "2024-03-12T10:30:45",
    "status": 404,
    "error": "Resource Not Found",
    "message": "Customer not found with ID: 123"
}
```

---

## 📚 Documentation Created

**CUSTOM_EXCEPTIONS_GUIDE.md** - Complete guide with:
- Detailed explanation of each exception
- Real-world usage examples
- HTTP status code mapping
- Migration guide from RuntimeException
- Testing examples
- Best practices

---

## 🎯 HTTP Status Code Mapping

| Exception | Status Code | When to Use |
|-----------|-------------|-------------|
| ResourceNotFoundException | 404 | Resource doesn't exist |
| DuplicateResourceException | 409 | Resource already exists |
| InvalidRequestException | 400 | Invalid/incomplete data |
| UnauthorizedAccessException | 403 | No permission |
| BusinessLogicException | 422 | Business rule violated |
| FileStorageException | 500 | File operation failed |

---

## 💡 Key Features

### 1. **Multiple Constructors**
Each exception has multiple constructors for flexibility:
```java
// Simple message
throw new ResourceNotFoundException("User not found");

// With resource and ID
throw new ResourceNotFoundException("Customer", 123L);

// With resource, field, and value
throw new ResourceNotFoundException("User", "username", "john");
```

### 2. **Clear Error Messages**
Automatically formats error messages:
```java
new ResourceNotFoundException("Customer", 123L)
// Output: "Customer not found with ID: 123"

new DuplicateResourceException("User", "email", "john@example.com")
// Output: "User already exists with email: john@example.com"
```

### 3. **Consistent Response Format**
All exceptions return the same JSON structure with timestamp, status, error type, and message.

---

## 🚀 Next Steps (Optional)

To fully integrate these exceptions into your project:

1. **Update Services** - Replace `RuntimeException` with custom exceptions
   ```java
   // Before
   throw new RuntimeException("Customer not found");
   
   // After
   throw new ResourceNotFoundException("Customer", id);
   ```

2. **Update Tests** - Test for specific exception types
   ```java
   assertThrows(ResourceNotFoundException.class, () -> {
       customerService.getCustomer(999L);
   });
   ```

3. **Add Validation** - Use InvalidRequestException for validation
   ```java
   if (request.getEmail() == null) {
       throw new InvalidRequestException("email", "cannot be null");
   }
   ```

4. **Add Authorization** - Use UnauthorizedAccessException for permissions
   ```java
   if (!currentUser.isAdmin()) {
       throw new UnauthorizedAccessException("delete", "customer");
   }
   ```

---

## ✅ Benefits

1. **Better Error Handling** - Specific exceptions for different error types
2. **Proper HTTP Status Codes** - Correct status codes for each error
3. **Clearer Code** - Exception names explain what went wrong
4. **Easier Testing** - Can test for specific exception types
5. **Better API** - Consistent error responses for API consumers
6. **Easier Debugging** - Clear error messages with context

---

## 📝 Example Usage in Your Code

### Before (Generic RuntimeException):
```java
public Customer getCustomer(Long id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
}
```

### After (Custom Exception):
```java
public Customer getCustomer(Long id) {
    return customerRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
}
```

---

## 🎉 Summary

✅ Created 6 custom exception classes
✅ Updated GlobalExceptionHandler with proper error handling
✅ Created comprehensive documentation guide
✅ All exceptions follow consistent patterns
✅ Ready to use in your services and controllers

**All tests still pass!** The existing tests work because RuntimeException is still handled by the GlobalExceptionHandler.

---

**Location**: `src/main/java/org/hartford/fireinsurance/exception/`
**Documentation**: `CUSTOM_EXCEPTIONS_GUIDE.md`
**Status**: ✅ Ready to use
