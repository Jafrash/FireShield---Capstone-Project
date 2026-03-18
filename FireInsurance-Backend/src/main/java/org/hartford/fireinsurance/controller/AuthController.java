package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.CustomerRegistrationRequest;
import org.hartford.fireinsurance.dto.CustomerRegistrationResponse;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.UserRepository;
import org.hartford.fireinsurance.service.CustomerService;
import org.hartford.fireinsurance.utility.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private PasswordEncoder passwordEncoder;

        @Value("${google.client-id:}")
        private String googleClientId;

        private final RestTemplate restTemplate = new RestTemplate();

    // ================= LOGIN =================
    @PostMapping("/login")
    public JwtResponse login(@RequestBody JwtRequest request) {

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getRole()
        );
        return new JwtResponse(token);
    }

        @PostMapping("/login/google")
        public JwtResponse loginWithGoogle(@RequestBody GoogleLoginRequest request) {
                if (request == null || request.getCredential() == null || request.getCredential().isBlank()) {
                        throw new IllegalArgumentException("Google credential is required.");
                }

                if (googleClientId == null || googleClientId.isBlank()) {
                        throw new IllegalArgumentException("Google login is not configured on the server.");
                }

                GoogleTokenInfo tokenInfo = verifyGoogleToken(request.getCredential());

                if (!googleClientId.equals(tokenInfo.getAud())) {
                        throw new BadCredentialsException("Invalid Google token audience.");
                }

                if (!"true".equalsIgnoreCase(tokenInfo.getEmailVerified())) {
                        throw new BadCredentialsException("Google email is not verified.");
                }

                long expirationEpoch;
                try {
                        expirationEpoch = Long.parseLong(tokenInfo.getExp());
                } catch (NumberFormatException ex) {
                        throw new BadCredentialsException("Invalid Google token expiration.");
                }

                if (Instant.now().getEpochSecond() >= expirationEpoch) {
                        throw new BadCredentialsException("Google token has expired.");
                }

                User user = userRepository.findByEmail(tokenInfo.getEmail())
                                .orElseThrow(() -> new BadCredentialsException("Google account is not linked to an existing customer account."));

                if (!"CUSTOMER".equalsIgnoreCase(user.getRole())) {
                        throw new BadCredentialsException("Google login is available for customer accounts only.");
                }

                String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                return new JwtResponse(token);
        }

        private GoogleTokenInfo verifyGoogleToken(String idToken) {
                String url = UriComponentsBuilder
                        .fromUriString("https://oauth2.googleapis.com/tokeninfo")
                                .queryParam("id_token", idToken)
                                .toUriString();

                try {
                        ResponseEntity<GoogleTokenInfo> response = restTemplate.getForEntity(url, GoogleTokenInfo.class);
                        GoogleTokenInfo tokenInfo = response.getBody();
                        if (!response.getStatusCode().is2xxSuccessful() || tokenInfo == null || tokenInfo.getEmail() == null || tokenInfo.getEmail().isBlank()) {
                                throw new BadCredentialsException("Unable to validate Google token.");
                        }
                        return tokenInfo;
                } catch (RestClientException ex) {
                        throw new BadCredentialsException("Unable to validate Google token.");
                }
        }

        static class GoogleTokenInfo {
                private String aud;
                private String email;
                private String email_verified;
                private String exp;

                public String getAud() {
                        return aud;
                }

                public void setAud(String aud) {
                        this.aud = aud;
                }

                public String getEmail() {
                        return email;
                }

                public void setEmail(String email) {
                        this.email = email;
                }

                public String getEmailVerified() {
                        return email_verified;
                }

                public void setEmail_verified(String email_verified) {
                        this.email_verified = email_verified;
                }

                public String getExp() {
                        return exp;
                }

                public void setExp(String exp) {
                        this.exp = exp;
                }
        }

    // ================= REGISTER CUSTOMER =================

    @PostMapping("/register/customer")
    public ResponseEntity<CustomerRegistrationResponse> registerCustomer(@RequestBody CustomerRegistrationRequest request) {
        CustomerRegistrationResponse response = customerService.registerCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

        @PostMapping("/forgot-password/customer")
        public ResponseEntity<Map<String, String>> resetCustomerPassword(@RequestBody CustomerForgotPasswordRequest request) {
                customerService.resetCustomerPassword(request);
                return ResponseEntity.ok(Map.of("message", "Password reset successful. You can now log in with your new password."));
        }
}