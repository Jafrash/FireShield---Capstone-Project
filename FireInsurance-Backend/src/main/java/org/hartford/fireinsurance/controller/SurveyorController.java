package org.hartford.fireinsurance.controller;

import org.hartford.fireinsurance.dto.SurveyorResponse;
import org.hartford.fireinsurance.dto.UpdateSurveyorRequest;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.service.SurveyorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/surveyors")
public class SurveyorController {

    private final SurveyorService surveyorService;

    public SurveyorController(SurveyorService surveyorService) {
        this.surveyorService = surveyorService;
    }

    // =============================
    // ADMIN - Get all surveyors
    // =============================
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'UNDERWRITER')")
    public ResponseEntity<List<SurveyorResponse>> getAllSurveyors() {

        List<SurveyorResponse> response =
                surveyorService.getAllSurveyors()
                        .stream()
                        .map(this::mapToResponse)
                        .toList();

        return ResponseEntity.ok(response);
    }

    // =============================
    // ADMIN - Get surveyor by ID
    // =============================
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SurveyorResponse> getSurveyorById(@PathVariable Long id) {

        Surveyor surveyor = surveyorService.getSurveyor(id);
        return ResponseEntity.ok(mapToResponse(surveyor));
    }

    // =============================
    // SURVEYOR - Get own profile
    // =============================
    @GetMapping("/me")
    @PreAuthorize("hasRole('SURVEYOR')")
    public ResponseEntity<SurveyorResponse> getMyProfile(Authentication authentication) {

        String username = authentication.getName();

        Surveyor surveyor = surveyorService.getSurveyorByUsername(username);

        return ResponseEntity.ok(mapToResponse(surveyor));
    }

    // =============================
    // SURVEYOR - Update own profile
    // =============================
    @PutMapping("/me")
    @PreAuthorize("hasRole('SURVEYOR')")
    public ResponseEntity<SurveyorResponse> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateSurveyorRequest request) {

        String username = authentication.getName();

        Surveyor updated = surveyorService.updateSurveyorProfile(username, request);

        return ResponseEntity.ok(mapToResponse(updated));
    }

    // =============================
    // ADMIN - Delete surveyor
    // =============================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteSurveyor(@PathVariable Long id) {

        surveyorService.deleteSurveyor(id);
        return ResponseEntity.ok("Surveyor deleted successfully");
    }

    // =============================
    // Mapper
    // =============================
    private SurveyorResponse mapToResponse(Surveyor surveyor) {

        return new SurveyorResponse(
                surveyor.getSurveyorId(),
                surveyor.getUser().getUsername(),
                surveyor.getUser().getEmail(),
                surveyor.getUser().getPhoneNumber(),
                surveyor.getLicenseNumber(),
                surveyor.getExperienceYears(),
                surveyor.getAssignedRegion()
        );
    }
}
