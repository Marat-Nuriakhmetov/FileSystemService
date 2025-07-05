package com.fos.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Represents the health status of the application and its components.
 * This class provides detailed information about the system's health,
 * including component-specific status and details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class HealthStatus {

    /**
     * Enumeration of possible health status values.
     */
    public enum Status {
        UP("UP"),
        DOWN("DOWN"),
        UNKNOWN("UNKNOWN");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private final Status status;
    private final Map<String, Object> details;
    private final String requestId;

    /**
     *  Returns the timestamp when this status was created.
     */
    @Getter
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private final Instant timestamp;

    /**
     * Creates a new HealthStatus instance with the specified status.
     *
     * @param status the overall health status
     */
    public HealthStatus(Status status, Map<String, Object> details, String requestId) {
        this.status = status;
        this.details = details;
        this.requestId = requestId;
        this.timestamp = Instant.now();
    }
}