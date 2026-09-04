package com.bank.ft.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bank.ft.common.exception.IdempotencyKeyReusedException;
import com.bank.ft.idempotency.IdempotencyAspect;
import com.bank.ft.idempotency.IdempotencyRecord;
import com.bank.ft.idempotency.IdempotencyRepository;
import com.bank.ft.idempotency.IdempotencyStatus;
import com.bank.ft.idempotency.Idempotent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Replays the exact scenario the DoD checklist calls for: a genuine retried
 * request with the same idempotency key must get back the original
 * response, not re-run the write; the same key with a different payload
 * must be rejected as a real error, not silently accepted.
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyReplayTest {

    private static final String HEADER = "Idempotency-Key";

    @Mock
    private IdempotencyRepository idempotencyRepository;

    private IdempotencyAspect idempotencyAspect;

    @BeforeEach
    void setUp() {
        idempotencyAspect = new IdempotencyAspect(idempotencyRepository, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void replaysStoredResponseForSameKeyAndSamePayload() throws Throwable {
        setIdempotencyKeyHeader("key-123");
        Object[] args = {"same-payload"};

        ProceedingJoinPoint joinPoint = mockJoinPointReturning(args, ResponseEntity.ok("fresh-response"));

        String requestHash = hashOf(args);
        IdempotencyRecord completed = IdempotencyRecord.builder()
                .idempotencyKey("key-123")
                .requestHash(requestHash)
                .status(IdempotencyStatus.COMPLETED)
                .httpStatus(200)
                .responseBody("\"stored-response\"")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(idempotencyRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(completed));

        Object result = idempotencyAspect.aroundIdempotentMethod(joinPoint, idempotent());

        assertThat(result).isInstanceOf(ResponseEntity.class);
        assertThat(((ResponseEntity<?>) result).getBody()).isEqualTo("stored-response");
    }

    @Test
    void rejectsSameKeyWithDifferentPayloadAsReuse() throws Throwable {
        setIdempotencyKeyHeader("key-123");
        Object[] args = {"different-payload"};

        ProceedingJoinPoint joinPoint = mockJoinPointReturning(args, ResponseEntity.ok("irrelevant"));

        IdempotencyRecord existing = IdempotencyRecord.builder()
                .idempotencyKey("key-123")
                .requestHash("hash-of-a-totally-different-original-payload")
                .status(IdempotencyStatus.COMPLETED)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(idempotencyRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> idempotencyAspect.aroundIdempotentMethod(joinPoint, idempotent()))
                .isInstanceOf(IdempotencyKeyReusedException.class);
    }

    @Test
    void rejectsConcurrentRequestWithSameKeyStillInProgress() throws Throwable {
        setIdempotencyKeyHeader("key-123");
        Object[] args = {"same-payload"};

        ProceedingJoinPoint joinPoint = mockJoinPointReturning(args, ResponseEntity.ok("irrelevant"));

        IdempotencyRecord inProgress = IdempotencyRecord.builder()
                .idempotencyKey("key-123")
                .requestHash(hashOf(args))
                .status(IdempotencyStatus.IN_PROGRESS)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(idempotencyRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(inProgress));

        assertThatThrownBy(() -> idempotencyAspect.aroundIdempotentMethod(joinPoint, idempotent()))
                .isInstanceOf(IdempotencyKeyReusedException.class);
    }

    private String hashOf(Object[] args) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(new ObjectMapper().writeValueAsString(args).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(hash);
    }

    private void setIdempotencyKeyHeader(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER, key);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private ProceedingJoinPoint mockJoinPointReturning(Object[] args, Object returnValue) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        doReturn(ResponseEntity.class).when(signature).getReturnType();
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(returnValue);
        return joinPoint;
    }

    private Idempotent idempotent() throws Exception {
        Method method = SampleController.class.getMethod("handle", String.class);
        return method.getAnnotation(Idempotent.class);
    }

    private static final class SampleController {
        @Idempotent
        public ResponseEntity<String> handle(String payload) {
            return ResponseEntity.ok(payload);
        }
    }
}
