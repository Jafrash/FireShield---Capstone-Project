package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.SiuInvestigatorRegistrationRequest;
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

    public SiuInvestigator createSiuInvestigator(SiuInvestigatorRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhone());
        user.setRole("SIU_INVESTIGATOR");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());

        SiuInvestigator investigator = new SiuInvestigator();
        investigator.setUser(user);
        investigator.setFirstName(request.getFirstName());
        investigator.setLastName(request.getLastName());
        investigator.setBadgeNumber(request.getBadgeNumber());
        investigator.setDepartment(request.getDepartment());
        investigator.setExperienceYears(request.getExperienceYears());
        investigator.setSpecialization(request.getSpecialization());
        investigator.setActive(true);
        investigator.setCreatedAt(LocalDateTime.now());

        log.info("Creating SIU investigator user={} department={}", request.getUsername(), request.getDepartment());
        return siuInvestigatorRepository.save(investigator);
    }

    public List<SiuInvestigator> getAllSiuInvestigators() {
        return siuInvestigatorRepository.findAll();
    }

    public SiuInvestigator getSiuInvestigatorById(Long id) {
        return siuInvestigatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SIU investigator not found with ID: " + id));
    }

    public SiuInvestigator getByUsername(String username) {
        return siuInvestigatorRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("SIU investigator not found for username: " + username));
    }

    public void deleteSiuInvestigator(Long id) {
        SiuInvestigator investigator = getSiuInvestigatorById(id);
        log.info("Deleting SIU investigator ID={} username={}", id, investigator.getUser().getUsername());
        siuInvestigatorRepository.delete(investigator);
    }
}