package com.vertyll.freshly.airquality.application.dto;

import java.time.Duration;

public record SyncReport(int stationsConsidered, int synced, int skipped, int failed, Duration took) {
    public boolean completelyFailed() {
        return stationsConsidered > 0 && synced == 0 && failed > 0;
    }
}
