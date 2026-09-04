package com.bank.ft.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.ft.api.v1.controller.TransferController;
import com.bank.ft.api.v1.mapper.TransactionMapper;
import com.bank.ft.common.security.IdorGuard;
import com.bank.ft.common.security.keycloak.CurrentUserResolver;
import com.bank.ft.common.security.keycloak.KeycloakJwtAuthConverter;
import com.bank.ft.domain.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TransferController.class)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransferService transferService;

    @MockBean
    private TransactionMapper transactionMapper;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @MockBean
    private IdorGuard idorGuard;

    @MockBean
    private KeycloakJwtAuthConverter keycloakJwtAuthConverter;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getMyTransfersReturnsOkForAuthenticatedCustomer() throws Exception {
        when(currentUserResolver.currentCif()).thenReturn("9999999999");
        when(transferService.getTransactionsForCif(any())).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/transfers"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyTransfersRejectsUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/transfers"))
                .andExpect(status().isUnauthorized());
    }
}
