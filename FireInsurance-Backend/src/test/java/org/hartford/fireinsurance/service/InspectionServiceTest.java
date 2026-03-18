package org.hartford.fireinsurance.service;

import org.hartford.fireinsurance.dto.InspectionRequest;
import org.hartford.fireinsurance.model.Inspection;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.repository.InspectionRepository;
import org.hartford.fireinsurance.repository.PolicySubscriptionRepository;
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
class InspectionServiceTest {

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private PropertyService propertyService;

    @Mock
    private SurveyorService surveyorService;

    @Mock
    private PolicySubscriptionRepository subscriptionRepository;

    @InjectMocks
    private InspectionService inspectionService;

    private Property property;
    private Surveyor surveyor;
    private User surveyorUser;
    private Inspection inspection;
    private InspectionRequest inspectionRequest;
    private PolicySubscription subscription;

    @BeforeEach
    void setUp() {
        surveyorUser = new User();
        surveyorUser.setId(1L);
        surveyorUser.setUsername("surveyor1");

        surveyor = new Surveyor();
        surveyor.setSurveyorId(1L);
        surveyor.setUser(surveyorUser);

        property = new Property();
        property.setPropertyId(1L);
        property.setAddress("123 Main St");

        inspection = new Inspection();
        inspection.setInspectionId(1L);
        inspection.setProperty(property);
        inspection.setSurveyor(surveyor);
        inspection.setStatus(Inspection.InspectionStatus.ASSIGNED);

        inspectionRequest = new InspectionRequest();
        inspectionRequest.setAssessedRiskScore(5.5);
        inspectionRequest.setRemarks("Property in good condition with moderate fire risk");

        subscription = new PolicySubscription();
        subscription.setSubscriptionId(1L);
        subscription.setProperty(property);
        subscription.setStatus(PolicySubscription.SubscriptionStatus.REQUESTED);
        subscription.setBasePremiumAmount(1000.0);
    }

    @Test
    @DisplayName("assignSurveyor - Success")
    void assignSurveyor_Success() {
        when(propertyService.getPropertyById(1L)).thenReturn(property);
        when(surveyorService.getSurveyor(1L)).thenReturn(surveyor);
        when(inspectionRepository.save(any(Inspection.class))).thenReturn(inspection);
        when(subscriptionRepository.findByProperty(property)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        Inspection result = inspectionService.assignSurveyor(1L, 1L);

        assertNotNull(result);
        assertEquals(1L, result.getInspectionId());
        assertEquals(Inspection.InspectionStatus.ASSIGNED, result.getStatus());
        assertEquals(1L, result.getProperty().getPropertyId());
        assertEquals(1L, result.getSurveyor().getSurveyorId());

        verify(propertyService, times(1)).getPropertyById(1L);
        verify(surveyorService, times(1)).getSurveyor(1L);
        verify(inspectionRepository, times(1)).save(any(Inspection.class));
        verify(subscriptionRepository, times(1)).findByProperty(property);
        verify(subscriptionRepository, times(1)).save(any(PolicySubscription.class));
    }

    @Test
    @DisplayName("assignSurveyor - Property Not Found")
    void assignSurveyor_PropertyNotFound() {
        when(propertyService.getPropertyById(999L))
                .thenThrow(new RuntimeException("Property not found with ID: 999"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inspectionService.assignSurveyor(999L, 1L);
        });

        assertEquals("Property not found with ID: 999", exception.getMessage());
        verify(propertyService, times(1)).getPropertyById(999L);
        verify(surveyorService, never()).getSurveyor(anyLong());
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    @DisplayName("assignSurveyor - Surveyor Not Found")
    void assignSurveyor_SurveyorNotFound() {
        when(propertyService.getPropertyById(1L)).thenReturn(property);
        when(surveyorService.getSurveyor(999L))
                .thenThrow(new RuntimeException("Surveyor not found with ID: 999"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inspectionService.assignSurveyor(1L, 999L);
        });

        assertEquals("Surveyor not found with ID: 999", exception.getMessage());
        verify(propertyService, times(1)).getPropertyById(1L);
        verify(surveyorService, times(1)).getSurveyor(999L);
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    @DisplayName("submitInspection - Success with Premium Calculation")
    void submitInspection_Success() {
        Inspection submittedInspection = new Inspection();
        submittedInspection.setInspectionId(1L);
        submittedInspection.setProperty(property);
        submittedInspection.setSurveyor(surveyor);
        submittedInspection.setStatus(Inspection.InspectionStatus.COMPLETED);
        submittedInspection.setAssessedRiskScore(5.5);
        submittedInspection.setRemarks("Property in good condition with moderate fire risk");
        submittedInspection.setInspectionDate(LocalDateTime.now());

        subscription.setStatus(PolicySubscription.SubscriptionStatus.PENDING);

        when(inspectionRepository.findById(1L)).thenReturn(Optional.of(inspection));
        when(inspectionRepository.save(any(Inspection.class))).thenReturn(submittedInspection);
        when(subscriptionRepository.findByProperty(property)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        Inspection result = inspectionService.submitInspection(1L, "surveyor1", inspectionRequest);

        assertNotNull(result);
        assertEquals(Inspection.InspectionStatus.COMPLETED, result.getStatus());
        assertEquals(5.5, result.getAssessedRiskScore());
        assertEquals("Property in good condition with moderate fire risk", result.getRemarks());
        assertNotNull(result.getInspectionDate());

        verify(inspectionRepository, times(1)).findById(1L);
        verify(inspectionRepository, times(1)).save(any(Inspection.class));
        verify(subscriptionRepository, times(1)).findByProperty(property);
        verify(subscriptionRepository, times(1)).save(any(PolicySubscription.class));
    }

    @Test
    @DisplayName("submitInspection - Inspection Not Found")
    void submitInspection_NotFound() {
        when(inspectionRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inspectionService.submitInspection(999L, "surveyor1", inspectionRequest);
        });

        assertEquals("Inspection not found with ID: 999", exception.getMessage());
        verify(inspectionRepository, times(1)).findById(999L);
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    @DisplayName("submitInspection - Unauthorized Surveyor")
    void submitInspection_UnauthorizedSurveyor() {
        when(inspectionRepository.findById(1L)).thenReturn(Optional.of(inspection));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inspectionService.submitInspection(1L, "surveyor2", inspectionRequest);
        });

        assertEquals("Unauthorized: Inspection does not belong to this surveyor", exception.getMessage());
        verify(inspectionRepository, times(1)).findById(1L);
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    @DisplayName("submitInspection - Invalid Risk Score (Too High)")
    void submitInspection_InvalidRiskScoreTooHigh() {
        InspectionRequest invalidRequest = new InspectionRequest();
        invalidRequest.setAssessedRiskScore(15.0);
        invalidRequest.setRemarks("Invalid risk score");

        when(inspectionRepository.findById(1L)).thenReturn(Optional.of(inspection));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inspectionService.submitInspection(1L, "surveyor1", invalidRequest);
        });

        assertEquals("Risk score must be between 0 and 10. Provided: 15.0", exception.getMessage());
        verify(inspectionRepository, times(1)).findById(1L);
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    @DisplayName("submitInspection - Invalid Risk Score (Negative)")
    void submitInspection_InvalidRiskScoreNegative() {
        InspectionRequest invalidRequest = new InspectionRequest();
        invalidRequest.setAssessedRiskScore(-1.0);
        invalidRequest.setRemarks("Negative risk score");

        when(inspectionRepository.findById(1L)).thenReturn(Optional.of(inspection));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inspectionService.submitInspection(1L, "surveyor1", invalidRequest);
        });

        assertEquals("Risk score must be between 0 and 10. Provided: -1.0", exception.getMessage());
        verify(inspectionRepository, times(1)).findById(1L);
        verify(inspectionRepository, never()).save(any(Inspection.class));
    }

    @Test
    @DisplayName("submitInspection - Low Risk Premium Calculation")
    void submitInspection_LowRiskPremiumCalculation() {
        InspectionRequest lowRiskRequest = new InspectionRequest();
        lowRiskRequest.setAssessedRiskScore(1.5);
        lowRiskRequest.setRemarks("Low risk property");

        Inspection submittedInspection = new Inspection();
        submittedInspection.setInspectionId(1L);
        submittedInspection.setProperty(property);
        submittedInspection.setSurveyor(surveyor);
        submittedInspection.setStatus(Inspection.InspectionStatus.COMPLETED);
        submittedInspection.setAssessedRiskScore(1.5);

        subscription.setStatus(PolicySubscription.SubscriptionStatus.PENDING);

        when(inspectionRepository.findById(1L)).thenReturn(Optional.of(inspection));
        when(inspectionRepository.save(any(Inspection.class))).thenReturn(submittedInspection);
        when(subscriptionRepository.findByProperty(property)).thenReturn(Arrays.asList(subscription));
        when(subscriptionRepository.save(any(PolicySubscription.class))).thenReturn(subscription);

        Inspection result = inspectionService.submitInspection(1L, "surveyor1", lowRiskRequest);

        assertNotNull(result);
        assertEquals(1.5, result.getAssessedRiskScore());

        verify(inspectionRepository, times(1)).save(any(Inspection.class));
        verify(subscriptionRepository, times(1)).save(any(PolicySubscription.class));
    }

    @Test
    @DisplayName("getBySurveyor - Success")
    void getBySurveyor_Success() {
        Inspection inspection2 = new Inspection();
        inspection2.setInspectionId(2L);
        inspection2.setSurveyor(surveyor);

        List<Inspection> inspections = Arrays.asList(inspection, inspection2);

        when(surveyorService.getSurveyorByUsername("surveyor1")).thenReturn(surveyor);
        when(inspectionRepository.findBySurveyor(surveyor)).thenReturn(inspections);

        List<Inspection> result = inspectionService.getBySurveyor("surveyor1");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getInspectionId());
        assertEquals(2L, result.get(1).getInspectionId());

        verify(surveyorService, times(1)).getSurveyorByUsername("surveyor1");
        verify(inspectionRepository, times(1)).findBySurveyor(surveyor);
    }

    @Test
    @DisplayName("getAll - Success")
    void getAll_Success() {
        Inspection inspection2 = new Inspection();
        inspection2.setInspectionId(2L);

        List<Inspection> inspections = Arrays.asList(inspection, inspection2);
        when(inspectionRepository.findAll()).thenReturn(inspections);

        List<Inspection> result = inspectionService.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getInspectionId());
        assertEquals(2L, result.get(1).getInspectionId());

        verify(inspectionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getById - Success")
    void getById_Success() {
        when(inspectionRepository.findById(1L)).thenReturn(Optional.of(inspection));

        Inspection result = inspectionService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getInspectionId());
        assertEquals(Inspection.InspectionStatus.ASSIGNED, result.getStatus());

        verify(inspectionRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getById - Not Found")
    void getById_NotFound() {
        when(inspectionRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            inspectionService.getById(999L);
        });

        assertEquals("Inspection not found with ID: 999", exception.getMessage());
        verify(inspectionRepository, times(1)).findById(999L);
    }
}
