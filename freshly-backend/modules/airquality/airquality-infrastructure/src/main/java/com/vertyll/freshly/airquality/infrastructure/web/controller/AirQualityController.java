package com.vertyll.freshly.airquality.infrastructure.web.controller;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vertyll.freshly.airquality.application.dto.MeasurementResponse;
import com.vertyll.freshly.airquality.application.dto.NearbyStationResponse;
import com.vertyll.freshly.airquality.application.dto.StationRankingResponse;
import com.vertyll.freshly.airquality.application.dto.StatisticsResponse;
import com.vertyll.freshly.airquality.application.dto.SyncReport;
import com.vertyll.freshly.airquality.application.port.inbound.command.AirQualitySyncUseCase;
import com.vertyll.freshly.airquality.application.port.inbound.query.AirQualityQueryUseCase;
import com.vertyll.freshly.airquality.application.security.AirQualityPermission;
import com.vertyll.freshly.airquality.domain.model.AnalysisWindow;
import com.vertyll.freshly.airquality.domain.model.Coordinates;
import com.vertyll.freshly.airquality.domain.model.RankingLimit;
import com.vertyll.freshly.airquality.domain.model.SearchRadius;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PagedResponse;
import com.vertyll.freshly.web.security.PublicEndpoint;
import com.vertyll.freshly.web.security.RequirePermission;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/air-quality")
@RequiredArgsConstructor
@Validated
public class AirQualityController {
    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "50";
    private static final String DEFAULT_DAYS = "7";
    private static final String PUBLIC_DATA = "government public-health data";
    private static final String DEFAULT_RADIUS = "10";
    private static final String DEFAULT_LIMIT = "10";

    private final AirQualityQueryUseCase queries;
    private final AirQualitySyncUseCase sync;

    @GetMapping("/stations/{stationId}/current")
    @PublicEndpoint(PUBLIC_DATA)
    public MeasurementResponse current(@PathVariable int stationId) {
        return queries.currentFor(stationId);
    }

    @GetMapping("/stations/{stationId}/history")
    @PublicEndpoint(PUBLIC_DATA)
    public PagedResponse<MeasurementResponse> history(
        @PathVariable int stationId,
        @RequestParam(defaultValue = DEFAULT_DAYS) @Min(1) @Max(90) int days,
        @RequestParam(defaultValue = DEFAULT_PAGE) int page,
        @RequestParam(defaultValue = DEFAULT_SIZE) int size
    ) {
        AnalysisWindow window = AnalysisWindow.ofDaysStrict(days);

        return queries.historyFor(stationId, window, new PageRequest(page, size));
    }

    @GetMapping("/stations/{stationId}/statistics")
    @PublicEndpoint(PUBLIC_DATA)
    public StatisticsResponse statistics(
        @PathVariable int stationId,
        @RequestParam(defaultValue = DEFAULT_DAYS) @Min(1) @Max(90) int days
    ) {
        return queries.statisticsFor(stationId, AnalysisWindow.ofDaysStrict(days));
    }

    @GetMapping("/ranking")
    @PublicEndpoint(PUBLIC_DATA)
    public List<StationRankingResponse> ranking(
        @RequestParam(defaultValue = DEFAULT_DAYS) @Min(1) @Max(90) int days,
        @RequestParam(defaultValue = DEFAULT_LIMIT) @Min(RankingLimit.MIN) @Max(RankingLimit.MAX) int limit
    ) {
        return queries.ranking(AnalysisWindow.ofDaysStrict(days), RankingLimit.of(limit));
    }

    @GetMapping("/stations/nearby")
    @PublicEndpoint(PUBLIC_DATA)
    public List<NearbyStationResponse> nearby(
        @RequestParam double latitude,
        @RequestParam double longitude,
        @RequestParam(defaultValue = DEFAULT_RADIUS) @DecimalMin("" + SearchRadius.MIN_KM) @DecimalMax(
            "" + SearchRadius.MAX_KM
        ) double radiusKm
    ) {
        return queries.stationsNear(new Coordinates(latitude, longitude), SearchRadius.of(radiusKm));
    }

    @PostMapping("/sync")
    @RequirePermission(AirQualityPermission.Values.AIRQUALITY_SYNC)
    public ResponseEntity<SyncReport> triggerSync() {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(sync.syncAll());
    }

    @PostMapping("/purge")
    @RequirePermission(AirQualityPermission.Values.AIRQUALITY_PURGE)
    public PurgeReport purge() {
        return new PurgeReport(sync.purgeExpired());
    }

    public record PurgeReport(long removed) {
    }
}
