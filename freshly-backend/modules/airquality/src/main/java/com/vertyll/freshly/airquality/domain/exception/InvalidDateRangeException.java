package com.vertyll.freshly.airquality.domain.exception;

import java.io.Serial;
import java.time.LocalDateTime;

public class InvalidDateRangeException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidDateRangeException(LocalDateTime from, LocalDateTime to) {
        super(String.format("Invalid date range: 'from' date (%s) must be before 'to' date (%s).", from, to));
    }

    public InvalidDateRangeException(String message) {
        super(message);
    }
}
