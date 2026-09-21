package com.vertyll.freshly.airquality.infrastructure.persistence.adapter;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.airquality.domain.model.Coordinates;
import com.vertyll.freshly.airquality.domain.model.SearchRadius;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.airquality.domain.model.StationDistance;
import com.vertyll.freshly.airquality.domain.repository.StationCatalogue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StationCatalogueAdapter implements StationCatalogue {
    private final StationCache cache;

    @Override
    public Optional<Station> findById(int stationId) {
        return Optional.ofNullable(cache.load().byId().get(stationId));
    }

    @Override
    public List<Station> findAll() {
        return cache.load().all();
    }

    @Override
    public List<StationDistance> findWithin(Coordinates centre, SearchRadius radius) {
        return cache.load()
            .all()
            .stream()
            .map(station -> new StationDistance(station, station.distanceTo(centre)))
            .filter(candidate -> radius.covers(candidate.distanceInKm()))
            .sorted(Comparator.comparingDouble(StationDistance::distanceInKm))
            .toList();
    }
}
