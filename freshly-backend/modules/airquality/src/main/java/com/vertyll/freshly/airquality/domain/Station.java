package com.vertyll.freshly.airquality.domain;

public record Station(
        int id, String name, String city, String address, double latitude, double longitude) {}
