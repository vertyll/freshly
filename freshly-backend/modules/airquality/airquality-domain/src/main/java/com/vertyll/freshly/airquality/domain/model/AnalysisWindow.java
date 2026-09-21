package com.vertyll.freshly.airquality.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.lang.error.DomainException;

public record AnalysisWindow(Instant from, Instant to) {
    public static final int MIN_DAYS = 1;
    public static final int MAX_DAYS = 90;
    public static final int DEFAULT_DAYS = 7;

    public AnalysisWindow {
        if (!from.isBefore(to)) {
            throw new DomainException(
                AirQualityError.INVALID_DATE_RANGE,
                Map.of("from", from.toString(), "to", to.toString())
            );
        }
    }

    public static AnalysisWindow ofDays(int daysBack) {
        int days = Math.clamp(daysBack, MIN_DAYS, MAX_DAYS);
        Instant now = Instant.now();
        return new AnalysisWindow(now.minus(Duration.ofDays(days)), now);
    }

    public static AnalysisWindow ofDaysStrict(int daysBack) {
        if (daysBack < MIN_DAYS || daysBack > MAX_DAYS) {
            throw new DomainException(
                AirQualityError.INVALID_DATE_RANGE,
                Map.of("days", daysBack, "min", MIN_DAYS, "max", MAX_DAYS)
            );
        }
        return ofDays(daysBack);
    }

    public static AnalysisWindow between(Instant from, Instant to) {
        return new AnalysisWindow(from, to);
    }
}
