package com.vertyll.freshly.airquality.domain.model;

public record SearchRadius(double kilometres) {
    public static final int MIN_KM = 1;
    public static final int MAX_KM = 100;
    public static final int DEFAULT_KM = 10;

    public SearchRadius {
        kilometres = Math.clamp(kilometres, MIN_KM, MAX_KM);
    }

    public static SearchRadius of(double requested) {
        return new SearchRadius(requested);
    }

    public boolean covers(double distanceInKm) {
        return distanceInKm <= kilometres;
    }
}
