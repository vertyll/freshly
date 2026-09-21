package com.vertyll.freshly.airquality.domain.model;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.lang.error.DomainException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalysisWindowTest {
    @Test
    @DisplayName("clamps an over-long request when asked leniently")
    void clampsLeniently() {
        AnalysisWindow window = AnalysisWindow.ofDays(500);

        assertThat(Duration.between(window.from(), window.to()).toDays()).isEqualTo(AnalysisWindow.MAX_DAYS);
    }

    @Test
    @DisplayName("refuses an over-long request when asked strictly")
    void refusesStrictly() {
        assertThatThrownBy(() -> AnalysisWindow.ofDaysStrict(500)).extracting(e -> ((DomainException) e).error())
            .isEqualTo(AirQualityError.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("refuses a window that ends before it starts")
    void refusesInvertedWindow() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> AnalysisWindow.between(now, now.minus(Duration.ofDays(1))))
            .extracting(e -> ((DomainException) e).error())
            .isEqualTo(AirQualityError.INVALID_DATE_RANGE);
    }
}
