package com.vertyll.freshly.airquality.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

class GiosApiDtos {

    // Station field aliases
    private static final String STATION_ID_PL = "Identyfikator stacji";
    private static final String STATION_NAME_PL = "Nazwa stacji";
    private static final String STATION_LAT_PL = "WGS84 φ N";
    private static final String STATION_LON_PL = "WGS84 λ E";
    private static final String CITY_NAME_PL = "Nazwa miasta";
    private static final String STREET_PL = "Ulica";

    // AQ Index field aliases
    private static final String AQ_STATION_ID_PL = "Identyfikator stacji pomiarowej";
    private static final String CALC_DATE_PL = "Data obliczenia";
    private static final String CALC_DATE_FULL_PL = "Data wykonania obliczeń indeksu";
    private static final String INDEX_OVERALL_PL = "Indeks ogólny";
    private static final String INDEX_CATEGORY_PL = "Nazwa kategorii indeksu";
    private static final String INDEX_SO2_PL = "Indeks SO2";
    private static final String INDEX_SO2_CATEGORY_PL = "Nazwa kategorii indeksu dla wskażnika SO2";
    private static final String INDEX_NO2_PL = "Indeks NO2";
    private static final String INDEX_NO2_CATEGORY_PL = "Nazwa kategorii indeksu dla wskażnika NO2";
    private static final String INDEX_PM10_PL = "Indeks PM10";
    private static final String INDEX_PM10_CATEGORY_PL =
            "Nazwa kategorii indeksu dla wskażnika PM10";

    // Sensor field aliases
    private static final String SENSOR_ID_PL = "Identyfikator stanowiska";
    private static final String SENSOR_ID_EN = "id";
    private static final String SENSOR_ID_SHORT_PL = "Identyfikator";
    private static final String PARAM_CODE_PL = "Wskaźnik - kod";
    private static final String PARAM_CODE_EN = "paramCode";
    private static final String PARAM_CODE_SHORT_PL = "Kod wskaźnika";
    private static final String PARAM_NAME_PL = "Wskaźnik";
    private static final String PARAM_NAME_FULL_PL = "Nazwa parametru";
    private static final String PARAM_NAME_EN = "paramName";
    private static final String PARAM_NAME_ALT_PL = "Wskaźnik - nazwa";

    // Data value field aliases
    private static final String DATE_EN_LOWERCASE = "data";
    private static final String DATE_EN = "date";
    private static final String DATE_PL = "Data";
    private static final String VALUE_PL_LOWERCASE = "wartość";
    private static final String VALUE_EN = "value";
    private static final String VALUE_PL = "Wartość";

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosStationDto(
            @JsonAlias(STATION_ID_PL) Integer id,
            @JsonAlias(STATION_NAME_PL) String stationName,
            @JsonAlias(STATION_LAT_PL) String gegrLat,
            @JsonAlias(STATION_LON_PL) String gegrLon,
            @JsonAlias(CITY_NAME_PL) String cityName,
            @JsonAlias(STREET_PL) String addressStreet) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosAQIndexDto(
            @JsonAlias(AQ_STATION_ID_PL) Integer id,
            @JsonAlias({CALC_DATE_PL, CALC_DATE_FULL_PL}) String stCalcDate,
            @JsonAlias({INDEX_OVERALL_PL, INDEX_CATEGORY_PL}) String stIndexLevel,
            @JsonAlias({INDEX_SO2_PL, INDEX_SO2_CATEGORY_PL}) String so2IndexLevel,
            @JsonAlias({INDEX_NO2_PL, INDEX_NO2_CATEGORY_PL}) String no2IndexLevel,
            @JsonAlias({INDEX_PM10_PL, INDEX_PM10_CATEGORY_PL}) String pm10IndexLevel) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosSensorDto(
            @JsonAlias({SENSOR_ID_PL, SENSOR_ID_EN, SENSOR_ID_SHORT_PL}) Integer id,
            @JsonAlias({PARAM_CODE_PL, PARAM_CODE_EN, PARAM_CODE_SHORT_PL}) String paramCode,
            @JsonAlias({PARAM_NAME_PL, PARAM_NAME_FULL_PL, PARAM_NAME_EN, PARAM_NAME_ALT_PL})
                    String paramName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GiosDataValueDto(
            @JsonAlias({DATE_EN_LOWERCASE, DATE_EN, DATE_PL}) String date,
            @JsonAlias({VALUE_PL_LOWERCASE, VALUE_EN, VALUE_PL}) Double value) {}
}
