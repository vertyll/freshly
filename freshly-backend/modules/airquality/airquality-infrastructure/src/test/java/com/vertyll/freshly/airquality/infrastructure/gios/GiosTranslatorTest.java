package com.vertyll.freshly.airquality.infrastructure.gios;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.airquality.domain.model.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.model.AirQualityLevel;
import com.vertyll.freshly.airquality.domain.model.Pollutant;
import com.vertyll.freshly.airquality.domain.model.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.lang.error.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GiosTranslatorTest {
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Nested
    @DisplayName("index values")
    class Levels {
        @Test
        @DisplayName("map the 0-5 scale onto the domain's levels")
        void mapsTheScale() {
            assertThat(GiosTranslator.level(0)).isEqualTo(AirQualityLevel.VERY_GOOD);
            assertThat(GiosTranslator.level(1)).isEqualTo(AirQualityLevel.GOOD);
            assertThat(GiosTranslator.level(2)).isEqualTo(AirQualityLevel.MODERATE);
            assertThat(GiosTranslator.level(3)).isEqualTo(AirQualityLevel.SUFFICIENT);
            assertThat(GiosTranslator.level(4)).isEqualTo(AirQualityLevel.BAD);
            assertThat(GiosTranslator.level(5)).isEqualTo(AirQualityLevel.VERY_BAD);
        }

        @Test
        @DisplayName("read null and -1 as no level, which is how GIOŚ says it computed none")
        void readsAbsence() {
            assertThat(GiosTranslator.level(null)).isNull();
            assertThat(GiosTranslator.level(-1)).isNull();
        }

        @Test
        @DisplayName("refuse a value off the scale rather than reading it as no level")
        void refusesUnknownValue() {
            assertThatThrownBy(() -> GiosTranslator.level(6)).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(AirQualityError.PROVIDER_RESPONSE_INVALID);
        }
    }

    @Nested
    @DisplayName("an index")
    class Index {
        @Test
        @DisplayName("carries every pollutant's level and a zoned calculation time")
        void translatesIndex() {
            AirQualityIndex index =
                    GiosTranslator.toIndex(552, new GiosResponses.IndexDto("2025-07-04 14:35:10", 1, null, 0, 0, 0, 1))
                        .orElseThrow();

            assertThat(index.overall()).isEqualTo(AirQualityLevel.GOOD);
            assertThat(index.so2()).isNull();
            assertThat(index.o3()).isEqualTo(AirQualityLevel.GOOD);
            assertThat(index.calculatedAt())
                .isEqualTo(LocalDateTime.of(2025, 7, 4, 14, 35, 10).atZone(WARSAW).toInstant());
        }

        @Test
        @DisplayName("is absent when GIOŚ has not computed one")
        void absentWithoutCalculation() {
            assertThat(
                GiosTranslator.toIndex(552, new GiosResponses.IndexDto(null, null, null, null, null, null, null))
            ).isEmpty();
        }
    }

    @Nested
    @DisplayName("pollutant codes")
    class Pollutants {
        @Test
        @DisplayName("map GIOŚ's notation, PM2.5 included, onto the domain's enum")
        void mapsCodes() {
            assertThat(GiosTranslator.pollutantOf(new GiosResponses.SensorDto(1, "PM2.5"))).contains(Pollutant.PM25);
            assertThat(GiosTranslator.pollutantOf(new GiosResponses.SensorDto(1, "NO2"))).contains(Pollutant.NO2);
        }

        @Test
        @DisplayName("leave out what the domain does not chart")
        void skipsUncharted() {
            assertThat(GiosTranslator.pollutantOf(new GiosResponses.SensorDto(1, "NOx"))).isEmpty();
            assertThat(GiosTranslator.pollutantOf(new GiosResponses.SensorDto(1, "C6H6"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("readings")
    class Readings {
        @Test
        @DisplayName("resolve Warsaw wall-clock time, across the daylight-saving boundary")
        void resolvesZone() {
            SensorMeasurement measurement = GiosTranslator.toMeasurement(
                1,
                Pollutant.PM10,
                List.of(
                    new GiosResponses.ValueDto("2025-07-04 14:00:00", 12.0),
                    new GiosResponses.ValueDto("2025-01-04 14:00:00", null)
                )
            );

            assertThat(measurement.readings().getFirst().measuredAt())
                .isEqualTo(LocalDateTime.of(2025, 7, 4, 12, 0).atZone(ZoneId.of("UTC")).toInstant());
            assertThat(measurement.readings().get(1).measuredAt())
                .isEqualTo(LocalDateTime.of(2025, 1, 4, 13, 0).atZone(ZoneId.of("UTC")).toInstant());
            assertThat(measurement.readings().get(1).value()).isNull();
        }

        @Test
        @DisplayName("refuse a timestamp GIOŚ did not write in its own format")
        void refusesMalformedTimestamp() {
            assertThatThrownBy(() -> GiosTranslator.toInstant("04.07.2025 14:00")).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).error())
                .isEqualTo(AirQualityError.PROVIDER_RESPONSE_INVALID);
        }
    }

    @Nested
    @DisplayName("stations")
    class Stations {
        @Test
        @DisplayName("keep city and street, which the domain calls city and address")
        void translatesStation() {
            Station station = GiosTranslator
                .toStation(
                    new GiosResponses.StationDto(
                        552,
                        "Warszawa, ul. Kondratowicza",
                        "52.290864",
                        "21.042458",
                        "Warszawa",
                        "ul. Kondratowicza 8"
                    )
                )
                .orElseThrow();

            assertThat(station.city()).isEqualTo("Warszawa");
            assertThat(station.address()).isEqualTo("ul. Kondratowicza 8");
            assertThat(station.coordinates().longitude()).isEqualTo(21.042_458);
        }

        @Test
        @DisplayName("skip one without coordinates rather than failing the whole list")
        void skipsWithoutCoordinates() {
            assertThat(
                GiosTranslator.toStation(new GiosResponses.StationDto(1, "Bez współrzędnych", null, null, null, null))
            ).isEmpty();
        }
    }
}
