package com.vertyll.freshly.airquality.domain;

import lombok.Getter;

/**
 * Enum representing air quality levels as defined by GIOŚ (Polish Air Quality Monitoring System).
 * This enum standardizes air quality level handling across the application. MongoDB stores the enum
 * name (e.g., "VERY_GOOD"), and the API returns the same. The polishName is only used for parsing
 * data from GIOŚ API.
 */
public enum AirQualityLevel {
    VERY_GOOD("Bardzo dobry", 0),
    GOOD("Dobry", 1),
    MODERATE("Umiarkowany", 2),
    SUFFICIENT("Dostateczny", 3),
    BAD("Zły", 4),
    VERY_BAD("Bardzo zły", 5);

    private final String polishName;

    /**
     * -- GETTER -- Returns severity level (0 = best, 5 = worst). Useful for comparisons and
     * calculations.
     */
    @Getter private final int severity;

    AirQualityLevel(String polishName, int severity) {
        this.polishName = polishName;
        this.severity = severity;
    }

    /**
     * Returns the API value (enum name) to be sent to frontend. Frontend should use this value for
     * translations.
     */
    public String toApiValue() {
        return this.name();
    }

    /**
     * Parses Polish name from GIOŚ API to enum value. Returns null if the name is not recognized.
     * This is only used when receiving data from external GIOŚ API.
     */
    public static AirQualityLevel fromPolishName(String polishName) {
        if (polishName == null) {
            return null;
        }
        for (AirQualityLevel level : values()) {
            if (level.polishName.equalsIgnoreCase(polishName)) {
                return level;
            }
        }
        return null;
    }

    /** Checks if this level represents good air quality (Very Good or Good). */
    public boolean isGood() {
        return this == VERY_GOOD || this == GOOD;
    }

    /** Checks if this level is better than the specified level. */
    public boolean isBetterThan(AirQualityLevel other) {
        return this.severity < other.severity;
    }

    /** Checks if this level is worse than the specified level. */
    public boolean isWorseThan(AirQualityLevel other) {
        return this.severity > other.severity;
    }
}
