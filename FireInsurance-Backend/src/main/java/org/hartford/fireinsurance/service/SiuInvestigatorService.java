package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.SiuInvestigatorRegistrationRequest;
import org.hartford.fireinsurance.dto.SiuInvestigatorResponse;
import org.hartford.fireinsurance.model.InvestigatorSpecialization;
import org.hartford.fireinsurance.model.SiuInvestigator;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.SiuInvestigatorRepository;
import org.hartford.fireinsurance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing SIU investigators
 */
@Service
@Transactional
public class SiuInvestigatorService {

    private static final Logger log = LoggerFactory.getLogger(SiuInvestigatorService.class);

    private final SiuInvestigatorRepository siuInvestigatorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SiuInvestigatorService(SiuInvestigatorRepository siuInvestigatorRepository,
                                  UserRepository userRepository,
                                  PasswordEncoder passwordEncoder) {
        this.siuInvestigatorRepository = siuInvestigatorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new SIU investigator
     */
    public SiuInvestigator registerSiuInvestigator(SiuInvestigatorRegistrationRequest request) {
        String fullName = request.getFirstName() + " " + request.getLastName();
        log.info("Registering new SIU investigator: {}", fullName);

        // Validation
        if (userRepository.existsByUsername(fullName)) {
            throw new RuntimeException("Name already exists: " + fullName);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }
        if (request.getBadgeNumber() != null && siuInvestigatorRepository.findByBadgeNumber(request.getBadgeNumber()).isPresent()) {
            throw new RuntimeException("Badge number already exists: " + request.getBadgeNumber());
        }

        // Create User entity
        User user = new User();
        // Store full name as username for display purposes
        user.setUsername(fullName);
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole("SIU_INVESTIGATOR");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Create SiuInvestigator entity
        SiuInvestigator investigator = new SiuInvestigator();
        investigator.setUser(savedUser);
        investigator.setUsername(fullName); // Set username to match database
        investigator.setBadgeNumber(request.getBadgeNumber() != null ? request.getBadgeNumber() : generateBadgeNumber());
        investigator.setSpecialization(request.getSpecialization());
        investigator.setExperienceYears(request.getExperienceYears());
        investigator.setActive(true);

        SiuInvestigator savedInvestigator = siuInvestigatorRepository.save(investigator);

        log.info("Successfully registered SIU investigator: {} with ID: {}",
                fullName, savedInvestigator.getInvestigatorId());

        return savedInvestigator;
    }

    /**
     * Get all SIU investigators
     */
    public List<SiuInvestigator> getAllSiuInvestigators() {
        return siuInvestigatorRepository.findAll();
    }

    /**
     * Get all active SIU investigators
     */
    public List<SiuInvestigator> getAllActiveInvestigators() {
        return siuInvestigatorRepository.findByActiveTrue();
    }

    /**
     * Get SIU investigator by ID
     */
    public SiuInvestigator getInvestigatorById(Long investigatorId) {
        return siuInvestigatorRepository.findById(investigatorId)
                .orElseThrow(() -> new RuntimeException("SIU Investigator not found with ID: " + investigatorId));
    }

    /**
     * Get SIU investigator by user ID
     */
    public Optional<SiuInvestigator> getInvestigatorByUserId(Long userId) {
        return siuInvestigatorRepository.findByUser_Id(userId);
    }

    /**
     * Activate SIU investigator
     */
    public void activateInvestigator(Long investigatorId) {
        SiuInvestigator investigator = getInvestigatorById(investigatorId);
        investigator.setActive(true);
        investigator.getUser().setActive(true);
        siuInvestigatorRepository.save(investigator);

        log.info("Activated SIU investigator: {}", investigator.getUser().getUsername());
    }

    /**
     * Deactivate SIU investigator
     */
    public void deactivateInvestigator(Long investigatorId) {
        SiuInvestigator investigator = getInvestigatorById(investigatorId);
        investigator.setActive(false);
        investigator.getUser().setActive(false);
        siuInvestigatorRepository.save(investigator);

        log.info("Deactivated SIU investigator: {}", investigator.getUser().getUsername());
    }

    /**
     * Get active investigators count
     */
    public long getActiveInvestigatorsCount() {
        return siuInvestigatorRepository.countByActiveTrue();
    }

    /**
     * Generate unique badge number
     */
    public String generateBadgeNumber() {
        long count = siuInvestigatorRepository.count();
        String badgeNumber;
        do {
            count++;
            badgeNumber = String.format("SIU-%04d", count);
        } while (siuInvestigatorRepository.existsByBadgeNumber(badgeNumber));

        return badgeNumber;
    }
}