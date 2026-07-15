package com.vertyll.freshly.airquality.domain.exception;

import java.io.Serial;

public class StationNotFoundException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public StationNotFoundException(int stationId) {
        super(String.format("Station with ID %d not found.", stationId));
    }
}
