package com.teamweather.service;

import com.teamweather.Config;
import com.teamweather.exception.CityNotFoundException;
import com.teamweather.exception.WeatherException;
import com.teamweather.model.City;
import com.teamweather.model.Weather;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    // URL для API OpenWeatherMap
    private static final String GEOCODING_URL = "http://api.openweathermap.org/geo/1.0/direct";
    private static final String CURRENT_WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";

    // Получаем API ключ из конфигурации
    private String getApiKey() throws WeatherException {
        String apiKey = Config.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new WeatherException("API ключ не настроен. Проверьте config.properties");
        }
        return apiKey;
    }

    /**
     * Поиск городов по названию (автодополнение)
     */
    public List<City> searchCities(String query) throws WeatherException {
        logger.info("Поиск городов по запросу: {}", query);
        List<City> cities = new ArrayList<>();

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlString = String.format("%s?q=%s&limit=5&appid=%s",
                    GEOCODING_URL, encodedQuery, getApiKey());

            String jsonResponse = fetchUrl(urlString);
            JSONArray citiesArray = new JSONArray(jsonResponse);

            for (int i = 0; i < citiesArray.length(); i++) {
                JSONObject cityJson = citiesArray.getJSONObject(i);
                City city = parseCityFromJson(cityJson);
                cities.add(city);
            }

            logger.info("Найдено городов: {}", cities.size());
            return cities;

        } catch (Exception e) {
            logger.error("Ошибка при поиске городов", e);
            throw new WeatherException("Ошибка при поиске городов: " + e.getMessage(), e);
        }
    }

    /**
     * Получение текущей погоды для города
     */
    public Weather getCurrentWeather(String cityName) throws WeatherException, CityNotFoundException {
        logger.info("Получение погоды для города: {}", cityName);

        try {
            // Сначала получаем координаты города
            City city = getCityCoordinates(cityName);

            // Затем получаем погоду по координатам
            String urlString = String.format("%s?lat=%f&lon=%f&appid=%s&units=%s&lang=%s",
                    CURRENT_WEATHER_URL, city.getLatitude(), city.getLongitude(),
                    getApiKey(), Config.getUnits(), Config.getLanguage());

            String jsonResponse = fetchUrl(urlString);
            JSONObject weatherJson = new JSONObject(jsonResponse);

            Weather weather = parseWeatherFromJson(weatherJson);
            weather.setCityName(city.getName());

            logger.info("Погода получена: {} - {}", cityName, weather.getFormattedTemperature());
            return weather;

        } catch (CityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Ошибка при получении погоды", e);
            throw new WeatherException("Ошибка при получении погоды: " + e.getMessage(), e);
        }
    }

    /**
     * Получение прогноза на 4 дня
     */
    public List<Weather> getForecast(String cityName, int days) throws WeatherException, CityNotFoundException {
        logger.info("Получение прогноза для города: {} на {} дней", cityName, days);
        List<Weather> forecast = new ArrayList<>();

        try {
            // Получаем координаты города
            City city = getCityCoordinates(cityName);

            // Получаем прогноз (8 записей в день, поэтому умножаем на 8)
            String urlString = String.format("%s?lat=%f&lon=%f&appid=%s&units=%s&lang=%s&cnt=%d",
                    FORECAST_URL, city.getLatitude(), city.getLongitude(),
                    getApiKey(), Config.getUnits(), Config.getLanguage(), days * 8);

            String jsonResponse = fetchUrl(urlString);
            JSONObject forecastJson = new JSONObject(jsonResponse);
            JSONArray list = forecastJson.getJSONArray("list");

            // Берем по одной записи в день (примерно в полдень)
            for (int i = 0; i < list.length() && forecast.size() < days; i += 8) {
                JSONObject forecastItem = list.getJSONObject(i);
                Weather weather = parseForecastFromJson(forecastItem);
                weather.setCityName(city.getName());
                forecast.add(weather);
            }

            logger.info("Прогноз получен: {} дней", forecast.size());
            return forecast;

        } catch (CityNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Ошибка при получении прогноза", e);
            throw new WeatherException("Ошибка при получении прогноза: " + e.getMessage(), e);
        }
    }

    /**
     * Получение координат города
     */
    private City getCityCoordinates(String cityName) throws WeatherException, CityNotFoundException {
        List<City> cities = searchCities(cityName);

        if (cities.isEmpty()) {
            throw new CityNotFoundException(cityName);
        }

        // Возвращаем первый подходящий город
        return cities.get(0);
    }

    /**
     * Парсинг города из JSON
     */
    private City parseCityFromJson(JSONObject cityJson) {
        City city = new City();
        city.setName(cityJson.getString("name"));
        city.setLatitude(cityJson.getDouble("lat"));
        city.setLongitude(cityJson.getDouble("lon"));

        if (cityJson.has("country")) {
            city.setCountry(cityJson.getString("country"));
        }

        if (cityJson.has("state")) {
            city.setState(cityJson.getString("state"));
        }

        return city;
    }

    /**
     * Парсинг текущей погоды из JSON
     */
    private Weather parseWeatherFromJson(JSONObject weatherJson) {
        Weather weather = new Weather();

        // Основные данные
        JSONObject main = weatherJson.getJSONObject("main");
        weather.setTemperature(main.getDouble("temp"));
        weather.setFeelsLike(main.getDouble("feels_like"));
        weather.setTempMin(main.getDouble("temp_min"));
        weather.setTempMax(main.getDouble("temp_max"));
        weather.setPressure(main.getInt("pressure"));
        weather.setHumidity(main.getInt("humidity"));

        // Координаты
        JSONObject coord = weatherJson.getJSONObject("coord");
        weather.setLatitude(coord.getDouble("lat"));
        weather.setLongitude(coord.getDouble("lon"));

        // Ветер
        JSONObject wind = weatherJson.getJSONObject("wind");
        weather.setWindSpeed(wind.getDouble("speed"));
        if (wind.has("deg")) {
            weather.setWindDegrees(wind.getInt("deg"));
        }

        // Описание и иконка
        JSONArray weatherArray = weatherJson.getJSONArray("weather");
        JSONObject weatherInfo = weatherArray.getJSONObject(0);
        weather.setDescription(weatherInfo.getString("description"));
        weather.setIconCode(weatherInfo.getString("icon"));

        // Облачность
        if (weatherJson.has("clouds")) {
            weather.setCloudiness(weatherJson.getJSONObject("clouds").getInt("all"));
        }

        // Видимость
        if (weatherJson.has("visibility")) {
            weather.setVisibility(weatherJson.getInt("visibility"));
        }

        // Время восхода и заката
        JSONObject sys = weatherJson.getJSONObject("sys");
        weather.setSunrise(sys.getLong("sunrise"));
        weather.setSunset(sys.getLong("sunset"));

        // Время получения данных
        weather.setTimestamp(weatherJson.getLong("dt"));

        return weather;
    }

    /**
     * Парсинг прогноза из JSON
     */
    private Weather parseForecastFromJson(JSONObject forecastJson) {
        Weather weather = new Weather();

        // Время прогноза
        weather.setTimestamp(forecastJson.getLong("dt"));

        // Основные данные
        JSONObject main = forecastJson.getJSONObject("main");
        weather.setTemperature(main.getDouble("temp"));
        weather.setFeelsLike(main.getDouble("feels_like"));
        weather.setTempMin(main.getDouble("temp_min"));
        weather.setTempMax(main.getDouble("temp_max"));
        weather.setPressure(main.getInt("pressure"));
        weather.setHumidity(main.getInt("humidity"));

        // Описание и иконка
        JSONArray weatherArray = forecastJson.getJSONArray("weather");
        JSONObject weatherInfo = weatherArray.getJSONObject(0);
        weather.setDescription(weatherInfo.getString("description"));
        weather.setIconCode(weatherInfo.getString("icon"));

        // Ветер
        JSONObject wind = forecastJson.getJSONObject("wind");
        weather.setWindSpeed(wind.getDouble("speed"));
        if (wind.has("deg")) {
            weather.setWindDegrees(wind.getInt("deg"));
        }

        // Облачность
        if (forecastJson.has("clouds")) {
            weather.setCloudiness(forecastJson.getJSONObject("clouds").getInt("all"));
        }

        // Вероятность осадков
        if (forecastJson.has("pop")) {
            // pop - probability of precipitation
        }

        return weather;
    }

    /**
     * Метод для выполнения HTTP запросов
     */
    private String fetchUrl(String urlString) throws Exception {
        logger.debug("Выполнение запроса: {}", urlString);

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            String errorMessage = readResponse(connection.getErrorStream());
            connection.disconnect();
            throw new WeatherException("API ошибка: " + responseCode + " - " + errorMessage);
        }

        String response = readResponse(connection.getInputStream());
        connection.disconnect();

        return response;
    }

    /**
     * Чтение ответа от сервера
     */
    private String readResponse(java.io.InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}