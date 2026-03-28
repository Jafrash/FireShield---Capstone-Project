package org.hartford.fireinsurance.config;

import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (userRepository.findByUsername("admin").isEmpty()) {

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@fireinsurance.com");
            admin.setPhoneNumber("9999999999");
            admin.setRole("ADMIN");
            admin.setActive(true);
            admin.setCreatedAt(LocalDateTime.now());
            admin.setPassword(passwordEncoder.encode("Admin@123"));

            userRepository.save(admin);

            System.out.println("Default admin created");
        }

        // Create SIU Investigator user
        if (userRepository.findByUsername("Mohit Kumar").isEmpty()) {

            User siuUser = new User();
            siuUser.setUsername("Mohit Kumar");
            siuUser.setEmail("mohit.kumar@fireinsurance.com");
            siuUser.setPhoneNumber("8888888888");
            siuUser.setRole("SIU_INVESTIGATOR");
            siuUser.setActive(true);
            siuUser.setCreatedAt(LocalDateTime.now());
            siuUser.setPassword(passwordEncoder.encode("Mohit@123"));

            userRepository.save(siuUser);

            System.out.println("Default SIU Investigator created");
        }
    }
}