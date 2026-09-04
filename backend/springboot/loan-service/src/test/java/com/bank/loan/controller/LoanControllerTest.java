package com.bank.loan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.loan.api.v1.controller.LoanController;
import com.bank.loan.api.v1.mapper.LoanMapper;
import com.bank.loan.common.security.IdorGuard;
import com.bank.loan.common.security.keycloak.CurrentUserResolver;
import com.bank.loan.common.security.keycloak.KeycloakJwtAuthConverter;
import com.bank.loan.domain.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LoanController.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @MockBean
    private LoanMapper loanMapper;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @MockBean
    private IdorGuard idorGuard;

    @MockBean
    private KeycloakJwtAuthConverter keycloakJwtAuthConverter;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getMyLoansReturnsOkForAuthenticatedCustomer() throws Exception {
        when(currentUserResolver.currentCif()).thenReturn("9999999999");
        when(loanService.getLoansForCif(any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/loans"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyLoansRejectsUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/loans"))
                .andExpect(status().isUnauthorized());
    }
}
