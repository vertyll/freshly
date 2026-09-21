package com.vertyll.freshly.airquality.application.port.inbound.command;

import com.vertyll.freshly.airquality.application.dto.SyncReport;

public interface AirQualitySyncUseCase {
    SyncReport syncAll();

    long purgeExpired();
}
