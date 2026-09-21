package com.vertyll.freshly.airquality.domain.model;

public enum AirQualityLevel {
    VERY_GOOD(0),
    GOOD(1),
    MODERATE(2),
    SUFFICIENT(3),
    BAD(4),
    VERY_BAD(5);

    private final int severity;

    AirQualityLevel(int severity) {
        this.severity = severity;
    }

    public int severity() {
        return severity;
    }

    public boolean isGood() {
        return this == VERY_GOOD || this == GOOD;
    }

    public boolean isWorseThan(AirQualityLevel other) {
        return severity > other.severity;
    }
}
