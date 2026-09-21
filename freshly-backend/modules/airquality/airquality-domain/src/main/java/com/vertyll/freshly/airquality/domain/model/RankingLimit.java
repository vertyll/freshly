package com.vertyll.freshly.airquality.domain.model;

public record RankingLimit(int value) {
    public static final int MIN = 5;
    public static final int MAX = 50;
    public static final int DEFAULT = 10;

    public RankingLimit {
        value = Math.clamp(value, MIN, MAX);
    }

    public static RankingLimit of(int requested) {
        return new RankingLimit(requested);
    }
}
