package main.java.com.teamweather.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Weather {
    // Основные данные о погоде
    private String cityName; // Название города
    private double temperature; // Температура
    private double feelsLike; // Ощущаемая температура
    private double tempMin; // Минимальная температура
    private double tempMax; // Максимальная температура
    private int humidity; // Влажность в процентах
    private int pressure; // Давление в hPa
    private double windSpeed; // Скорость ветра в м/с
    private int windDegrees; // Направление ветра в градусах
    private String description; // Описание погоды (ясно, облачно и т.д.)
    private String iconCode; // Код иконки погоды
    private double latitude; // Широта
    private double longitude; // Долгота
    private long timestamp; // Время получения данных
    private long sunrise; // Время восхода
    private long sunset; // Время заката
    private int visibility; // Видимость в метрах
    private int cloudiness; // Облачность в процентах

    // Конструктор
    public Weather() {
        this.timestamp = System.currentTimeMillis() / 1000; // Текущее время в секундах
    }

    // Геттеры
    public String getCityName() { return cityName; }
    public double getTemperature() { return temperature; }
    public double getFeelsLike() { return feelsLike; }
    public double getTempMin() { return tempMin; }
    public double getTempMax() { return tempMax; }
    public int getHumidity() { return humidity; }
    public int getPressure() { return pressure; }
    public double getWindSpeed() { return windSpeed; }
    public int getWindDegrees() { return windDegrees; }
    public String getDescription() { return description; }
    public String getIconCode() { return iconCode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }
    public long getSunrise() { return sunrise; }
    public long getSunset() { return sunset; }
    public int getVisibility() { return visibility; }
    public int getCloudiness() { return cloudiness; }

    // Сеттеры
    public void setCityName(String cityName) { this.cityName = cityName; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public void setFeelsLike(double feelsLike) { this.feelsLike = feelsLike; }
    public void setTempMin(double tempMin) { this.tempMin = tempMin; }
    public void setTempMax(double tempMax) { this.tempMax = tempMax; }
    public void setHumidity(int humidity) { this.humidity = humidity; }
    public void setPressure(int pressure) { this.pressure = pressure; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
    public void setWindDegrees(int windDegrees) { this.windDegrees = windDegrees; }
    public void setDescription(String description) { this.description = description; }
    public void setIconCode(String iconCode) { this.iconCode = iconCode; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setSunrise(long sunrise) { this.sunrise = sunrise; }
    public void setSunset(long sunset) { this.sunset = sunset; }
    public void setVisibility(int visibility) { this.visibility = visibility; }
    public void setCloudiness(int cloudiness) { this.cloudiness = cloudiness; }

    // Методы для форматирования данных

    // URL для загрузки иконки погоды
    public String getIconUrl() {
        return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
    }

    // Форматированная температура
    public String getFormattedTemperature() {
        return String.format("%.1f°C", temperature);
    }

    // Форматированная ощущаемая температура
    public String getFormattedFeelsLike() {
        return String.format("%.1f°C", feelsLike);
    }

    // Форматированная скорость ветра
    public String getFormattedWindSpeed() {
        return String.format("%.1f м/с", windSpeed);
    }

    // Направление ветра по сторонам света
    public String getWindDirection() {
        if (windDegrees >= 337.5 || windDegrees < 22.5) return "С";
        if (windDegrees < 67.5) return "СВ";
        if (windDegrees < 112.5) return "В";
        if (windDegrees < 157.5) return "ЮВ";
        if (windDegrees < 202.5) return "Ю";
        if (windDegrees < 247.5) return "ЮЗ";
        if (windDegrees < 292.5) return "З";
        return "СЗ";
    }

    // Преобразование timestamp в LocalDateTime
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        );
    }

    // Форматированная дата и время
    public String getFormattedDate() {
        return getLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    // Форматированное время восхода
    public String getFormattedSunrise() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(sunrise),
                ZoneId.systemDefault()
        ).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // Форматированное время заката
    public String getFormattedSunset() {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(sunset),
                ZoneId.systemDefault()
        ).format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    // Строковое представление
    @Override
    public String toString() {
        return cityName + ": " + getFormattedTemperature() + ", " + description;
    }
}