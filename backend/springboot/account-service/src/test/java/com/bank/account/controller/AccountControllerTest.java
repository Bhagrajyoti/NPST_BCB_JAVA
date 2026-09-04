package com.bank.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.account.api.v1.controller.AccountController;
import com.bank.account.api.v1.mapper.AccountMapper;
import com.bank.account.common.security.IdorGuard;
import com.bank.account.common.security.keycloak.CurrentUserResolver;
import com.bank.account.common.security.keycloak.KeycloakJwtAuthConverter;
import com.bank.account.domain.entity.Account;
import com.bank.account.domain.service.AccountService;
import com.bank.account.domain.statemachine.AccountStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @MockBean
    private AccountMapper accountMapper;

    @MockBean
    private CurrentUserResolver currentUserResolver;

    @MockBean
    private IdorGuard idorGuard;

    @MockBean
    private KeycloakJwtAuthConverter keycloakJwtAuthConverter;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getMyAccountsReturnsOkForAuthenticatedCustomer() throws Exception {
        when(currentUserResolver.currentCif()).thenReturn("1234567890");
        when(accountService.getAccountsForCif("1234567890")).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isOk());
    }

    @Test
    void getMyAccountsRejectsUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createAccountReturnsOkForValidRequest() throws Exception {
        Account created = Account.builder()
                .id(UUID.randomUUID())
                .cif("1234567890")
                .accountType("SAVINGS")
                .balanceMinorUnits(BigInteger.ZERO)
                .currency("INR")
                .status(AccountStatus.PENDING_ACTIVATION)
                .build();
        when(accountService.createAccount(any(), any(), any())).thenReturn(created);

        String body = """
                {"cif":"1234567890","accountType":"SAVINGS","currency":"INR"}
                """;

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }
}
