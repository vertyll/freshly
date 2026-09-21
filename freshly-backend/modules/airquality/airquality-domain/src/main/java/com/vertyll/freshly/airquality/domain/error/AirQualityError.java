package com.vertyll.freshly.airquality.domain.error;

import com.vertyll.freshly.lang.error.DomainError;
import com.vertyll.freshly.lang.error.ErrorKind;

public enum AirQualityError implements DomainError {
    STATION_NOT_FOUND("error.airquality.stationNotFound", ErrorKind.NOT_FOUND),
    DATA_NOT_FOUND("error.airquality.dataNotFound", ErrorKind.NOT_FOUND),
    INVALID_DATE_RANGE("error.airquality.invalidDateRange", ErrorKind.INVALID),
    INVALID_COORDINATES("error.airquality.invalidCoordinates", ErrorKind.INVALID),

    PROVIDER_UNAVAILABLE("error.airquality.providerUnavailable", ErrorKind.EXTERNAL_SERVICE_FAILURE),
    PROVIDER_RESPONSE_INVALID("error.airquality.providerResponseInvalid", ErrorKind.EXTERNAL_SERVICE_FAILURE);

    private final String key;
    private final ErrorKind kind;

    AirQualityError(String key, ErrorKind kind) {
        this.key = key;
        this.kind = kind;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public ErrorKind kind() {
        return kind;
    }
}
