package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * Tests authentication and user registration logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setPhoneNumber("1234567890");
        testUser.setRole("CUSTOMER");
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
    }

    // ========== register() Tests ==========

    @Test
    @DisplayName("Test 1: Should register user successfully")
    void testRegisterSuccess() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = authService.register(testUser);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals("testuser", result.getUsername(), "Username should match");
        assertEquals("test@example.com", result.getEmail(), "Email should match");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    @DisplayName("Test 2: Should save user with all fields")
    void testRegisterWithAllFields() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = authService.register(testUser);

        // Assert
        assertNotNull(result);
        assertEquals("CUSTOMER", result.getRole());
        assertTrue(result.getActive());
        assertNotNull(result.getCreatedAt());
        verify(userRepository).save(testUser);
    }

    // ========== login() Tests ==========

    @Test
    @DisplayName("Test 3: Should login successfully with correct credentials")
    void testLoginSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = authService.login("testuser", "password123");

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals("testuser", result.getUsername(), "Username should match");
        assertEquals("password123", result.getPassword(), "Password should match");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Test 4: Should throw exception when user not found")
    void testLoginUserNotFound() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login("nonexistent", "password123");
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Test 5: Should throw exception with wrong password")
    void testLoginWrongPassword() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login("testuser", "wrongpassword");
        });

        assertEquals("Invalid password", exception.getMessage());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Test 6: Should handle null username in login")
    void testLoginNullUsername() {
        // Arrange
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authService.login(null, "password123");
        });

        verify(userRepository).findByUsername(null);
    }

    @Test
    @DisplayName("Test 7: Should handle null password in login")
    void testLoginNullPassword() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login("testuser", null);
        });

        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    @DisplayName("Test 8: Should handle empty username")
    void testLoginEmptyUsername() {
        // Arrange
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            authService.login("", "password123");
        });
    }

    @Test
    @DisplayName("Test 9: Should verify repository interaction")
    void testRegisterRepositoryInteraction() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        authService.register(testUser);

        // Assert
        verify(userRepository, times(1)).save(testUser);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Test 10: Should handle user with different roles")
    void testRegisterDifferentRoles() {
        // Arrange
        testUser.setRole("ADMIN");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = authService.register(testUser);

        // Assert
        assertEquals("ADMIN", result.getRole());
        verify(userRepository).save(testUser);
    }
}

