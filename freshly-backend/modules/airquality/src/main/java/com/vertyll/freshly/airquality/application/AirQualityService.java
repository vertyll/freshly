package com.vertyll.freshly.airquality.application;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.vertyll.freshly.airquality.domain.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AirQualityService {

    private final AirQualityProvider airQualityProvider;
    private final AirQualityHistoryRepository historyRepository;

    /** Get list of all air quality stations */
    public List<Station> getAllStations() {
        return airQualityProvider.findAllStations();
    }

    /** Get current air quality index from database (live data) */
    public Optional<AirQualityIndex> getIndexForStation(int stationId) {
        return airQualityProvider.findIndexByStationId(stationId);
    }

    /** Get current sensor measurements from database (live data) */
    public List<SensorMeasurement> getMeasurementsForStation(int stationId) {
        return airQualityProvider.findMeasurementsByStationId(stationId);
    }

    /** Get latest measurement from database (cached data) */
    public Optional<AirQualityMeasurement> getLatestMeasurement(int stationId) {
        return historyRepository.findLatestByStationId(stationId);
    }

    /**
     * Get historical measurements for a station (for charts)
     *
     * @param stationId Station ID
     * @param daysBack Number of days to look back (default 7, max 90)
     */
    public List<AirQualityMeasurement> getHistoricalMeasurements(int stationId, int daysBack) {
        int days = Math.clamp(daysBack, 1, 90); // Clamp between 1-90
        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(days);
        LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);
        return historyRepository.findByStationIdAndDateRange(stationId, from, to);
    }

    /** Get measurements for all stations within time range */
    public List<AirQualityMeasurement> getMeasurementsInRange(
            LocalDateTime from, LocalDateTime to) {
        return historyRepository.findByDateRange(from, to);
    }

    /**
     * Find nearest stations to given coordinates
     *
     * @param latitude Latitude
     * @param longitude Longitude
     * @param radiusKm Search radius in kilometers (default 10, max 100)
     * @return List of stations with distances, sorted by distance
     */
    public List<StationDistance> findNearestStations(
            double latitude, double longitude, double radiusKm) {
        double radius = Math.clamp(radiusKm, 1, 100); // Clamp between 1-100

        List<Station> allStations = airQualityProvider.findAllStations();
        List<StationDistance> stationsWithDistance = new ArrayList<>();

        for (Station station : allStations) {
            double distance =
                    calculateDistance(latitude, longitude, station.latitude(), station.longitude());

            if (distance <= radius) {
                stationsWithDistance.add(new StationDistance(station, distance));
            }
        }

        stationsWithDistance.sort(Comparator.comparingDouble(StationDistance::distanceInKm));

        log.debug(
                "Found {} stations within {}km of ({}, {})",
                stationsWithDistance.size(),
                radius,
                latitude,
                longitude);

        return stationsWithDistance;
    }

    /** Calculate statistics for a station within time range */
    public Optional<AirQualityStatistics> getStatistics(int stationId, int daysBack) {
        int days = Math.clamp(daysBack, 1, 90);
        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(days);
        LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);

        return historyRepository.calculateStatistics(stationId, from, to);
    }

    /**
     * Get ranking of stations (best to worst air quality)
     *
     * @param daysBack Number of days to analyze (default 7, max 90)
     * @param limit Max number of stations to return (default 10, max 50)
     */
    public List<StationRanking> getRanking(int daysBack, int limit) {
        int days = Math.clamp(daysBack, 1, 90);
        int maxResults = Math.clamp(limit, 5, 50);

        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(days);
        LocalDateTime to = LocalDateTime.now(ZoneOffset.UTC);

        return historyRepository.getRanking(from, to, maxResults);
    }

    /** Haversine formula to calculate distance between two coordinates */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int earthRadiusKm = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2)
                        + Math.cos(Math.toRadians(lat1))
                                * Math.cos(Math.toRadians(lat2))
                                * Math.sin(dLon / 2)
                                * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }
}
