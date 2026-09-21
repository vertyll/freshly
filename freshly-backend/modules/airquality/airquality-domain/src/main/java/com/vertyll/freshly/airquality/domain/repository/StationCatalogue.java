package com.vertyll.freshly.airquality.domain.repository;

import java.util.List;
import java.util.Optional;

import com.vertyll.freshly.airquality.domain.model.Coordinates;
import com.vertyll.freshly.airquality.domain.model.SearchRadius;
import com.vertyll.freshly.airquality.domain.model.Station;
import com.vertyll.freshly.airquality.domain.model.StationDistance;

public interface StationCatalogue {
    Optional<Station> findById(int stationId);

    List<Station> findAll();

    List<StationDistance> findWithin(Coordinates centre, SearchRadius radius);
}
