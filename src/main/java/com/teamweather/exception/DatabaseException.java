package com.teamweather.exception;


public class DatabaseException extends WeatherException {

    public DatabaseException(String message) {
        super("Ошибка базы данных: " + message);
    }

    public DatabaseException(String message, Throwable cause) {
        super("Ошибка базы данных: " + message, cause);
    }
}