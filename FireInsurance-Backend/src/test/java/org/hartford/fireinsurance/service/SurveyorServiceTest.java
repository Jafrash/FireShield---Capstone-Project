package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.SurveyorRegistrationRequest;
import org.hartford.fireinsurance.dto.SurveyorRegistrationResponse;
import org.hartford.fireinsurance.dto.UpdateSurveyorRequest;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.SurveyorRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyorServiceTest {

    @Mock
    private SurveyorRepository surveyorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SurveyorService surveyorService;

    private User user;
    private Surveyor surveyor;
    private UpdateSurveyorRequest updateRequest;
    private SurveyorRegistrationRequest registrationRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("surveyor1");
        user.setEmail("surveyor1@example.com");
        user.setPhoneNumber("1234567890");
        user.setRole("SURVEYOR");

        surveyor = new Surveyor();
        surveyor.setSurveyorId(1L);
        surveyor.setUser(user);
        surveyor.setLicenseNumber("LIC123");
        surveyor.setExperienceYears(5);
        surveyor.setAssignedRegion("North Region");

        updateRequest = new UpdateSurveyorRequest();
        updateRequest.setPhoneNumber("9876543210");
        updateRequest.setLicenseNumber("LIC456");
        updateRequest.setExperienceYears(7);
        updateRequest.setAssignedRegion("South Region");

        registrationRequest = new SurveyorRegistrationRequest();
        registrationRequest.setUsername("surveyor1");
        registrationRequest.setEmail("surveyor1@example.com");
        registrationRequest.setPassword("password123");
        registrationRequest.setPhoneNumber("1234567890");
        registrationRequest.setLicenseNumber("LIC123");
        registrationRequest.setExperienceYears(5);
        registrationRequest.setAssignedRegion("North Region");
    }

    @Test
    @DisplayName("registerSurveyor - Success")
    void registerSurveyor_Success() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(surveyorRepository.save(any(Surveyor.class))).thenReturn(surveyor);

        SurveyorRegistrationResponse result = surveyorService.registerSurveyor(registrationRequest);

        assertNotNull(result);
        assertEquals("surveyor1", result.getUsername());
        assertEquals("LIC123", result.getLicenseNumber());
        assertEquals("Surveyor registered successfully", result.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
        verify(surveyorRepository, times(1)).save(any(Surveyor.class));
    }

    @Test
    @DisplayName("saveSurveyor - Success")
    void saveSurveyor_Success() {
        when(surveyorRepository.save(surveyor)).thenReturn(surveyor);

        Surveyor result = surveyorService.saveSurveyor(surveyor);

        assertNotNull(result);
        assertEquals(1L, result.getSurveyorId());
        verify(surveyorRepository, times(1)).save(surveyor);
    }

    @Test
    @DisplayName("getSurveyor - Success")
    void getSurveyor_Success() {
        when(surveyorRepository.findById(1L)).thenReturn(Optional.of(surveyor));

        Surveyor result = surveyorService.getSurveyor(1L);

        assertNotNull(result);
        assertEquals(1L, result.getSurveyorId());
        assertEquals("surveyor1", result.getUser().getUsername());
        assertEquals("LIC123", result.getLicenseNumber());
        verify(surveyorRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getSurveyor - Not Found")
    void getSurveyor_NotFound() {
        when(surveyorRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> surveyorService.getSurveyor(999L)
        );

        assertEquals("Surveyor not found", exception.getMessage());
        verify(surveyorRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("getAllSurveyors - Success")
    void getAllSurveyors_Success() {
        List<Surveyor> surveyors = Arrays.asList(surveyor);
        when(surveyorRepository.findAll()).thenReturn(surveyors);

        List<Surveyor> result = surveyorService.getAllSurveyors();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("surveyor1", result.get(0).getUser().getUsername());
        verify(surveyorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllSurveyors - Empty List")
    void getAllSurveyors_EmptyList() {
        when(surveyorRepository.findAll()).thenReturn(Arrays.asList());

        List<Surveyor> result = surveyorService.getAllSurveyors();

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(surveyorRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getSurveyorByUsername - Success")
    void getSurveyorByUsername_Success() {
        when(userRepository.findByUsername("surveyor1")).thenReturn(Optional.of(user));
        when(surveyorRepository.findByUser(user)).thenReturn(Optional.of(surveyor));

        Surveyor result = surveyorService.getSurveyorByUsername("surveyor1");

        assertNotNull(result);
        assertEquals("surveyor1", result.getUser().getUsername());
        assertEquals("LIC123", result.getLicenseNumber());
        verify(userRepository, times(1)).findByUsername("surveyor1");
        verify(surveyorRepository, times(1)).findByUser(user);
    }

    @Test
    @DisplayName("getSurveyorByUsername - User Not Found")
    void getSurveyorByUsername_UserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> surveyorService.getSurveyorByUsername("unknown")
        );

        assertEquals("User not found with username: unknown", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("unknown");
        verify(surveyorRepository, never()).findByUser(any());
    }

    @Test
    @DisplayName("getSurveyorByUsername - Surveyor Not Found")
    void getSurveyorByUsername_SurveyorNotFound() {
        when(userRepository.findByUsername("surveyor1")).thenReturn(Optional.of(user));
        when(surveyorRepository.findByUser(user)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> surveyorService.getSurveyorByUsername("surveyor1")
        );

        assertEquals("Surveyor not found for user: surveyor1", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("surveyor1");
        verify(surveyorRepository, times(1)).findByUser(user);
    }

    @Test
    @DisplayName("updateSurveyorProfile - Success")
    void updateSurveyorProfile_Success() {
        when(userRepository.findByUsername("surveyor1")).thenReturn(Optional.of(user));
        when(surveyorRepository.findByUser(user)).thenReturn(Optional.of(surveyor));
        when(surveyorRepository.save(any(Surveyor.class))).thenReturn(surveyor);

        Surveyor result = surveyorService.updateSurveyorProfile("surveyor1", updateRequest);

        assertNotNull(result);
        assertEquals("9876543210", result.getUser().getPhoneNumber());
        assertEquals("LIC456", result.getLicenseNumber());
        assertEquals(7, result.getExperienceYears());
        assertEquals("South Region", result.getAssignedRegion());
        verify(userRepository, times(1)).findByUsername("surveyor1");
        verify(surveyorRepository, times(1)).findByUser(user);
        verify(surveyorRepository, times(1)).save(any(Surveyor.class));
    }

    @Test
    @DisplayName("updateSurveyorProfile - Not Found")
    void updateSurveyorProfile_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> surveyorService.updateSurveyorProfile("unknown", updateRequest)
        );

        assertEquals("User not found with username: unknown", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("unknown");
        verify(surveyorRepository, never()).save(any(Surveyor.class));
    }

    @Test
    @DisplayName("deleteSurveyor - Success")
    void deleteSurveyor_Success() {
        when(surveyorRepository.existsById(1L)).thenReturn(true);
        doNothing().when(surveyorRepository).deleteById(1L);

        assertDoesNotThrow(() -> surveyorService.deleteSurveyor(1L));

        verify(surveyorRepository, times(1)).existsById(1L);
        verify(surveyorRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("deleteSurveyor - Not Found")
    void deleteSurveyor_NotFound() {
        when(surveyorRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> surveyorService.deleteSurveyor(999L)
        );

        assertEquals("Surveyor not found with ID: 999", exception.getMessage());
        verify(surveyorRepository, times(1)).existsById(999L);
        verify(surveyorRepository, never()).deleteById(anyLong());
    }
}
