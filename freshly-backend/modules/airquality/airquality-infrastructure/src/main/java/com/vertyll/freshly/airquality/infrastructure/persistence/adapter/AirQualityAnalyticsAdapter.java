package com.vertyll.freshly.airquality.infrastructure.persistence.adapter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.vertyll.freshly.airquality.application.port.outbound.AirQualityAnalyticsPort;
import com.vertyll.freshly.airquality.domain.model.AirQualityLevel;
import com.vertyll.freshly.airquality.domain.model.AirQualityStatistics;
import com.vertyll.freshly.airquality.domain.model.AnalysisWindow;
import com.vertyll.freshly.airquality.domain.model.Pollutant;
import com.vertyll.freshly.airquality.domain.model.RankingLimit;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.airquality.domain.model.StationRanking;
import com.vertyll.freshly.airquality.domain.repository.StationCatalogue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AirQualityAnalyticsAdapter implements AirQualityAnalyticsPort {
    private static final String COLLECTION = "airquality_measurement";
    private static final String STATION_ID = "station_id";
    private static final String MEASURED_AT = "measured_at";
    private static final String OVERALL_LEVEL = "overall_level";
    private static final String READINGS = "readings.";
    private static final String COUNT = "count";
    private static final String AVERAGE_SCORE = "averageScore";

    private final MongoTemplate mongoTemplate;
    private final StationCatalogue stations;

    @Override
    public Optional<AirQualityStatistics> statisticsFor(int stationId, AnalysisWindow window) {
        Criteria inWindow =
                Criteria.where(STATION_ID).is(stationId).and(MEASURED_AT).gte(window.from()).lte(window.to());

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(inWindow),
            Aggregation.group()
                .count()
                .as(COUNT)
                .first("station_name")
                .as("stationName")
                .avg(READINGS + Pollutant.PM10.name())
                .as("pm10Avg")
                .min(READINGS + Pollutant.PM10.name())
                .as("pm10Min")
                .max(READINGS + Pollutant.PM10.name())
                .as("pm10Max")
                .avg(READINGS + Pollutant.PM25.name())
                .as("pm25Avg")
                .min(READINGS + Pollutant.PM25.name())
                .as("pm25Min")
                .max(READINGS + Pollutant.PM25.name())
                .as("pm25Max")
                .avg(READINGS + Pollutant.SO2.name())
                .as("so2Avg")
                .avg(READINGS + Pollutant.NO2.name())
                .as("no2Avg")
                .avg(READINGS + Pollutant.CO.name())
                .as("coAvg")
                .avg(READINGS + Pollutant.O3.name())
                .as("o3Avg")
        );

        AggregationResults<StatisticsRow> results =
                mongoTemplate.aggregate(aggregation, COLLECTION, StatisticsRow.class);

        StatisticsRow row = results.getUniqueMappedResult();
        if (row == null || row.count() == 0) {
            return Optional.empty();
        }

        return Optional.of(
            new AirQualityStatistics(
                stationId,
                row.stationName(),
                window,
                row.count(),
                summaries(row),
                levelDistribution(stationId, window)
            )
        );
    }

    @Override
    public List<StationRanking> ranking(AnalysisWindow window, RankingLimit limit) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where(MEASURED_AT).gte(window.from()).lte(window.to())),
            Aggregation.group(STATION_ID).count().as(COUNT).avg(READINGS + Pollutant.PM10.name()).as(AVERAGE_SCORE),
            Aggregation.match(Criteria.where(AVERAGE_SCORE).ne(null)),
            Aggregation.sort(Sort.Direction.ASC, AVERAGE_SCORE),
            Aggregation.limit(limit.value())
        );

        List<RankingRow> rows = mongoTemplate.aggregate(aggregation, COLLECTION, RankingRow.class).getMappedResults();

        Map<Integer, AirQualityLevel> dominant =
                dominantLevelsFor(rows.stream().map(RankingRow::id).collect(Collectors.toUnmodifiableSet()), window);

        List<StationRanking> ranking = new ArrayList<>(rows.size());
        int position = 1;

        for (RankingRow row : rows) {
            Optional<Station> station = stations.findById(row.id());
            if (station.isEmpty()) {
                continue;
            }
            ranking.add(
                new StationRanking(position, station.get(), row.averageScore(), dominant.get(row.id()), row.count())
            );
            position++;
        }

        return List.copyOf(ranking);
    }

    private Map<Pollutant, AirQualityStatistics.PollutantSummary> summaries(StatisticsRow row) {
        Map<Pollutant, AirQualityStatistics.PollutantSummary> summaries = new EnumMap<>(Pollutant.class);

        putIfMeasured(summaries, Pollutant.PM10, row.pm10Avg(), row.pm10Min(), row.pm10Max());
        putIfMeasured(summaries, Pollutant.PM25, row.pm25Avg(), row.pm25Min(), row.pm25Max());
        putIfMeasured(summaries, Pollutant.SO2, row.so2Avg(), null, null);
        putIfMeasured(summaries, Pollutant.NO2, row.no2Avg(), null, null);
        putIfMeasured(summaries, Pollutant.CO, row.coAvg(), null, null);
        putIfMeasured(summaries, Pollutant.O3, row.o3Avg(), null, null);

        return summaries;
    }

    private static void putIfMeasured(
        Map<Pollutant, AirQualityStatistics.PollutantSummary> target,
        Pollutant pollutant,
        @Nullable Double average,
        @Nullable Double minimum,
        @Nullable Double maximum
    ) {
        if (average != null) {
            target.put(pollutant, new AirQualityStatistics.PollutantSummary(average, minimum, maximum));
        }
    }

    private Map<AirQualityLevel, Integer> levelDistribution(int stationId, AnalysisWindow window) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where(STATION_ID)
                    .is(stationId)
                    .and(MEASURED_AT)
                    .gte(window.from())
                    .lte(window.to())
                    .and(OVERALL_LEVEL)
                    .ne(null)
            ),
            Aggregation.group(OVERALL_LEVEL).count().as(COUNT)
        );

        Map<AirQualityLevel, Integer> distribution = AirQualityStatistics.emptyDistribution();

        mongoTemplate.aggregate(aggregation, COLLECTION, LevelCountRow.class)
            .getMappedResults()
            .forEach(row -> distribution.put(AirQualityLevel.valueOf(row.id()), row.count()));

        return distribution;
    }

    private Map<Integer, AirQualityLevel> dominantLevelsFor(Set<Integer> stationIds, AnalysisWindow window) {
        if (stationIds.isEmpty()) {
            return Map.of();
        }

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(
                Criteria.where(STATION_ID)
                    .in(stationIds)
                    .and(MEASURED_AT)
                    .gte(window.from())
                    .lte(window.to())
                    .and(OVERALL_LEVEL)
                    .ne(null)
            ),
            Aggregation.group(STATION_ID, OVERALL_LEVEL).count().as(COUNT)
        );

        Map<Integer, Map.Entry<AirQualityLevel, Integer>> best = new HashMap<>();

        for (StationLevelCountRow row : mongoTemplate.aggregate(aggregation, COLLECTION, StationLevelCountRow.class)
            .getMappedResults()) {
            AirQualityLevel level = AirQualityLevel.valueOf(row.level());
            Map.Entry<AirQualityLevel, Integer> current = best.get(row.stationId());

            boolean better = current == null || row.count() > current.getValue()
                    || (row.count() == current.getValue() && level.isWorseThan(current.getKey()));

            if (better) {
                best.put(row.stationId(), Map.entry(level, row.count()));
            }
        }

        Map<Integer, AirQualityLevel> dominant = new HashMap<>();
        best.forEach((stationId, entry) -> dominant.put(stationId, entry.getKey()));
        return dominant;
    }

    @SuppressWarnings("java:S107")
    record StatisticsRow(
        int count,
        String stationName,
        @Nullable Double pm10Avg,
        @Nullable Double pm10Min,
        @Nullable Double pm10Max,
        @Nullable Double pm25Avg,
        @Nullable Double pm25Min,
        @Nullable Double pm25Max,
        @Nullable Double so2Avg,
        @Nullable Double no2Avg,
        @Nullable Double coAvg,
        @Nullable Double o3Avg
    ) {
    }

    record RankingRow(int id, int count, double averageScore) {
    }

    record LevelCountRow(String id, int count) {
    }

    record StationLevelCountRow(@Field("station_id") int stationId, @Field("overall_level") String level, int count) {
    }
}
