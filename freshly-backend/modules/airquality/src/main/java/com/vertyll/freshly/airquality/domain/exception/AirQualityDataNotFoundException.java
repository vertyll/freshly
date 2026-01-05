package com.vertyll.freshly.airquality.domain.exception;

import java.io.Serial;

public class AirQualityDataNotFoundException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    public AirQualityDataNotFoundException(int stationId) {
        super(String.format("No air quality data found for station with ID %d.", stationId));
    }

    public AirQualityDataNotFoundException(String message) {
        super(message);
    }
}
