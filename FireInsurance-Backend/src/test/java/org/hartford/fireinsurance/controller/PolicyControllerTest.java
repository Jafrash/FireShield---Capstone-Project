package org.hartford.fireinsurance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hartford.fireinsurance.dto.CreatePolicyRequest;
import org.hartford.fireinsurance.dto.UpdatePolicyRequest;
import org.hartford.fireinsurance.model.Policy;
import org.hartford.fireinsurance.service.PolicyService;
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
class PolicyControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private PolicyService policyService;

    private ObjectMapper objectMapper;
    private Policy policy;
    private CreatePolicyRequest createRequest;
    private UpdatePolicyRequest updateRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();

        policy = new Policy();
        policy.setPolicyId(1L);
        policy.setPolicyName("Basic Fire Insurance");
        policy.setCoverageDetails("Covers fire damage up to policy limit");
        policy.setBasePremium(1000.0);
        policy.setMaxCoverageAmount(100000.0);
        policy.setDurationMonths(12);

        createRequest = new CreatePolicyRequest();
        createRequest.setPolicyName("Basic Fire Insurance");
        createRequest.setCoverageDetails("Covers fire damage up to policy limit");
        createRequest.setBasePremium(1000.0);
        createRequest.setMaxCoverageAmount(100000.0);
        createRequest.setDurationMonths(12);

        updateRequest = new UpdatePolicyRequest();
        updateRequest.setPolicyName("Updated Fire Insurance");
        updateRequest.setBasePremium(1200.0);
    }

    @Test
    @DisplayName("POST /api/policies - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createPolicy_Success() throws Exception {
        when(policyService.createPolicy(any(CreatePolicyRequest.class))).thenReturn(policy);

        mockMvc.perform(post("/api/policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(1))
                .andExpect(jsonPath("$.policyName").value("Basic Fire Insurance"))
                .andExpect(jsonPath("$.basePremium").value(1000.0))
                .andExpect(jsonPath("$.maxCoverageAmount").value(100000.0))
                .andExpect(jsonPath("$.durationMonths").value(12));

        verify(policyService, times(1)).createPolicy(any(CreatePolicyRequest.class));
    }

    @Test
    @DisplayName("POST /api/policies - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void createPolicy_Forbidden() throws Exception {
        mockMvc.perform(post("/api/policies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)));

        verify(policyService, never()).createPolicy(any(CreatePolicyRequest.class));
    }

    @Test
    @DisplayName("PUT /api/policies/{id} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updatePolicy_Success() throws Exception {
        Policy updatedPolicy = new Policy();
        updatedPolicy.setPolicyId(1L);
        updatedPolicy.setPolicyName("Updated Fire Insurance");
        updatedPolicy.setCoverageDetails("Covers fire damage up to policy limit");
        updatedPolicy.setBasePremium(1200.0);
        updatedPolicy.setMaxCoverageAmount(100000.0);
        updatedPolicy.setDurationMonths(12);

        when(policyService.updatePolicy(eq(1L), any(UpdatePolicyRequest.class))).thenReturn(updatedPolicy);

        mockMvc.perform(put("/api/policies/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(1))
                .andExpect(jsonPath("$.policyName").value("Updated Fire Insurance"))
                .andExpect(jsonPath("$.basePremium").value(1200.0));

        verify(policyService, times(1)).updatePolicy(eq(1L), any(UpdatePolicyRequest.class));
    }

    @Test
    @DisplayName("PUT /api/policies/{id} - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void updatePolicy_Forbidden() throws Exception {
        mockMvc.perform(put("/api/policies/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)));

        verify(policyService, never()).updatePolicy(anyLong(), any(UpdatePolicyRequest.class));
    }

    @Test
    @DisplayName("PUT /api/policies/{id} - Not Found (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void updatePolicy_NotFound() throws Exception {
        when(policyService.updatePolicy(eq(999L), any(UpdatePolicyRequest.class)))
                .thenThrow(new RuntimeException("Policy not found with ID: 999"));

        mockMvc.perform(put("/api/policies/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Policy not found with ID: 999"));

        verify(policyService, times(1)).updatePolicy(eq(999L), any(UpdatePolicyRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/policies/{id} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deletePolicy_Success() throws Exception {
        doNothing().when(policyService).deletePolicy(1L);

        mockMvc.perform(delete("/api/policies/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Policy deleted successfully"));

        verify(policyService, times(1)).deletePolicy(1L);
    }

    @Test
    @DisplayName("DELETE /api/policies/{id} - Forbidden (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void deletePolicy_Forbidden() throws Exception {
        mockMvc.perform(delete("/api/policies/1")
                        .with(csrf()));

        verify(policyService, never()).deletePolicy(anyLong());
    }

    @Test
    @DisplayName("DELETE /api/policies/{id} - Not Found (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deletePolicy_NotFound() throws Exception {
        doThrow(new RuntimeException("Policy not found with ID: 999"))
                .when(policyService).deletePolicy(999L);

        mockMvc.perform(delete("/api/policies/999")
                        .with(csrf()))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Policy not found with ID: 999"));

        verify(policyService, times(1)).deletePolicy(999L);
    }

    @Test
    @DisplayName("GET /api/policies - Success")
    @WithMockUser(username = "user", roles = {"CUSTOMER"})
    void getAllPolicies_Success() throws Exception {
        List<Policy> policies = Arrays.asList(policy);
        when(policyService.getAllPolicies()).thenReturn(policies);

        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyId").value(1))
                .andExpect(jsonPath("$[0].policyName").value("Basic Fire Insurance"))
                .andExpect(jsonPath("$[0].basePremium").value(1000.0));

        verify(policyService, times(1)).getAllPolicies();
    }

    @Test
    @DisplayName("GET /api/policies/me - Success (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getMyPolicies_SuccessCustomer() throws Exception {
        List<Policy> policies = Arrays.asList(policy);
        when(policyService.getAllPolicies()).thenReturn(policies);

        mockMvc.perform(get("/api/policies/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyId").value(1))
                .andExpect(jsonPath("$[0].policyName").value("Basic Fire Insurance"));

        verify(policyService, times(1)).getAllPolicies();
    }

    @Test
    @DisplayName("GET /api/policies/me - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getMyPolicies_SuccessAdmin() throws Exception {
        List<Policy> policies = Arrays.asList(policy);
        when(policyService.getAllPolicies()).thenReturn(policies);

        mockMvc.perform(get("/api/policies/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyId").value(1));

        verify(policyService, times(1)).getAllPolicies();
    }

    @Test
    @DisplayName("GET /api/policies/me - Forbidden (SURVEYOR)")
    @WithMockUser(username = "surveyor", roles = {"SURVEYOR"})
    void getMyPolicies_Forbidden() throws Exception {
        mockMvc.perform(get("/api/policies/me"));

        verify(policyService, never()).getAllPolicies();
    }

    @Test
    @DisplayName("GET /api/policies/{id} - Success (CUSTOMER)")
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void getPolicyById_SuccessCustomer() throws Exception {
        when(policyService.getPolicyById(1L)).thenReturn(policy);

        mockMvc.perform(get("/api/policies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(1))
                .andExpect(jsonPath("$.policyName").value("Basic Fire Insurance"))
                .andExpect(jsonPath("$.coverageDetails").value("Covers fire damage up to policy limit"));

        verify(policyService, times(1)).getPolicyById(1L);
    }

    @Test
    @DisplayName("GET /api/policies/{id} - Success (ADMIN)")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getPolicyById_SuccessAdmin() throws Exception {
        when(policyService.getPolicyById(1L)).thenReturn(policy);

        mockMvc.perform(get("/api/policies/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyId").value(1))
                .andExpect(jsonPath("$.policyName").value("Basic Fire Insurance"));

        verify(policyService, times(1)).getPolicyById(1L);
    }

    @Test
    @DisplayName("GET /api/policies/{id} - Not Found")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getPolicyById_NotFound() throws Exception {
        when(policyService.getPolicyById(999L))
                .thenThrow(new RuntimeException("Policy not found with ID: 999"));

        mockMvc.perform(get("/api/policies/999"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.message").value("Policy not found with ID: 999"));

        verify(policyService, times(1)).getPolicyById(999L);
    }
}
