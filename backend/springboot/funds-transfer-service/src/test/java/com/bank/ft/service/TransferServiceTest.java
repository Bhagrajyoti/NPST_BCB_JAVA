package com.bank.ft.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bank.ft.api.v1.dto.request.InitiateTransferRequest;
import com.bank.ft.client.feign.CbsFeignClient;
import com.bank.ft.client.feign.SwitchFeignClient;
import com.bank.ft.domain.entity.Transaction;
import com.bank.ft.domain.repository.TransactionRepository;
import com.bank.ft.domain.service.TransferServiceImpl;
import com.bank.ft.domain.statemachine.TransactionStateMachine;
import com.bank.ft.domain.statemachine.TransactionStatus;
import com.bank.ft.messaging.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private CbsFeignClient cbsFeignClient;

    @Mock
    private SwitchFeignClient switchFeignClient;

    private TransferServiceImpl transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(
                transactionRepository,
                outboxEventRepository,
                new TransactionStateMachine(),
                cbsFeignClient,
                switchFeignClient,
                new ObjectMapper());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private InitiateTransferRequest sampleRequest() {
        return new InitiateTransferRequest(
                null, "1234567890123", "HDFC0001234", BigInteger.valueOf(50000), "INR", "IMPS", "test transfer");
    }

    @Test
    void successfulTransferReachesSuccessStatus() {
        when(cbsFeignClient.debit(anyString(), any(BigInteger.class)))
                .thenReturn(new CbsFeignClient.DebitResult(true, "CBS-REF-1", null));
        when(switchFeignClient.submitTransfer(any()))
                .thenReturn(new SwitchFeignClient.SwitchTransferResult(true, "SWITCH-REF-1", null));

        Transaction result = transferService.initiateTransfer(
                sampleRequest(), "9999999999", UUID.randomUUID(), "default-bank", "idem-key-1");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(result.getCbsReferenceNumber()).isEqualTo("CBS-REF-1");
    }

    @Test
    void cbsDebitFailureMarksTransactionFailedWithoutCallingSwitch() {
        when(cbsFeignClient.debit(anyString(), any(BigInteger.class)))
                .thenReturn(new CbsFeignClient.DebitResult(false, null, "INSUFFICIENT_FUNDS"));

        Transaction result = transferService.initiateTransfer(
                sampleRequest(), "9999999999", UUID.randomUUID(), "default-bank", "idem-key-2");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    @Test
    void switchRejectionMarksTransactionFailedAfterSuccessfulDebit() {
        when(cbsFeignClient.debit(anyString(), any(BigInteger.class)))
                .thenReturn(new CbsFeignClient.DebitResult(true, "CBS-REF-2", null));
        when(switchFeignClient.submitTransfer(any()))
                .thenReturn(new SwitchFeignClient.SwitchTransferResult(false, null, "SWITCH_TIMEOUT"));

        Transaction result = transferService.initiateTransfer(
                sampleRequest(), "9999999999", UUID.randomUUID(), "default-bank", "idem-key-3");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("SWITCH_TIMEOUT");
    }
}
