package com.vertyll.freshly.airquality.infrastructure.persistence.mongo;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.airquality.domain.AirQualityMeasurement;

@Component
class AirQualityMeasurementMapper {

    public AirQualityMeasurementDocument toDocument(AirQualityMeasurement domain) {
        return new AirQualityMeasurementDocument(
                domain.getId(),
                domain.getStationId(),
                domain.getStationName(),
                domain.getMeasurementDate(),
                domain.getOverallIndexLevel(),
                domain.getSo2IndexLevel(),
                domain.getNo2IndexLevel(),
                domain.getPm10IndexLevel(),
                domain.getPm25IndexLevel(),
                domain.getPm10Value(),
                domain.getPm25Value(),
                domain.getSo2Value(),
                domain.getNo2Value(),
                domain.getCoValue(),
                domain.getO3Value(),
                domain.getCreatedAt());
    }

    public AirQualityMeasurement toDomain(AirQualityMeasurementDocument document) {
        return new AirQualityMeasurement(
                document.getId(),
                document.getStationId(),
                document.getStationName(),
                document.getMeasurementDate(),
                document.getOverallIndexLevel(),
                document.getSo2IndexLevel(),
                document.getNo2IndexLevel(),
                document.getPm10IndexLevel(),
                document.getPm25IndexLevel(),
                document.getPm10Value(),
                document.getPm25Value(),
                document.getSo2Value(),
                document.getNo2Value(),
                document.getCoValue(),
                document.getO3Value(),
                document.getCreatedAt());
    }
}
