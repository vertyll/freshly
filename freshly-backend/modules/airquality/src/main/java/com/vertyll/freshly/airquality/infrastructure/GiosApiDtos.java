package com.vertyll.freshly.airquality.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

class GiosApiDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosStationDto(
            @JsonAlias("Identyfikator stacji") Integer id,
            @JsonAlias("Nazwa stacji") String stationName,
            @JsonAlias("WGS84 φ N") String gegrLat,
            @JsonAlias("WGS84 λ E") String gegrLon,
            @JsonAlias("Nazwa miasta") String cityName,
            @JsonAlias("Ulica") String addressStreet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosAQIndexDto(
            @JsonAlias("Identyfikator stacji pomiarowej") Integer id,
            @JsonAlias({"Data obliczenia", "Data wykonania obliczeń indeksu"}) String stCalcDate,
            @JsonAlias({"Indeks ogólny", "Nazwa kategorii indeksu"}) String stIndexLevel,
            @JsonAlias({"Indeks SO2", "Nazwa kategorii indeksu dla wskażnika SO2"})
                    String so2IndexLevel,
            @JsonAlias({"Indeks NO2", "Nazwa kategorii indeksu dla wskażnika NO2"})
                    String no2IndexLevel,
            @JsonAlias({"Indeks PM10", "Nazwa kategorii indeksu dla wskażnika PM10"})
                    String pm10IndexLevel) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosSensorDto(
            @JsonAlias({"Identyfikator stanowiska", "id", "Identyfikator"}) Integer id,
            @JsonAlias({"Wskaźnik - kod", "paramCode", "Kod wskaźnika"}) String paramCode,
            @JsonAlias({"Wskaźnik", "Nazwa parametru", "paramName", "Wskaźnik - nazwa"})
                    String paramName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosDataValueDto(
            @JsonAlias({"data", "date", "Data"}) String date,
            @JsonAlias({"wartość", "value", "Wartość"}) Double value) {}
}
