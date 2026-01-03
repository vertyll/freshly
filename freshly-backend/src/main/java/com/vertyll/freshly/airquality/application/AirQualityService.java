package com.vertyll.freshly.airquality.application;

import com.vertyll.freshly.airquality.domain.AirQualityIndex;
import com.vertyll.freshly.airquality.domain.AirQualityProvider;
import com.vertyll.freshly.airquality.domain.SensorMeasurement;
import com.vertyll.freshly.airquality.domain.Station;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirQualityService {

    private final AirQualityProvider airQualityProvider;

    public List<Station> getAllStations() {
        return airQualityProvider.findAllStations();
    }

    public Optional<AirQualityIndex> getIndexForStation(int stationId) {
        return airQualityProvider.findIndexByStationId(stationId);
    }

    public List<SensorMeasurement> getMeasurementsForStation(int stationId) {
        return airQualityProvider.findMeasurementsByStationId(stationId);
    }
}