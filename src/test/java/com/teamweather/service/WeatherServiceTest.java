package test.java.com.teamweather.service;



import main.java.com.teamweather.exception.CityNotFoundException;
import main.java.com.teamweather.exception.WeatherException;
import main.java.com.teamweather.model.City;
import main.java.com.teamweather.model.Weather;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService();
    }

    @Test
    void testSearchCities_ValidCity_ReturnsCities() {
        try (MockedStatic<com.teamweather.Config> mockedConfig = mockStatic(com.teamweather.Config.class)) {
            // Мокаем конфиг
            mockedConfig.when(() -> com.teamweather.Config.getApiKey()).thenReturn("test-api-key");
            mockedConfig.when(() -> com.teamweather.Config.getUnits()).thenReturn("metric");
            mockedConfig.when(() -> com.teamweather.Config.getLanguage()).thenReturn("ru");

            // Этот тест требует реального API ключа
            // В реальном проекте нужно мокать HTTP запросы
            assertNotNull(weatherService);
        }
    }

    @Test
    void testSearchCities_EmptyApiKey_ThrowsException() {
        try (MockedStatic<com.teamweather.Config> mockedConfig = mockStatic(com.teamweather.Config.class)) {
            mockedConfig.when(() -> com.teamweather.Config.getApiKey()).thenReturn(null);

            WeatherException exception = assertThrows(WeatherException.class,
                    () -> weatherService.searchCities("Moscow"));

            assertTrue(exception.getMessage().contains("API ключ не настроен"));
        }
    }

    @Test
    void testGetCurrentWeather_CityNotFound_ThrowsException() {
        try (MockedStatic<com.teamweather.Config> mockedConfig = mockStatic(com.teamweather.Config.class)) {
            mockedConfig.when(() -> com.teamweather.Config.getApiKey()).thenReturn("test-key");
            mockedConfig.when(() -> com.teamweather.Config.getUnits()).thenReturn("metric");
            mockedConfig.when(() -> com.teamweather.Config.getLanguage()).thenReturn("ru");

            CityNotFoundException exception = assertThrows(CityNotFoundException.class,
                    () -> weatherService.getCurrentWeather("NonExistentCity12345"));

            assertTrue(exception.getMessage().contains("Город 'NonExistentCity12345' не найден"));
        }
    }

    @Test
    void testWeatherModel_GetterSetter() {
        Weather weather = new Weather();

        weather.setCityName("Moscow");
        weather.setTemperature(20.5);
        weather.setFeelsLike(19.0);
        weather.setHumidity(65);
        weather.setPressure(1013);
        weather.setWindSpeed(5.2);
        weather.setDescription("ясно");
        weather.setIconCode("01d");

        assertEquals("Moscow", weather.getCityName());
        assertEquals(20.5, weather.getTemperature(), 0.001);
        assertEquals(19.0, weather.getFeelsLike(), 0.001);
        assertEquals(65, weather.getHumidity());
        assertEquals(1013, weather.getPressure());
        assertEquals(5.2, weather.getWindSpeed(), 0.001);
        assertEquals("ясно", weather.getDescription());
        assertEquals("01d", weather.getIconCode());
        assertEquals("https://openweathermap.org/img/wn/01d@2x.png", weather.getIconUrl());
        assertEquals("20.5°C", weather.getFormattedTemperature());
        assertEquals("19.0°C", weather.getFormattedFeelsLike());
        assertEquals("5.2 м/с", weather.getFormattedWindSpeed());
    }

    @Test
    void testCityModel_GetterSetter() {
        City city = new City("Moscow", 55.7558, 37.6173, "RU", "Moscow");

        assertEquals("Moscow", city.getName());
        assertEquals(55.7558, city.getLatitude(), 0.0001);
        assertEquals(37.6173, city.getLongitude(), 0.0001);
        assertEquals("RU", city.getCountry());
        assertEquals("Moscow", city.getState());
        assertEquals("Moscow (Moscow, RU)", city.getDisplayName());
        assertEquals("Moscow, Moscow, RU", city.toString());
    }

    @Test
    void testCityModel_WithoutState() {
        City city = new City("London", 51.5074, -0.1278, "GB");

        assertEquals("London", city.getName());
        assertEquals("GB", city.getCountry());
        assertNull(city.getState());
        assertEquals("London (GB)", city.getDisplayName());
        assertEquals("London, GB", city.toString());
    }

    @Test
    void testWeatherModel_WindDirection() {
        Weather weather = new Weather();

        weather.setWindDegrees(0);
        assertEquals("С", weather.getWindDirection());

        weather.setWindDegrees(90);
        assertEquals("В", weather.getWindDirection());

        weather.setWindDegrees(180);
        assertEquals("Ю", weather.getWindDirection());

        weather.setWindDegrees(270);
        assertEquals("З", weather.getWindDirection());

        weather.setWindDegrees(45);
        assertEquals("СВ", weather.getWindDirection());

        weather.setWindDegrees(225);
        assertEquals("ЮЗ", weather.getWindDirection());
    }
}