package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.ClaimInspectionRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.ClaimInspection;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.ClaimInspectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimInspectionServiceTest {

    @Mock
    private ClaimInspectionRepository claimInspectionRepository;

    @Mock
    private ClaimService claimService;

    @Mock
    private SurveyorService surveyorService;

    @InjectMocks
    private ClaimInspectionService claimInspectionService;

    private Claim claim;
    private Surveyor surveyor;
    private User surveyorUser;
    private ClaimInspection inspection;
    private ClaimInspectionRequest inspectionRequest;

    @BeforeEach
    void setUp() {
        surveyorUser = new User();
        surveyorUser.setId(1L);
        surveyorUser.setUsername("surveyor1");

        surveyor = new Surveyor();
        surveyor.setSurveyorId(1L);
        surveyor.setUser(surveyorUser);

        claim = new Claim();
        claim.setClaimId(1L);
        claim.setStatus(Claim.ClaimStatus.SUBMITTED);

        inspection = new ClaimInspection();
        inspection.setClaimInspectionId(1L);
        inspection.setClaim(claim);
        inspection.setSurveyor(surveyor);
        inspection.setStatus(ClaimInspection.ClaimInspectionStatus.ASSIGNED);

        inspectionRequest = new ClaimInspectionRequest();
        inspectionRequest.setEstimatedLoss(45000.0);
        inspectionRequest.setDamageReport("Severe fire damage to kitchen and living room");
    }

    @Test
    @DisplayName("assignSurveyor - Success")
    void assignSurveyor_Success() {
        when(claimService.getClaimById(1L)).thenReturn(claim);
        when(surveyorService.getSurveyor(1L)).thenReturn(surveyor);
        when(claimInspectionRepository.save(any(ClaimInspection.class))).thenReturn(inspection);
        when(claimService.updateClaimStatus(eq(1L), eq(Claim.ClaimStatus.INSPECTING))).thenReturn(claim);

        ClaimInspection result = claimInspectionService.assignSurveyor(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getClaimInspectionId());
        assertEquals(ClaimInspection.ClaimInspectionStatus.ASSIGNED, result.getStatus());
        assertEquals(1L, result.getClaim().getClaimId());
        assertEquals(1L, result.getSurveyor().getSurveyorId());

        verify(claimService, times(1)).getClaimById(1L);
        verify(surveyorService, times(1)).getSurveyor(1L);
        verify(claimInspectionRepository, times(1)).save(any(ClaimInspection.class));
        verify(claimService, times(1)).updateClaimStatus(1L, Claim.ClaimStatus.INSPECTING);
    }

    @Test
    @DisplayName("assignSurveyor - Claim Not Found")
    void assignSurveyor_ClaimNotFound() {
        when(claimService.getClaimById(999L))
                .thenThrow(new RuntimeException("Claim not found with ID: 999"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            claimInspectionService.assignSurveyor(999L, 1L);
        });

        assertEquals("Claim not found with ID: 999", exception.getMessage());
        verify(claimService, times(1)).getClaimById(999L);
        verify(surveyorService, never()).getSurveyor(anyLong());
        verify(claimInspectionRepository, never()).save(any(ClaimInspection.class));
    }

    @Test
    @DisplayName("assignSurveyor - Surveyor Not Found")
    void assignSurveyor_SurveyorNotFound() {
        when(claimService.getClaimById(1L)).thenReturn(claim);
        when(surveyorService.getSurveyor(999L))
                .thenThrow(new RuntimeException("Surveyor not found with ID: 999"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            claimInspectionService.assignSurveyor(1L, 999L);
        });

        assertEquals("Surveyor not found with ID: 999", exception.getMessage());
        verify(claimService, times(1)).getClaimById(1L);
        verify(surveyorService, times(1)).getSurveyor(999L);
        verify(claimInspectionRepository, never()).save(any(ClaimInspection.class));
    }

    @Test
    @DisplayName("submitInspection - Success")
    void submitInspection_Success() {
        ClaimInspection submittedInspection = new ClaimInspection();
        submittedInspection.setClaimInspectionId(1L);
        submittedInspection.setClaim(claim);
        submittedInspection.setSurveyor(surveyor);
        submittedInspection.setStatus(ClaimInspection.ClaimInspectionStatus.UNDER_REVIEW);
        submittedInspection.setEstimatedLoss(45000.0);
        submittedInspection.setDamageReport("Severe fire damage to kitchen and living room");
        submittedInspection.setInspectionDate(LocalDateTime.now());

        when(claimInspectionRepository.findById(1L)).thenReturn(Optional.of(inspection));
        when(claimInspectionRepository.save(any(ClaimInspection.class))).thenReturn(submittedInspection);
        when(claimService.updateClaimStatus(eq(1L), eq(Claim.ClaimStatus.INSPECTED))).thenReturn(claim);

        ClaimInspection result = claimInspectionService.submitInspection(1L, "surveyor1", inspectionRequest);

        assertNotNull(result);
        assertEquals(ClaimInspection.ClaimInspectionStatus.UNDER_REVIEW, result.getStatus());
        assertEquals(45000.0, result.getEstimatedLoss());
        assertEquals("Severe fire damage to kitchen and living room", result.getDamageReport());
        assertNotNull(result.getInspectionDate());

        verify(claimInspectionRepository, times(1)).findById(1L);
        verify(claimInspectionRepository, times(1)).save(any(ClaimInspection.class));
        verify(claimService, times(1)).updateClaimStatus(1L, Claim.ClaimStatus.INSPECTED);
    }

    @Test
    @DisplayName("submitInspection - Inspection Not Found")
    void submitInspection_NotFound() {
        when(claimInspectionRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            claimInspectionService.submitInspection(999L, "surveyor1", inspectionRequest);
        });

        assertEquals("Claim Inspection not found", exception.getMessage());
        verify(claimInspectionRepository, times(1)).findById(999L);
        verify(claimInspectionRepository, never()).save(any(ClaimInspection.class));
    }

    @Test
    @DisplayName("submitInspection - Unauthorized Surveyor")
    void submitInspection_UnauthorizedSurveyor() {
        when(claimInspectionRepository.findById(1L)).thenReturn(Optional.of(inspection));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            claimInspectionService.submitInspection(1L, "surveyor2", inspectionRequest);
        });

        assertEquals("Unauthorized: Inspection does not belong to this surveyor", exception.getMessage());
        verify(claimInspectionRepository, times(1)).findById(1L);
        verify(claimInspectionRepository, never()).save(any(ClaimInspection.class));
    }

    @Test
    @DisplayName("getBySurveyor - Success")
    void getBySurveyor_Success() {
        ClaimInspection inspection2 = new ClaimInspection();
        inspection2.setClaimInspectionId(2L);
        inspection2.setSurveyor(surveyor);

        List<ClaimInspection> inspections = Arrays.asList(inspection, inspection2);

        when(surveyorService.getSurveyorByUsername("surveyor1")).thenReturn(surveyor);
        when(claimInspectionRepository.findBySurveyor(surveyor)).thenReturn(inspections);

        List<ClaimInspection> result = claimInspectionService.getBySurveyor("surveyor1");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getClaimInspectionId());
        assertEquals(2L, result.get(1).getClaimInspectionId());

        verify(surveyorService, times(1)).getSurveyorByUsername("surveyor1");
        verify(claimInspectionRepository, times(1)).findBySurveyor(surveyor);
    }

    @Test
    @DisplayName("getAll - Success")
    void getAll_Success() {
        ClaimInspection inspection2 = new ClaimInspection();
        inspection2.setClaimInspectionId(2L);

        List<ClaimInspection> inspections = Arrays.asList(inspection, inspection2);
        when(claimInspectionRepository.findAll()).thenReturn(inspections);

        List<ClaimInspection> result = claimInspectionService.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getClaimInspectionId());
        assertEquals(2L, result.get(1).getClaimInspectionId());

        verify(claimInspectionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getById - Success")
    void getById_Success() {
        when(claimInspectionRepository.findById(1L)).thenReturn(Optional.of(inspection));

        ClaimInspection result = claimInspectionService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getClaimInspectionId());
        assertEquals(ClaimInspection.ClaimInspectionStatus.ASSIGNED, result.getStatus());

        verify(claimInspectionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById - Not Found")
    void getById_NotFound() {
        when(claimInspectionRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            claimInspectionService.getById(999L);
        });

        assertEquals("Claim Inspection not found", exception.getMessage());
        verify(claimInspectionRepository, times(1)).findById(999L);
    }
}
