package com.teamweather.exception;

public class CityNotFoundException extends WeatherException {

    public CityNotFoundException(String cityName) {
        super("Город '" + cityName + "' не найден. Проверьте правильность написания.");
    }

    public CityNotFoundException(String cityName, Throwable cause) {
        super("Город '" + cityName + "' не найден. Проверьте правильность написания.", cause);
    }
}