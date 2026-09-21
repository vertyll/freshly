package com.vertyll.freshly.airquality.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.lang.error.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CoordinatesTest {
    private static final Coordinates WARSAW = new Coordinates(52.2297, 21.0122);
    private static final Coordinates KRAKOW = new Coordinates(50.0647, 19.9450);

    @Test
    @DisplayName("computes a known great-circle distance")
    void computesKnownDistance() {
        assertThat(WARSAW.distanceTo(KRAKOW)).isCloseTo(252.0, within(3.0));
    }

    @Test
    @DisplayName("distance to itself is zero")
    void zeroToItself() {
        assertThat(WARSAW.distanceTo(WARSAW)).isCloseTo(0.0, within(0.0001));
    }

    @Test
    @DisplayName("is symmetric")
    void isSymmetric() {
        assertThat(WARSAW.distanceTo(KRAKOW)).isCloseTo(KRAKOW.distanceTo(WARSAW), within(0.0001));
    }

    @Test
    @DisplayName("refuses a latitude outside the globe")
    void refusesImpossibleLatitude() {
        assertThatThrownBy(() -> new Coordinates(200.0, 21.0)).extracting(e -> ((DomainException) e).error())
            .isEqualTo(AirQualityError.INVALID_COORDINATES);
    }

    @Test
    @DisplayName("accepts the poles and the antimeridian")
    void acceptsExtremes() {
        assertThat(new Coordinates(90.0, 180.0)).isNotNull();
        assertThat(new Coordinates(-90.0, -180.0)).isNotNull();
    }
}
