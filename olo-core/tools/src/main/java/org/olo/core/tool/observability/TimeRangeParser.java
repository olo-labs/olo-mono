/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.observability;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Parses inclusive start/end instants from tool request arguments.
 */
final class TimeRangeParser {

    private TimeRangeParser() {
    }

    static TimeRange parseTimeRange(String startTime, String endTime) {
        if (startTime.isBlank() || endTime.isBlank()) {
            throw new IllegalArgumentException("startTime and endTime are required");
        }
        Instant start = parseInstant(startTime, "startTime");
        Instant end = parseInstant(endTime, "endTime");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("endTime must be on or after startTime");
        }
        return new TimeRange(start, end);
    }

    static Instant parseInstant(String raw, String fieldName) {
        String text = raw.trim();
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            // fall through to ISO local date-time
        }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid " + fieldName + " timestamp: " + raw, e);
        }
    }

    /** Inclusive time window used when filtering metrics and logs. */
    public record TimeRange(Instant start, Instant end) {
        boolean contains(Instant instant) {
            return !instant.isBefore(start) && !instant.isAfter(end);
        }
    }
}
