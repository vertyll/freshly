package com.vertyll.freshly.airquality.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SensorMeasurementResponseDto(
        int sensorId,
        String paramCode,
        String paramName,
        List<ReadingDto> readings
) {
    public record ReadingDto(
            LocalDateTime date,
            Double value
    ) {
    }
}
