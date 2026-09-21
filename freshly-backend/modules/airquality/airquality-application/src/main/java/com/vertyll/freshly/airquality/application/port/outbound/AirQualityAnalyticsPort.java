package com.vertyll.freshly.airquality.application.port.outbound;

import java.util.List;
import java.util.Optional;

import com.vertyll.freshly.airquality.domain.model.AirQualityStatistics;
import com.vertyll.freshly.airquality.domain.model.AnalysisWindow;
import com.vertyll.freshly.airquality.domain.model.RankingLimit;
import com.vertyll.freshly.airquality.domain.model.StationRanking;

public interface AirQualityAnalyticsPort {
    Optional<AirQualityStatistics> statisticsFor(int stationId, AnalysisWindow window);

    List<StationRanking> ranking(AnalysisWindow window, RankingLimit limit);
}
