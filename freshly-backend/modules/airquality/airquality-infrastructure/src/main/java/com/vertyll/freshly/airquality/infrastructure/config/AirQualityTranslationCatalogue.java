package com.vertyll.freshly.airquality.infrastructure.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vertyll.freshly.lang.i18n.TranslationCatalogue;

@Component
public class AirQualityTranslationCatalogue implements TranslationCatalogue {
    @Override
    public String context() {
        return "airquality";
    }

    @Override
    public Map<String, Map<String, String>> defaults() {
        Map<String, Map<String, String>> defaults = new LinkedHashMap<>();

        defaults.put(
            "error.airquality.dataNotFound",
            Map.of(
                "en",
                "Air quality data not found for the specified criteria",
                "pl",
                "Nie znaleziono danych dla podanych kryteriów"
            )
        );
        defaults.put(
            "error.airquality.invalidCoordinates",
            Map.of("en", "Those coordinates are not on the globe.", "pl", "Te współrzędne nie leżą na kuli ziemskiej.")
        );
        defaults.put(
            "error.airquality.invalidDateRange",
            Map.of(
                "en",
                "Invalid date range: start date must be before end date",
                "pl",
                "Nieprawidłowy zakres dat: data początkowa musi być wcześniejsza niż końcowa"
            )
        );
        defaults.put(
            "error.airquality.providerUnavailable",
            Map.of(
                "en",
                "The air quality service is unavailable. Try again shortly.",
                "pl",
                "Serwis jakości powietrza jest niedostępny. Spróbuj za chwilę."
            )
        );
        defaults.put(
            "error.airquality.providerResponseInvalid",
            Map.of(
                "en",
                "The air quality service returned data that could not be read.",
                "pl",
                "Serwis jakości powietrza zwrócił dane, których nie da się odczytać."
            )
        );
        defaults.put(
            "error.airquality.stationNotFound",
            Map.of("en", "Air quality station not found", "pl", "Nie znaleziono stacji monitoringu powietrza")
        );

        defaults.put(
            "permission.airquality.sync",
            Map.of(
                "en",
                "Run the measurement sync by hand, outside its schedule.",
                "pl",
                "Ręczne uruchomienie synchronizacji pomiarów, poza harmonogramem."
            )
        );
        defaults.put(
            "permission.airquality.purge",
            Map.of(
                "en",
                "Delete measurements older than the retention window.",
                "pl",
                "Usuwanie pomiarów starszych niż okres przechowywania."
            )
        );

        return Map.copyOf(defaults);
    }
}
