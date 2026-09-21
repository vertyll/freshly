package com.vertyll.freshly.airquality.application.service.query;

import java.util.List;
import java.util.Map;

import com.vertyll.freshly.airquality.application.dto.MeasurementResponse;
import com.vertyll.freshly.airquality.application.dto.NearbyStationResponse;
import com.vertyll.freshly.airquality.application.dto.StationRankingResponse;
import com.vertyll.freshly.airquality.application.dto.StatisticsResponse;
import com.vertyll.freshly.airquality.application.port.inbound.query.AirQualityQueryUseCase;
import com.vertyll.freshly.airquality.application.port.outbound.AirQualityAnalyticsPort;
import com.vertyll.freshly.airquality.domain.error.AirQualityError;
import com.vertyll.freshly.airquality.domain.model.AnalysisWindow;
import com.vertyll.freshly.airquality.domain.model.Coordinates;
import com.vertyll.freshly.airquality.domain.model.RankingLimit;
import com.vertyll.freshly.airquality.domain.model.SearchRadius;
import com.vertyll.freshly.airquality.domain.repository.AirQualityHistoryRepository;
import com.vertyll.freshly.airquality.domain.repository.StationCatalogue;
import com.vertyll.freshly.lang.error.DomainException;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PagedResponse;

public class AirQualityQueryService implements AirQualityQueryUseCase {
    private static final String STATION_ID = "stationId";

    private final AirQualityHistoryRepository history;
    private final AirQualityAnalyticsPort analytics;
    private final StationCatalogue stations;

    public AirQualityQueryService(
        AirQualityHistoryRepository history,
        AirQualityAnalyticsPort analytics,
        StationCatalogue stations
    ) {
        this.history = history;
        this.analytics = analytics;
        this.stations = stations;
    }

    @Override
    public MeasurementResponse currentFor(int stationId) {
        return history.findLatestByStationId(stationId)
            .map(MeasurementResponse::from)
            .orElseThrow(() -> new DomainException(AirQualityError.DATA_NOT_FOUND, Map.of(STATION_ID, stationId)));
    }

    @Override
    public PagedResponse<MeasurementResponse> historyFor(int stationId, AnalysisWindow window, PageRequest page) {
        requireStation(stationId);
        return PagedResponse.from(history.findByStation(stationId, window, page)).map(MeasurementResponse::from);
    }

    @Override
    public StatisticsResponse statisticsFor(int stationId, AnalysisWindow window) {
        requireStation(stationId);

        return analytics.statisticsFor(stationId, window)
            .map(StatisticsResponse::from)
            .orElseThrow(() -> new DomainException(AirQualityError.DATA_NOT_FOUND, Map.of(STATION_ID, stationId)));
    }

    @Override
    public List<StationRankingResponse> ranking(AnalysisWindow window, RankingLimit limit) {
        return analytics.ranking(window, limit).stream().map(StationRankingResponse::from).toList();
    }

    @Override
    public List<NearbyStationResponse> stationsNear(Coordinates point, SearchRadius radius) {
        return stations.findWithin(point, radius).stream().map(NearbyStationResponse::from).toList();
    }

    private void requireStation(int stationId) {
        if (stations.findById(stationId).isEmpty()) {
            throw new DomainException(AirQualityError.STATION_NOT_FOUND, Map.of(STATION_ID, stationId));
        }
    }
}
