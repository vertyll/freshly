package com.vertyll.freshly.airquality.api.dto;

public record StationResponseDto(
        int id, String name, String city, String address, double latitude, double longitude) {}
