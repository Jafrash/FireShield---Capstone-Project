package org.hartford.fireinsurance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hartford.fireinsurance.dto.CreateClaimRequest;
import org.hartford.fireinsurance.dto.UpdateClaimStatusRequest;
import org.hartford.fireinsurance.model.Claim;
import org.hartford.fireinsurance.model.PolicySubscription;
import org.hartford.fireinsurance.service.ClaimService;
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

import java.time.LocalDate;
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
class ClaimControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private ClaimService claimService;

    private ObjectMapper objectMapper;
    private Claim claim;
    private CreateClaimRequest createRequest;
    private UpdateClaimStatusRequest updateStatusRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        PolicySubscription subscription = new PolicySubscription();
        subscription.setSubscriptionId(1L);

        claim = new Claim();
        claim.setClaimId(1L);
        claim.setSubscription(subscription);
        claim.setDescription("Fire damage to property");
        claim.setClaimAmount(50000.0);
        claim.setStatus(Claim.ClaimStatus.SUBMITTED);
        claim.setCreatedAt(LocalDateTime.now());
        claim.setIncidentDate(LocalDate.now());

        createRequest = new CreateClaimRequest();
        createRequest.setSubscriptionId(1L);
        createRequest.setDescription("Fire damage to property");
        createRequest.setClaimAmount(50000.0);
        createRequest.setIncidentDate(LocalDate.now());

        updateStatusRequest = new UpdateClaimStatusRequest();
        updateStatusRequest.setStatus(Claim.ClaimStatus.UNDER_REVIEW);

        when(claimService.getEstimatedLoss(any())).thenReturn(0.0);
        when(claimService.getCalculatedDeductible(any())).thenReturn(0.0);
        when(claimService.getCalculatedDepreciation(any())).thenReturn(0.0);
    }

    @Test
    @DisplayName("POST /api/claims - Success (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void createClaim_Success() throws Exception {
        when(claimService.createClaim(eq("customer"), any(CreateClaimRequest.class))).thenReturn(claim);

        mockMvc.perform(post("/api/claims")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(1))
                .andExpect(jsonPath("$.description").value("Fire damage to property"))
                .andExpect(jsonPath("$.claimAmount").value(50000.0))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        verify(claimService, times(1)).createClaim(eq("customer"), any(CreateClaimRequest.class));
    }

    @Test
    @DisplayName("POST /api/claims - Forbidden (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createClaim_Forbidden() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)));

        verify(claimService, never()).createClaim(anyString(), any(CreateClaimRequest.class));
    }

    @Test
    @DisplayName("POST /api/claims - Unauthorized Subscription")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void createClaim_UnauthorizedSubscription() throws Exception {
        when(claimService.createClaim(eq("customer"), any(CreateClaimRequest.class)))
                .thenThrow(new RuntimeException("Unauthorized: Subscription does not belong to this customer"));

        mockMvc.perform(post("/api/claims")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Unauthorized: Subscription does not belong to this customer"));

        verify(claimService, times(1)).createClaim(eq("customer"), any(CreateClaimRequest.class));
    }

    @Test
    @DisplayName("GET /api/claims/me - Success (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getMyClaims_Success() throws Exception {
        List<Claim> claims = Arrays.asList(claim);
        when(claimService.getClaimsByUsername("customer")).thenReturn(claims);

        mockMvc.perform(get("/api/claims/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimId").value(1))
                .andExpect(jsonPath("$[0].description").value("Fire damage to property"));

        verify(claimService, times(1)).getClaimsByUsername("customer");
    }

    @Test
    @DisplayName("GET /api/claims/me - Forbidden (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getMyClaims_Forbidden() throws Exception {
        mockMvc.perform(get("/api/claims/me"));

        verify(claimService, never()).getClaimsByUsername(anyString());
    }

    @Test
    @DisplayName("GET /api/claims - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllClaims_Success() throws Exception {
        List<Claim> claims = Arrays.asList(claim);
        when(claimService.getAllClaims()).thenReturn(claims);

        mockMvc.perform(get("/api/claims"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].claimId").value(1))
                .andExpect(jsonPath("$[0].claimAmount").value(50000.0));

        verify(claimService, times(1)).getAllClaims();
    }

    @Test
    @DisplayName("GET /api/claims - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getAllClaims_Forbidden() throws Exception {
        mockMvc.perform(get("/api/claims"));

        verify(claimService, never()).getAllClaims();
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/status - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateClaimStatus_SuccessAdmin() throws Exception {
        Claim updatedClaim = new Claim();
        updatedClaim.setClaimId(1L);
        updatedClaim.setSubscription(claim.getSubscription());
        updatedClaim.setStatus(Claim.ClaimStatus.UNDER_REVIEW);
        updatedClaim.setDescription("Fire damage to property");
        updatedClaim.setClaimAmount(50000.0);

        when(claimService.updateClaimStatus(eq(1L), eq(Claim.ClaimStatus.UNDER_REVIEW))).thenReturn(updatedClaim);

        mockMvc.perform(put("/api/claims/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(1))
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        verify(claimService, times(1)).updateClaimStatus(1L, Claim.ClaimStatus.UNDER_REVIEW);
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/status - Success (SURVEYOR)")
    @WithMockUser(username = "admin", roles = {"SURVEYOR"})
    void updateClaimStatus_SuccessSurveyor() throws Exception {
        Claim updatedClaim = new Claim();
        updatedClaim.setClaimId(1L);
        updatedClaim.setSubscription(claim.getSubscription());
        updatedClaim.setStatus(Claim.ClaimStatus.INSPECTING);

        when(claimService.updateClaimStatus(eq(1L), eq(Claim.ClaimStatus.INSPECTING))).thenReturn(updatedClaim);

        updateStatusRequest.setStatus(Claim.ClaimStatus.INSPECTING);

        mockMvc.perform(put("/api/claims/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INSPECTING"));

        verify(claimService, times(1)).updateClaimStatus(1L, Claim.ClaimStatus.INSPECTING);
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/status - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void updateClaimStatus_Forbidden() throws Exception {
        mockMvc.perform(put("/api/claims/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)));

        verify(claimService, never()).updateClaimStatus(anyLong(), any());
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/status - Not Found")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updateClaimStatus_NotFound() throws Exception {
        when(claimService.updateClaimStatus(eq(999L), any()))
                .thenThrow(new RuntimeException("Claim not found with ID: 999"));

        mockMvc.perform(put("/api/claims/999/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Claim not found with ID: 999"));

        verify(claimService, times(1)).updateClaimStatus(eq(999L), any());
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/approve - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void approveClaim_Success() throws Exception {
        Claim approvedClaim = new Claim();
        approvedClaim.setClaimId(1L);
        approvedClaim.setSubscription(claim.getSubscription());
        approvedClaim.setStatus(Claim.ClaimStatus.APPROVED);
        approvedClaim.setSettlementAmount(45000.0);

        when(claimService.approveClaim(1L)).thenReturn(approvedClaim);

        mockMvc.perform(put("/api/claims/1/approve")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.settlementAmount").value(45000.0));

        verify(claimService, times(1)).approveClaim(1L);
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/approve - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void approveClaim_Forbidden() throws Exception {
        mockMvc.perform(put("/api/claims/1/approve")
                        .with(csrf()));

        verify(claimService, never()).approveClaim(anyLong());
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/reject - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void rejectClaim_Success() throws Exception {
        Claim rejectedClaim = new Claim();
        rejectedClaim.setClaimId(1L);
        rejectedClaim.setSubscription(claim.getSubscription());
        rejectedClaim.setStatus(Claim.ClaimStatus.REJECTED);

        when(claimService.rejectClaim(1L)).thenReturn(rejectedClaim);

        mockMvc.perform(put("/api/claims/1/reject")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(1))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        verify(claimService, times(1)).rejectClaim(1L);
    }

    @Test
    @DisplayName("PUT /api/claims/{id}/reject - Forbidden (SURVEYOR)")
    @WithMockUser(username = "admin", roles = {"SURVEYOR"})
    void rejectClaim_Forbidden() throws Exception {
        mockMvc.perform(put("/api/claims/1/reject")
                        .with(csrf()));

        verify(claimService, never()).rejectClaim(anyLong());
    }

    @Test
    @DisplayName("GET /api/claims/{id} - Success (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getClaimById_SuccessCustomer() throws Exception {
        when(claimService.getClaimById(1L)).thenReturn(claim);

        mockMvc.perform(get("/api/claims/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(1))
                .andExpect(jsonPath("$.description").value("Fire damage to property"));

        verify(claimService, times(1)).getClaimById(1L);
    }

    @Test
    @DisplayName("GET /api/claims/{id} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getClaimById_SuccessAdmin() throws Exception {
        when(claimService.getClaimById(1L)).thenReturn(claim);

        mockMvc.perform(get("/api/claims/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(1));

        verify(claimService, times(1)).getClaimById(1L);
    }

    @Test
    @DisplayName("GET /api/claims/{id} - Success (SURVEYOR)")
    @WithMockUser(username = "admin", roles = {"SURVEYOR"})
    void getClaimById_SuccessSurveyor() throws Exception {
        when(claimService.getClaimById(1L)).thenReturn(claim);

        mockMvc.perform(get("/api/claims/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimId").value(1));

        verify(claimService, times(1)).getClaimById(1L);
    }

    @Test
    @DisplayName("GET /api/claims/{id} - Not Found")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getClaimById_NotFound() throws Exception {
        when(claimService.getClaimById(999L))
                .thenThrow(new RuntimeException("Claim not found with ID: 999"));

        mockMvc.perform(get("/api/claims/999"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Claim not found with ID: 999"));

        verify(claimService, times(1)).getClaimById(999L);
    }
}
