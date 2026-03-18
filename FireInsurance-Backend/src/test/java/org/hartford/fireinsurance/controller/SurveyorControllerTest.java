package org.hartford.fireinsurance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hartford.fireinsurance.dto.UpdateSurveyorRequest;
import org.hartford.fireinsurance.model.Surveyor;
import org.hartford.fireinsurance.model.User;
import org.hartford.fireinsurance.service.SurveyorService;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class logicSurveyorControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private SurveyorService surveyorService;

    private ObjectMapper objectMapper;
    private Surveyor surveyor;
    private User user;
    private UpdateSurveyorRequest updateRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();

        user = new User();
        user.setId(1L);
        user.setUsername("surveyor1");
        user.setEmail("surveyor1@example.com");
        user.setPhoneNumber("1234567890");
        user.setRole("SURVEYOR");

        surveyor = new Surveyor();
        surveyor.setSurveyorId(1L);
        surveyor.setUser(user);
        surveyor.setLicenseNumber("LIC123");
        surveyor.setExperienceYears(5);
        surveyor.setAssignedRegion("North Region");

        updateRequest = new UpdateSurveyorRequest();
        updateRequest.setPhoneNumber("9876543210");
        updateRequest.setLicenseNumber("LIC456");
        updateRequest.setExperienceYears(7);
        updateRequest.setAssignedRegion("South Region");
    }

    @Test
    @DisplayName("GET /api/surveyors - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllSurveyors_Success() throws Exception {
        List<Surveyor> surveyors = Arrays.asList(surveyor);
        when(surveyorService.getAllSurveyors()).thenReturn(surveyors);

        mockMvc.perform(get("/api/surveyors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].surveyorId").value(1))
                .andExpect(jsonPath("$[0].username").value("surveyor1"))
                .andExpect(jsonPath("$[0].email").value("surveyor1@example.com"));

        verify(surveyorService, times(1)).getAllSurveyors();
    }

    @Test
    @DisplayName("GET /api/surveyors - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getAllSurveyors_Forbidden() throws Exception {
        mockMvc.perform(get("/api/surveyors"))
                .andExpect(status().isForbidden());

        verify(surveyorService, never()).getAllSurveyors();
    }

    @Test
    @DisplayName("GET /api/surveyors/{id} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getSurveyorById_Success() throws Exception {
        when(surveyorService.getSurveyor(1L)).thenReturn(surveyor);

        mockMvc.perform(get("/api/surveyors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.surveyorId").value(1))
                .andExpect(jsonPath("$.username").value("surveyor1"))
                .andExpect(jsonPath("$.licenseNumber").value("LIC123"));

        verify(surveyorService, times(1)).getSurveyor(1L);
    }

    @Test
    @DisplayName("GET /api/surveyors/{id} - Not Found")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getSurveyorById_NotFound() throws Exception {
        when(surveyorService.getSurveyor(999L))
                .thenThrow(new RuntimeException("Surveyor not found"));

        mockMvc.perform(get("/api/surveyors/999"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Surveyor not found"));

        verify(surveyorService, times(1)).getSurveyor(999L);
    }

    @Test
    @DisplayName("GET /api/surveyors/{id} - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getSurveyorById_Forbidden() throws Exception {
        mockMvc.perform(get("/api/surveyors/1"))
                .andExpect(status().isForbidden());

        verify(surveyorService, never()).getSurveyor(anyLong());
    }

    @Test
    @DisplayName("GET /api/surveyors/me - Forbidden (Non-Surveyor)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getMyProfile_Forbidden() throws Exception {
        mockMvc.perform(get("/api/surveyors/me"))
                .andExpect(status().isForbidden());

        verify(surveyorService, never()).getSurveyorByUsername(anyString());
    }

    @Test
    @DisplayName("PUT /api/surveyors/me - Forbidden (Non-Surveyor)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void updateMyProfile_Forbidden() throws Exception {
        mockMvc.perform(put("/api/surveyors/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        verify(surveyorService, never()).updateSurveyorProfile(anyString(), any(UpdateSurveyorRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/surveyors/{id} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteSurveyor_Success() throws Exception {
        doNothing().when(surveyorService).deleteSurveyor(1L);

        mockMvc.perform(delete("/api/surveyors/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Surveyor deleted successfully"));

        verify(surveyorService, times(1)).deleteSurveyor(1L);
    }

    @Test
    @DisplayName("DELETE /api/surveyors/{id} - Not Found")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteSurveyor_NotFound() throws Exception {
        doThrow(new RuntimeException("Surveyor not found with ID: 999"))
                .when(surveyorService).deleteSurveyor(999L);

        mockMvc.perform(delete("/api/surveyors/999")
                        .with(csrf()))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Surveyor not found with ID: 999"));

        verify(surveyorService, times(1)).deleteSurveyor(999L);
    }

    @Test
    @DisplayName("DELETE /api/surveyors/{id} - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void deleteSurveyor_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/surveyors/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(surveyorService, never()).deleteSurveyor(anyLong());
    }

    @Test
    @DisplayName("DELETE /api/surveyors/{id} - Forbidden (SURVEYOR)")
    @WithMockUser(username = "surveyor1", roles = {"SURVEYOR"})
    void deleteSurveyor_ForbiddenForSurveyor() throws Exception {
        mockMvc.perform(delete("/api/surveyors/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(surveyorService, never()).deleteSurveyor(anyLong());
    }
}
