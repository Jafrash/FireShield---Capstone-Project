package org.hartford.fireinsurance.config;

import org.hartford.fireinsurance.filter.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;       
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;        
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;    
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwtFilter) throws Exception {
        
        http
                .cors(cors -> {
                }) // Enable CORS
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/h2-console/**",
                                "/api/auth/**",
                                "/api/test/**",
                                "/error")
                        .permitAll()

                        // Public read-only endpoints for the marketing/landing page
                        .requestMatchers(HttpMethod.GET, "/api/policies").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/quotes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/notification-preferences").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/notification-preferences/valid-events").permitAll()

                        // Admin and underwriter domains
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SIU_INVESTIGATOR")
                        .requestMatchers("/api/underwriter/**").hasRole("UNDERWRITER")

                        // Claims - include underwriter workflow
                        .requestMatchers("/api/claims/**").hasAnyRole("ADMIN", "SURVEYOR", "CUSTOMER", "UNDERWRITER")

                        // Claim inspections
                        .requestMatchers("/api/claim-inspections/**").hasAnyRole("SURVEYOR", "ADMIN", "UNDERWRITER")

                        // Property inspections
                        .requestMatchers("/api/inspections/**").hasAnyRole("SURVEYOR", "ADMIN", "UNDERWRITER")

                        // Surveyors
                        .requestMatchers("/api/surveyors/**").hasAnyRole("ADMIN", "UNDERWRITER")

                        // Customers
                        .requestMatchers("/api/customers/**").hasAnyRole("ADMIN", "CUSTOMER")

                        // Properties
                        .requestMatchers("/api/properties/**").hasAnyRole("ADMIN", "CUSTOMER", "SURVEYOR", "UNDERWRITER")

                        // Policy subscriptions
                        .requestMatchers("/api/subscriptions/**").hasAnyRole("ADMIN", "CUSTOMER", "UNDERWRITER")

                        .anyRequest().authenticated())

                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:4201"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
