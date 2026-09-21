package com.vertyll.freshly.airquality.application.port.outbound;

import java.util.List;
import java.util.Optional;

import com.vertyll.freshly.airquality.domain.model.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.model.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.model.Station;

public interface AirQualityProviderPort {
    List<Station> findAllStations();

    Optional<AirQualityIndex> findCurrentIndex(int stationId);

    List<SensorMeasurement> findSensorMeasurements(int stationId);
}
