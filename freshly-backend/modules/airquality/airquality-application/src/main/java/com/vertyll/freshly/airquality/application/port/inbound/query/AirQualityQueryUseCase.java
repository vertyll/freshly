package com.vertyll.freshly.airquality.application.port.inbound.query;

import java.util.List;

import com.vertyll.freshly.airquality.application.dto.MeasurementResponse;
import com.vertyll.freshly.airquality.application.dto.NearbyStationResponse;
import com.vertyll.freshly.airquality.application.dto.StationRankingResponse;
import com.vertyll.freshly.airquality.application.dto.StatisticsResponse;
import com.vertyll.freshly.airquality.domain.model.AnalysisWindow;
import com.vertyll.freshly.airquality.domain.model.Coordinates;
import com.vertyll.freshly.airquality.domain.model.RankingLimit;
import com.vertyll.freshly.airquality.domain.model.SearchRadius;
import com.vertyll.freshly.lang.page.PageRequest;
import com.vertyll.freshly.lang.page.PagedResponse;

public interface AirQualityQueryUseCase {
    MeasurementResponse currentFor(int stationId);

    PagedResponse<MeasurementResponse> historyFor(int stationId, AnalysisWindow window, PageRequest page);

    StatisticsResponse statisticsFor(int stationId, AnalysisWindow window);

    List<StationRankingResponse> ranking(AnalysisWindow window, RankingLimit limit);

    List<NearbyStationResponse> stationsNear(Coordinates point, SearchRadius radius);
}
