package com.bank.termdeposit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.termdeposit.api.v1.controller.TermDepositController;
import com.bank.termdeposit.api.v1.mapper.TermDepositMapper;
import com.bank.termdeposit.common.security.IdorGuard;
import com.bank.termdeposit.common.security.keycloak.CurrentUserResolver;
import com.bank.termdeposit.common.security.keycloak.KeycloakJwtAuthConverter;
import com.bank.termdeposit.domain.service.TermDepositService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TermDepositController.class)
class TermDepositControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TermDepositService termDepositService;

    @MockBean
    private TermDepositMapper termDepositMapper;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @MockBean
    private IdorGuard idorGuard;

    @MockBean
    private KeycloakJwtAuthConverter keycloakJwtAuthConverter;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getMyDepositsReturnsOkForAuthenticatedCustomer() throws Exception {
        when(currentUserResolver.currentCif()).thenReturn("9999999999");
        when(termDepositService.getDepositsForCif(any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/term-deposits"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyDepositsRejectsUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/term-deposits"))
                .andExpect(status().isUnauthorized());
    }
}
