package com.vertyll.freshly.airquality.infrastructure.gios;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.airquality.domain.model.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.model.AirQualityLevel;
import com.vertyll.freshly.airquality.domain.model.Coordinates;
import com.vertyll.freshly.airquality.domain.model.Pollutant;
import com.vertyll.freshly.airquality.domain.model.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.lang.error.DomainException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
final class GiosTranslator {

    private static final Map<String, Pollutant> POLLUTANT_BY_CODE = Map.of(
        "PM10",
        Pollutant.PM10,
        "PM2.5",
        Pollutant.PM25,
        "SO2",
        Pollutant.SO2,
        "NO2",
        Pollutant.NO2,
        "CO",
        Pollutant.CO,
        "O3",
        Pollutant.O3
    );

    private static final Map<Integer, AirQualityLevel> LEVEL_BY_INDEX_VALUE = Map.of(
        0,
        AirQualityLevel.VERY_GOOD,
        1,
        AirQualityLevel.GOOD,
        2,
        AirQualityLevel.MODERATE,
        3,
        AirQualityLevel.SUFFICIENT,
        4,
        AirQualityLevel.BAD,
        5,
        AirQualityLevel.VERY_BAD
    );

    private static final int NO_INDEX = -1;

    private static final DateTimeFormatter GIOS_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId GIOS_ZONE = ZoneId.of("Europe/Warsaw");

    private GiosTranslator() {
    }

    static Optional<Station> toStation(GiosResponses.StationDto dto) {
        String latitude = dto.latitude();
        String longitude = dto.longitude();
        if (latitude == null || longitude == null) {
            log.warn("Skipping station {} with no coordinates", dto.id());
            return Optional.empty();
        }

        try {
            Coordinates coordinates = new Coordinates(Double.parseDouble(latitude), Double.parseDouble(longitude));
            return Optional.of(new Station(dto.id(), dto.name(), dto.city(), dto.street(), coordinates));
        } catch (NumberFormatException | DomainException e) {
            log.warn("Skipping station {} with unusable coordinates", dto.id(), e);
            return Optional.empty();
        }
    }

    static Optional<Pollutant> pollutantOf(GiosResponses.SensorDto sensor) {
        return Optional.ofNullable(POLLUTANT_BY_CODE.get(sensor.pollutantCode()));
    }

    static SensorMeasurement toMeasurement(int sensorId, Pollutant pollutant, List<GiosResponses.ValueDto> values) {
        List<SensorMeasurement.Reading> readings = values.stream()
            .map(value -> new SensorMeasurement.Reading(toInstant(value.date()), value.value()))
            .toList();
        return new SensorMeasurement(sensorId, pollutant, readings);
    }

    static Optional<AirQualityIndex> toIndex(int stationId, GiosResponses.IndexDto dto) {
        String calculatedAt = dto.calculatedAt();
        if (calculatedAt == null) {
            return Optional.empty();
        }

        return Optional.of(
            new AirQualityIndex(
                stationId,
                toInstant(calculatedAt),
                level(dto.overall()),
                level(dto.so2()),
                level(dto.no2()),
                level(dto.pm10()),
                level(dto.pm25()),
                level(dto.o3())
            )
        );
    }

    @Nullable static AirQualityLevel level(@Nullable Integer indexValue) {
        if (indexValue == null || indexValue == NO_INDEX) {
            return null;
        }
        AirQualityLevel level = LEVEL_BY_INDEX_VALUE.get(indexValue);
        if (level == null) {
            throw new DomainException(AirQualityError.PROVIDER_RESPONSE_INVALID, Map.of("value", indexValue));
        }
        return level;
    }

    static Instant toInstant(String raw) {
        try {
            return LocalDateTime.parse(raw.strip(), GIOS_TIMESTAMP).atZone(GIOS_ZONE).toInstant();
        } catch (DateTimeParseException e) {
            throw new DomainException(AirQualityError.PROVIDER_RESPONSE_INVALID, Map.of("value", raw), e);
        }
    }
}
