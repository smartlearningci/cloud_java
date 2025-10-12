package pt.taskflow.tasks.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

import static pt.taskflow.tasks.config.CorrelationIdFilter.MDC_CORR_ID;

/**
 * GlobalExceptionHandler
 * ----------------------
 * Purpose:
 *   - Transform exceptions into a consistent JSON error payload.
 *   - Keep messages safe and helpful (no stack traces or sensitive details).
 *
 * Recommendations:
 *   - In production, avoid returning internal details; log them instead.
 *   - Map domain-specific exceptions to appropriate HTTP statuses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        String message = ex.getBindingResult().getAllErrors().stream()
                .findFirst().map(e -> e.getDefaultMessage()).orElse("Validation error");
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest req) {
        String message = ex.getAllErrors().stream().findFirst()
                .map(e -> e.getDefaultMessage()).orElse("Binding error");
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleSpring(ErrorResponseException ex, HttpServletRequest req) {
        ProblemDetail pd = ex.getBody();
        String message = (pd != null && pd.getDetail() != null) ? pd.getDetail() : ex.getMessage();
        HttpStatus status = (ex.getStatusCode() instanceof HttpStatus http) ? http : HttpStatus.INTERNAL_SERVER_ERROR;
        return build(status, message, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        // Avoid exposing internals; log the exception if needed (not shown here).
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", req);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                MDC.get(MDC_CORR_ID) // populated by CorrelationIdFilter
        );
        return ResponseEntity.status(status).body(body);
    }
}
