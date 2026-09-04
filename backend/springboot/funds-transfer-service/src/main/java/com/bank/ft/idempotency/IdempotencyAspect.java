package com.bank.ft.idempotency;

import com.bank.ft.common.exception.IdempotencyKeyReusedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Runs BEFORE the controller method's business logic executes — checks a
 * real stored {@link IdempotencyRecord}, not a format validator. Applies
 * only to methods annotated {@link Idempotent}.
 *
 * <p>Header name/format is an open cross-stack contract item (architecture
 * doc §6) — confirmed value goes in {@code idempotency.header-name}.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_HEADER = "Idempotency-Key";

    @Around("@annotation(idempotent)")
    public Object aroundIdempotentMethod(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = currentRequestHeader(DEFAULT_HEADER);
        if (key == null || key.isBlank()) {
            // No key supplied: proceed without idempotency protection. Whether this
            // should instead be a hard 400 is a per-endpoint policy decision.
            return joinPoint.proceed();
        }

        String requestHash = hashArgs(joinPoint.getArgs());

        var existing = idempotencyRepository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReusedException(
                        "Idempotency key reused with a different request payload: " + key);
            }
            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                return replay(record, joinPoint);
            }
            if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                throw new IdempotencyKeyReusedException("Request with this idempotency key is already in progress: " + key);
            }
            // FAILED: fall through and retry the underlying operation for the same key/hash.
        }

        IdempotencyRecord inProgress = existing.orElseGet(() -> IdempotencyRecord.builder()
                .idempotencyKey(key)
                .requestHash(requestHash)
                .status(IdempotencyStatus.IN_PROGRESS)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(idempotent.ttlHours(), ChronoUnit.HOURS))
                .build());
        inProgress.setStatus(IdempotencyStatus.IN_PROGRESS);
        idempotencyRepository.save(inProgress);

        try {
            Object result = joinPoint.proceed();
            persistOutcome(inProgress, IdempotencyStatus.COMPLETED, result);
            return result;
        } catch (Throwable ex) {
            persistOutcome(inProgress, IdempotencyStatus.FAILED, null);
            throw ex;
        }
    }

    private Object replay(IdempotencyRecord record, ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Replaying stored response for idempotency key {}", record.getIdempotencyKey());
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (!ResponseEntity.class.isAssignableFrom(signature.getReturnType())) {
            // Aspect only supports transparent replay for ResponseEntity<?> controller methods.
            return joinPoint.proceed();
        }
        Object body = record.getResponseBody() == null
                ? null
                : objectMapper.readValue(record.getResponseBody(), Object.class);
        int status = record.getHttpStatus() != null ? record.getHttpStatus() : 200;
        return ResponseEntity.status(status).body(body);
    }

    private void persistOutcome(IdempotencyRecord record, IdempotencyStatus status, Object result) {
        record.setStatus(status);
        if (result instanceof ResponseEntity<?> responseEntity) {
            record.setHttpStatus(responseEntity.getStatusCode().value());
            try {
                record.setResponseBody(objectMapper.writeValueAsString(responseEntity.getBody()));
            } catch (Exception e) {
                log.warn("Failed to serialize response body for idempotency record {}", record.getIdempotencyKey(), e);
            }
        }
        idempotencyRepository.save(record);
    }

    private String hashArgs(Object[] args) {
        try {
            String serialized = objectMapper.writeValueAsString(args);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(serialized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash request for idempotency check", e);
        }
    }

    private String currentRequestHeader(String headerName) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        return attrs.getRequest().getHeader(headerName);
    }
}
