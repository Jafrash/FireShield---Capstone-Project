package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.SurveyorRegistrationRequest;
import org.hartford.fireinsurance.dto.SurveyorRegistrationResponse;
import org.hartford.fireinsurance.dto.UpdateSurveyorRequest;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.SurveyorRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SurveyorService {

    private final SurveyorRepository surveyorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SurveyorService(SurveyorRepository surveyorRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.surveyorRepository = surveyorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public SurveyorRegistrationResponse registerSurveyor(SurveyorRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (surveyorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new RuntimeException("License number already exists");
        }

        // Create User entity
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole("SURVEYOR");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Create Surveyor entity
        Surveyor surveyor = new Surveyor();
        surveyor.setUser(user);
        surveyor.setLicenseNumber(request.getLicenseNumber());
        surveyor.setExperienceYears(request.getExperienceYears());
        surveyor.setAssignedRegion(request.getAssignedRegion());

        // Save entities
        userRepository.save(user);
        surveyor.setUser(user);
        Surveyor savedSurveyor = surveyorRepository.save(surveyor);

        // Build and return response DTO
        return new SurveyorRegistrationResponse(
            savedSurveyor.getSurveyorId(),
            savedSurveyor.getUser().getUsername(),
            savedSurveyor.getUser().getEmail(),
            savedSurveyor.getUser().getPhoneNumber(),
            savedSurveyor.getLicenseNumber(),
            savedSurveyor.getExperienceYears(),
            savedSurveyor.getAssignedRegion(),
            "Surveyor registered successfully"
        );
    }

    public Surveyor saveSurveyor(Surveyor surveyor) {
        return surveyorRepository.save(surveyor);
    }

    public Surveyor getSurveyor(Long id) {

        return surveyorRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Surveyor not found"));
    }

    /**
     * Get all surveyors
     */
    public List<Surveyor> getAllSurveyors() {
        return surveyorRepository.findAll();
    }

    /**
     * Get surveyor by username
     */
    public Surveyor getSurveyorByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        return surveyorRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Surveyor not found for user: " + username));
    }

    /**
     * Update surveyor profile (for /surveyors/me endpoint)
     */
    public Surveyor updateSurveyorProfile(String username, UpdateSurveyorRequest request) {
        Surveyor surveyor = getSurveyorByUsername(username);

        // Update user phone number
        if (request.getPhoneNumber() != null) {
            surveyor.getUser().setPhoneNumber(request.getPhoneNumber());
        }

        // Update surveyor fields
        if (request.getLicenseNumber() != null) {
            surveyor.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getExperienceYears() != null) {
            surveyor.setExperienceYears(request.getExperienceYears());
        }
        if (request.getAssignedRegion() != null) {
            surveyor.setAssignedRegion(request.getAssignedRegion());
        }

        return surveyorRepository.save(surveyor);
    }

    /**
     * Delete surveyor by ID
     */
    public void deleteSurveyor(Long id) {
        if (!surveyorRepository.existsById(id)) {
            throw new RuntimeException("Surveyor not found with ID: " + id);
        }
        surveyorRepository.deleteById(id);
    }
}
