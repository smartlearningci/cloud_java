package pt.taskflow.tasks.api;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

/**
 * ErrorResponse
 * -------------
 * A minimal, uniform error payload returned to API clients.
 *
 * Fields:
 *   - timestamp: when the error occurred (ISO string)
 *   - status:    HTTP status code (e.g., 400, 500)
 *   - error:     HTTP reason phrase (e.g., "Bad Request", "Internal Server Error")
 *   - message:   human-friendly description (safe to expose)
 *   - path:      endpoint path that failed (e.g., "/tasks")
 *   - corrId:    correlation id injected by CorrelationIdFilter
 */
public record ErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        String corrId
) {}
