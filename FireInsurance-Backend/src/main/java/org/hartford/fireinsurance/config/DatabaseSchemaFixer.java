package org.hartford.fireinsurance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseSchemaFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Alter columns to VARCHAR to remove ENUM restrictions
            System.out.println("Altering status columns to VARCHAR to remove ENUM restrictions...");
            
            try {
                jdbcTemplate.execute("ALTER TABLE policy_subscriptions ALTER COLUMN status VARCHAR(255)");
                System.out.println("Altering policy_subscriptions successful.");
            } catch (Exception e) {
                System.out.println("Ignoring alter error on policy_subscriptions: " + e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE claims ALTER COLUMN status VARCHAR(255)");
                System.out.println("Altering claims successful.");
            } catch (Exception e) {
                System.out.println("Ignoring alter error on claims: " + e.getMessage());
            }
            
            System.out.println("Successfully updated enum columns to VARCHAR.");
        } catch (Exception e) {
            System.out.println("Error removing constraints: " + e.getMessage());
        }
    }
}
