package com.example.mpesa.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String UNAVAILABLE_MESSAGE = "Service temporarily unavailable";
    private static final String INTERNAL_ERROR_MESSAGE = "Unexpected error";
    private static final String MALFORMED_REQUEST_MESSAGE = "Request is malformed or fails validation";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, ex.getMessage()));
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseUnavailable(Exception ex) {
        log.error("Database unavailable", ex);
        ErrorCode code = ErrorCode.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, UNAVAILABLE_MESSAGE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, INTERNAL_ERROR_MESSAGE));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (statusCode.is5xxServerError()) {
            log.error("Framework exception", ex);
        }
        return ResponseEntity.status(statusCode).headers(headers).body(frameworkError(statusCode));
    }

    private ErrorResponse frameworkError(HttpStatusCode statusCode) {
        if (statusCode.isSameCodeAs(HttpStatus.BAD_REQUEST)) {
            return ErrorResponse.of(ErrorCode.VALIDATION_FAILED, MALFORMED_REQUEST_MESSAGE);
        }
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String reason = status != null ? status.getReasonPhrase() : "Error";
        return new ErrorResponse(reason.toUpperCase().replace(' ', '_'), reason);
    }
}
