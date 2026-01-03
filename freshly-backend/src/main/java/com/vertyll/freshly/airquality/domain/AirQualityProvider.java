package com.vertyll.freshly.airquality.domain;

import java.util.List;
import java.util.Optional;

public interface AirQualityProvider {
    List<Station> findAllStations();
    Optional<AirQualityIndex> findIndexByStationId(int stationId);

    List<SensorMeasurement> findMeasurementsByStationId(int stationId);
}