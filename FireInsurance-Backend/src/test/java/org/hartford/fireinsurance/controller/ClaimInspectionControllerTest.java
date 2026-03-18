package org.hartford.fireinsurance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hartford.fireinsurance.dto.ClaimInspectionRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.ClaimInspection;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.service.ClaimInspectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class ClaimInspectionControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private ClaimInspectionService claimInspectionService;

    private ObjectMapper objectMapper;
    private ClaimInspection inspection;
    private ClaimInspectionRequest inspectionRequest;
    private Claim claim;
    private Surveyor surveyor;
    private User surveyorUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

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
        inspection.setInspectionDate(LocalDateTime.now());

        inspectionRequest = new ClaimInspectionRequest();
        inspectionRequest.setEstimatedLoss(45000.0);
        inspectionRequest.setDamageReport("Severe fire damage to kitchen and living room");
    }

    @Test
    @DisplayName("POST /api/claim-inspections/assign/{claimId} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void assignSurveyor_Success() throws Exception {
        when(claimInspectionService.assignSurveyor(eq(1L), eq(1L))).thenReturn(inspection);

        mockMvc.perform(post("/api/claim-inspections/assign/1")
                        .with(csrf())
                        .param("surveyorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(1))
                .andExpect(jsonPath("$.claimId").value(1))
                .andExpect(jsonPath("$.surveyorName").value("surveyor1"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(claimInspectionService, times(1)).assignSurveyor(1L, 1L);
    }

    @Test
    @DisplayName("POST /api/claim-inspections/assign/{claimId} - Forbidden (SURVEYOR)")
    @WithMockUser(username = "surveyor", roles = {"SURVEYOR"})
    void assignSurveyor_Forbidden() throws Exception {
        mockMvc.perform(post("/api/claim-inspections/assign/1")
                        .with(csrf())
                        .param("surveyorId", "1"));

        verify(claimInspectionService, never()).assignSurveyor(anyLong(), anyLong());
    }

    @Test
    @DisplayName("POST /api/claim-inspections/assign/{claimId} - Claim Not Found")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void assignSurveyor_ClaimNotFound() throws Exception {
        when(claimInspectionService.assignSurveyor(eq(999L), eq(1L)))
                .thenThrow(new RuntimeException("Claim not found with ID: 999"));

        mockMvc.perform(post("/api/claim-inspections/assign/999")
                        .with(csrf())
                        .param("surveyorId", "1"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Claim not found with ID: 999"));

        verify(claimInspectionService, times(1)).assignSurveyor(999L, 1L);
    }

    @Test
    @DisplayName("PUT /api/claim-inspections/{inspectionId}/submit - Success (SURVEYOR)")
    @WithMockUser(username = "surveyor1", roles = {"SURVEYOR"})
    void submitInspection_Success() throws Exception {
        ClaimInspection submittedInspection = new ClaimInspection();
        submittedInspection.setClaimInspectionId(1L);
        submittedInspection.setClaim(claim);
        submittedInspection.setSurveyor(surveyor);
        submittedInspection.setStatus(ClaimInspection.ClaimInspectionStatus.UNDER_REVIEW);
        submittedInspection.setEstimatedLoss(45000.0);
        submittedInspection.setInspectionDate(LocalDateTime.now());

        when(claimInspectionService.submitInspection(eq(1L), eq("surveyor1"), any(ClaimInspectionRequest.class)))
                .thenReturn(submittedInspection);

        mockMvc.perform(put("/api/claim-inspections/1/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inspectionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(1))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.estimatedLoss").value(45000.0));

        verify(claimInspectionService, times(1)).submitInspection(eq(1L), eq("surveyor1"), any(ClaimInspectionRequest.class));
    }

    @Test
    @DisplayName("PUT /api/claim-inspections/{inspectionId}/submit - Forbidden (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void submitInspection_Forbidden() throws Exception {
        mockMvc.perform(put("/api/claim-inspections/1/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inspectionRequest)));

        verify(claimInspectionService, never()).submitInspection(anyLong(), anyString(), any(ClaimInspectionRequest.class));
    }

    @Test
    @DisplayName("PUT /api/claim-inspections/{inspectionId}/submit - Unauthorized Surveyor")
    @WithMockUser(username = "surveyor2", roles = {"SURVEYOR"})
    void submitInspection_UnauthorizedSurveyor() throws Exception {
        when(claimInspectionService.submitInspection(eq(1L), eq("surveyor2"), any(ClaimInspectionRequest.class)))
                .thenThrow(new RuntimeException("Unauthorized: Inspection does not belong to this surveyor"));

        mockMvc.perform(put("/api/claim-inspections/1/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inspectionRequest)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Unauthorized: Inspection does not belong to this surveyor"));

        verify(claimInspectionService, times(1)).submitInspection(eq(1L), eq("surveyor2"), any(ClaimInspectionRequest.class));
    }

    @Test
    @DisplayName("GET /api/claim-inspections/me - Success (SURVEYOR)")
    @WithMockUser(username = "surveyor1", roles = {"SURVEYOR"})
    void getMyInspections_Success() throws Exception {
        List<ClaimInspection> inspections = Arrays.asList(inspection);
        when(claimInspectionService.getBySurveyor("surveyor1")).thenReturn(inspections);

        mockMvc.perform(get("/api/claim-inspections/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inspectionId").value(1))
                .andExpect(jsonPath("$[0].claimId").value(1))
                .andExpect(jsonPath("$[0].surveyorName").value("surveyor1"));

        verify(claimInspectionService, times(1)).getBySurveyor("surveyor1");
    }

    @Test
    @DisplayName("GET /api/claim-inspections/me - Forbidden (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getMyInspections_Forbidden() throws Exception {
        mockMvc.perform(get("/api/claim-inspections/me"));

        verify(claimInspectionService, never()).getBySurveyor(anyString());
    }

    @Test
    @DisplayName("GET /api/claim-inspections - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAll_Success() throws Exception {
        List<ClaimInspection> inspections = Arrays.asList(inspection);
        when(claimInspectionService.getAll()).thenReturn(inspections);

        mockMvc.perform(get("/api/claim-inspections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inspectionId").value(1))
                .andExpect(jsonPath("$[0].claimId").value(1))
                .andExpect(jsonPath("$[0].status").value("ASSIGNED"));

        verify(claimInspectionService, times(1)).getAll();
    }

    @Test
    @DisplayName("GET /api/claim-inspections - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getAll_Forbidden() throws Exception {
        mockMvc.perform(get("/api/claim-inspections"));

        verify(claimInspectionService, never()).getAll();
    }

    @Test
    @DisplayName("GET /api/claim-inspections/{id} - Success (SURVEYOR)")
    @WithMockUser(username = "surveyor1", roles = {"SURVEYOR"})
    void getById_SuccessSurveyor() throws Exception {
        when(claimInspectionService.getById(1L)).thenReturn(inspection);

        mockMvc.perform(get("/api/claim-inspections/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(1))
                .andExpect(jsonPath("$.claimId").value(1))
                .andExpect(jsonPath("$.surveyorName").value("surveyor1"));

        verify(claimInspectionService, times(1)).getById(1L);
    }

    @Test
    @DisplayName("GET /api/claim-inspections/{id} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getById_SuccessAdmin() throws Exception {
        when(claimInspectionService.getById(1L)).thenReturn(inspection);

        mockMvc.perform(get("/api/claim-inspections/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(1))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(claimInspectionService, times(1)).getById(1L);
    }

    @Test
    @DisplayName("GET /api/claim-inspections/{id} - Not Found")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getById_NotFound() throws Exception {
        when(claimInspectionService.getById(999L))
                .thenThrow(new RuntimeException("Claim Inspection not found"));

        mockMvc.perform(get("/api/claim-inspections/999"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Claim Inspection not found"));

        verify(claimInspectionService, times(1)).getById(999L);
    }
}
