package com.example.mpesa.idempotency;

import com.example.mpesa.exception.ApiException;
import com.example.mpesa.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class IdempotencyService {

    private static final int MAX_KEY_LENGTH = 64;
    private static final int RETENTION_HOURS = 24;

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    public void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key header is required");
        }
        if (key.length() > MAX_KEY_LENGTH) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key max length is 64");
        }
    }

    public String hashRequest(Object body) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(body);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash request", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findValid(String key) {
        return idempotencyRepository.findById(key)
                .filter(record -> record.getCreatedAt().isAfter(Instant.now().minus(RETENTION_HOURS, ChronoUnit.HOURS)));
    }

    public void assertSameBodyOrConflict(IdempotencyRecord existing, String requestHash) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new ApiException(
                    ErrorCode.IDEMPOTENCY_CONFLICT,
                    "Idempotency-Key was reused with a different request body"
            );
        }
    }

    @Transactional
    public void save(String key, String requestHash, int status, Object responseBody) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setRequestHash(requestHash);
        record.setResponseStatus(status);
        record.setResponseBody(objectMapper.writeValueAsString(responseBody));
        record.setCreatedAt(Instant.now());
        idempotencyRepository.save(record);
    }

    public <T> T readBody(IdempotencyRecord record, Class<T> type) {
        return objectMapper.readValue(record.getResponseBody().getBytes(StandardCharsets.UTF_8), type);
    }
}
