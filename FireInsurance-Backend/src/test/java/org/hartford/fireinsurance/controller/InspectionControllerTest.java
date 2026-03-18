package org.hartford.fireinsurance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hartford.fireinsurance.dto.InspectionRequest;
import org.hartford.fireinsurance.model.Inspection;
import org.hartford.fireinsurance.model.Property;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.service.InspectionService;
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
class InspectionControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private InspectionService inspectionService;

    private ObjectMapper objectMapper;
    private Inspection inspection;
    private InspectionRequest inspectionRequest;
    private Property property;
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

        property = new Property();
        property.setPropertyId(1L);
        property.setAddress("123 Main St");

        inspection = new Inspection();
        inspection.setInspectionId(1L);
        inspection.setProperty(property);
        inspection.setSurveyor(surveyor);
        inspection.setStatus(Inspection.InspectionStatus.ASSIGNED);
        inspection.setInspectionDate(LocalDateTime.now());

        inspectionRequest = new InspectionRequest();
        inspectionRequest.setAssessedRiskScore(5.5);
        inspectionRequest.setRemarks("Property in good condition with moderate fire risk");
    }

    @Test
    @DisplayName("POST /api/inspections/assign/{propertyId} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void assignSurveyor_Success() throws Exception {
        when(inspectionService.assignSurveyor(eq(1L), eq(1L))).thenReturn(inspection);

        mockMvc.perform(post("/api/inspections/assign/1")
                        .with(csrf())
                        .param("surveyorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(1))
                .andExpect(jsonPath("$.propertyId").value(1))
                .andExpect(jsonPath("$.surveyorName").value("surveyor1"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(inspectionService, times(1)).assignSurveyor(1L, 1L);
    }

    @Test
    @DisplayName("POST /api/inspections/assign/{propertyId} - Forbidden (SURVEYOR)")
    @WithMockUser(username = "surveyor", roles = {"SURVEYOR"})
    void assignSurveyor_Forbidden() throws Exception {
        mockMvc.perform(post("/api/inspections/assign/1")
                        .with(csrf())
                        .param("surveyorId", "1"));

        verify(inspectionService, never()).assignSurveyor(anyLong(), anyLong());
    }

    @Test
    @DisplayName("POST /api/inspections/assign/{propertyId} - Property Not Found")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void assignSurveyor_PropertyNotFound() throws Exception {
        when(inspectionService.assignSurveyor(eq(999L), eq(1L)))
                .thenThrow(new RuntimeException("Property not found with ID: 999"));

        mockMvc.perform(post("/api/inspections/assign/999")
                        .with(csrf())
                        .param("surveyorId", "1"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Property not found with ID: 999"));

        verify(inspectionService, times(1)).assignSurveyor(999L, 1L);
    }

    @Test
    @DisplayName("PUT /api/inspections/{inspectionId}/submit - Success (SURVEYOR)")
    @WithMockUser(username = "surveyor1", roles = {"SURVEYOR"})
    void submitInspection_Success() throws Exception {
        Inspection submittedInspection = new Inspection();
        submittedInspection.setInspectionId(1L);
        submittedInspection.setProperty(property);
        submittedInspection.setSurveyor(surveyor);
        submittedInspection.setStatus(Inspection.InspectionStatus.COMPLETED);
        submittedInspection.setAssessedRiskScore(5.5);
        submittedInspection.setInspectionDate(LocalDateTime.now());

        when(inspectionService.submitInspection(eq(1L), eq("surveyor1"), any(InspectionRequest.class)))
                .thenReturn(submittedInspection);

        mockMvc.perform(put("/api/inspections/1/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inspectionRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(1))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.assessedRiskScore").value(5.5));

        verify(inspectionService, times(1)).submitInspection(eq(1L), eq("surveyor1"), any(InspectionRequest.class));
    }

    @Test
    @DisplayName("PUT /api/inspections/{inspectionId}/submit - Forbidden (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void submitInspection_Forbidden() throws Exception {
        mockMvc.perform(put("/api/inspections/1/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inspectionRequest)));

        verify(inspectionService, never()).submitInspection(anyLong(), anyString(), any(InspectionRequest.class));
    }

    @Test
    @DisplayName("PUT /api/inspections/{inspectionId}/submit - Unauthorized Surveyor")
    @WithMockUser(username = "surveyor2", roles = {"SURVEYOR"})
    void submitInspection_UnauthorizedSurveyor() throws Exception {
        when(inspectionService.submitInspection(eq(1L), eq("surveyor2"), any(InspectionRequest.class)))
                .thenThrow(new RuntimeException("Unauthorized: Inspection does not belong to this surveyor"));

        mockMvc.perform(put("/api/inspections/1/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inspectionRequest)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Unauthorized: Inspection does not belong to this surveyor"));

        verify(inspectionService, times(1)).submitInspection(eq(1L), eq("surveyor2"), any(InspectionRequest.class));
    }

    @Test
    @DisplayName("PUT /api/inspections/{inspectionId}/submit - Invalid Risk Score")
    @WithMockUser(username = "surveyor1", roles = {"SURVEYOR"})
    void submitInspection_InvalidRiskScore() throws Exception {
        InspectionRequest invalidRequest = new InspectionRequest();
        invalidRequest.setAssessedRiskScore(15.0);
        invalidRequest.setRemarks("Invalid risk score");

        when(inspectionService.submitInspection(eq(1L), eq("surveyor1"), any(InspectionRequest.class)))
                .thenThrow(new RuntimeException("Risk score must be between 0 and 10. Provided: 15.0"));

        mockMvc.perform(put("/api/inspections/1/submit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Risk score must be between 0 and 10. Provided: 15.0"));

        verify(inspectionService, times(1)).submitInspection(eq(1L), eq("surveyor1"), any(InspectionRequest.class));
    }

    @Test
    @DisplayName("GET /api/inspections/me - Success (SURVEYOR)")
    @WithMockUser(username = "surveyor1", roles = {"SURVEYOR"})
    void getMyInspections_Success() throws Exception {
        List<Inspection> inspections = Arrays.asList(inspection);
        when(inspectionService.getBySurveyor("surveyor1")).thenReturn(inspections);

        mockMvc.perform(get("/api/inspections/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inspectionId").value(1))
                .andExpect(jsonPath("$[0].propertyId").value(1))
                .andExpect(jsonPath("$[0].surveyorName").value("surveyor1"));

        verify(inspectionService, times(1)).getBySurveyor("surveyor1");
    }

    @Test
    @DisplayName("GET /api/inspections/me - Forbidden (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getMyInspections_Forbidden() throws Exception {
        mockMvc.perform(get("/api/inspections/me"));

        verify(inspectionService, never()).getBySurveyor(anyString());
    }

    @Test
    @DisplayName("GET /api/inspections - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAll_Success() throws Exception {
        List<Inspection> inspections = Arrays.asList(inspection);
        when(inspectionService.getAll()).thenReturn(inspections);

        mockMvc.perform(get("/api/inspections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].inspectionId").value(1))
                .andExpect(jsonPath("$[0].propertyId").value(1))
                .andExpect(jsonPath("$[0].status").value("ASSIGNED"));

        verify(inspectionService, times(1)).getAll();
    }

    @Test
    @DisplayName("GET /api/inspections - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getAll_Forbidden() throws Exception {
        mockMvc.perform(get("/api/inspections"));

        verify(inspectionService, never()).getAll();
    }

    @Test
    @DisplayName("GET /api/inspections/{id} - Success (SURVEYOR)")
    @WithMockUser(username = "surveyor1", roles = {"SURVEYOR"})
    void getById_SuccessSurveyor() throws Exception {
        when(inspectionService.getById(1L)).thenReturn(inspection);

        mockMvc.perform(get("/api/inspections/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(1))
                .andExpect(jsonPath("$.propertyId").value(1))
                .andExpect(jsonPath("$.surveyorName").value("surveyor1"));

        verify(inspectionService, times(1)).getById(1L);
    }

    @Test
    @DisplayName("GET /api/inspections/{id} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getById_SuccessAdmin() throws Exception {
        when(inspectionService.getById(1L)).thenReturn(inspection);

        mockMvc.perform(get("/api/inspections/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inspectionId").value(1))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(inspectionService, times(1)).getById(1L);
    }

    @Test
    @DisplayName("GET /api/inspections/{id} - Not Found")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getById_NotFound() throws Exception {
        when(inspectionService.getById(999L))
                .thenThrow(new RuntimeException("Inspection not found with ID: 999"));

        mockMvc.perform(get("/api/inspections/999"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Inspection not found with ID: 999"));

        verify(inspectionService, times(1)).getById(999L);
    }
}
