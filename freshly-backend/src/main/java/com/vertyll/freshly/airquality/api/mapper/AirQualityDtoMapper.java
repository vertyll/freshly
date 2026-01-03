package com.vertyll.freshly.airquality.api.mapper;

import com.vertyll.freshly.airquality.api.dto.*;
import com.vertyll.freshly.airquality.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AirQualityDtoMapper {

    // Station mappings
    StationResponseDto toStationResponse(Station station);

    List<StationResponseDto> toStationResponseList(List<Station> stations);

    // AirQualityIndex mappings
    @Mapping(target = "overallIndex", source = "stIndexLevel")
    @Mapping(target = "so2Index", source = "so2IndexLevel")
    @Mapping(target = "no2Index", source = "no2IndexLevel")
    @Mapping(target = "pm10Index", source = "pm10IndexLevel")
    AirQualityIndexResponseDto toAirQualityIndexResponse(AirQualityIndex index);

    // SensorMeasurement mappings
    SensorMeasurementResponseDto toSensorMeasurementResponse(SensorMeasurement measurement);

    List<SensorMeasurementResponseDto> toSensorMeasurementResponseList(List<SensorMeasurement> measurements);

    SensorMeasurementResponseDto.ReadingDto toReadingDto(SensorMeasurement.Reading reading);

    // AirQualityMeasurement mappings
    AirQualityMeasurementResponseDto toAirQualityMeasurementResponse(AirQualityMeasurement measurement);

    List<AirQualityMeasurementResponseDto> toAirQualityMeasurementResponseList(List<AirQualityMeasurement> measurements);

    // StationDistance mappings
    StationDistanceResponseDto toStationDistanceResponse(StationDistance stationDistance);

    List<StationDistanceResponseDto> toStationDistanceResponseList(List<StationDistance> stationDistances);

    // Statistics mappings
    @Mapping(target = "pm10", expression = "java(mapPm10Stats(stats))")
    @Mapping(target = "pm25", expression = "java(mapPm25Stats(stats))")
    @Mapping(target = "otherPollutants", expression = "java(mapOtherPollutants(stats))")
    @Mapping(target = "qualityDistribution", expression = "java(mapQualityDistribution(stats))")
    AirQualityStatisticsResponseDto toStatisticsResponse(AirQualityStatistics stats);

    default AirQualityStatisticsResponseDto.Pm10Statistics mapPm10Stats(AirQualityStatistics stats) {
        return new AirQualityStatisticsResponseDto.Pm10Statistics(
                stats.pm10Avg(), stats.pm10Min(), stats.pm10Max()
        );
    }

    default AirQualityStatisticsResponseDto.Pm25Statistics mapPm25Stats(AirQualityStatistics stats) {
        return new AirQualityStatisticsResponseDto.Pm25Statistics(
                stats.pm25Avg(), stats.pm25Min(), stats.pm25Max()
        );
    }

    default AirQualityStatisticsResponseDto.OtherPollutants mapOtherPollutants(AirQualityStatistics stats) {
        return new AirQualityStatisticsResponseDto.OtherPollutants(
                stats.so2Avg(), stats.no2Avg(), stats.coAvg(), stats.o3Avg()
        );
    }

    default AirQualityStatisticsResponseDto.QualityDistribution mapQualityDistribution(AirQualityStatistics stats) {
        return new AirQualityStatisticsResponseDto.QualityDistribution(
                stats.veryGoodCount(), stats.goodCount(), stats.moderateCount(),
                stats.sufficientCount(), stats.badCount(), stats.veryBadCount()
        );
    }

    // Ranking mappings
    StationRankingResponseDto toRankingResponse(StationRanking ranking);

    List<StationRankingResponseDto> toRankingResponseList(List<StationRanking> rankings);
}
