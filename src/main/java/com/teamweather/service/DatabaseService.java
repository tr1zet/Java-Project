package main.java.com.teamweather.service;

import main.java.com.teamweather.Config;
import main.java.com.teamweather.exception.DatabaseException;
import main.java.com.teamweather.model.City;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);
    private static final String DB_URL = "jdbc:sqlite:weather.db";

    private Connection connection;

    public DatabaseService() throws DatabaseException {
        initDatabase();
    }

    /**
     * Инициализация базы данных
     */
    private void initDatabase() throws DatabaseException {
        try {
            // Создаем подключение к базе данных
            connection = DriverManager.getConnection(DB_URL);
            logger.info("Подключение к базе данных установлено: {}", DB_URL);

            // Создаем таблицы если они не существуют
            createTables();

        } catch (SQLException e) {
            logger.error("Ошибка при инициализации базы данных", e);
            throw new DatabaseException("Не удалось инициализировать базу данных", e);
        }
    }

    /**
     * Создание таблиц в базе данных
     */
    private void createTables() throws SQLException {
        String createCitiesTable = """
            CREATE TABLE IF NOT EXISTS cities (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                country TEXT,
                state TEXT,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                last_selected TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createWeatherHistoryTable = """
            CREATE TABLE IF NOT EXISTS weather_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                city_id INTEGER NOT NULL,
                temperature REAL NOT NULL,
                feels_like REAL NOT NULL,
                temp_min REAL NOT NULL,
                temp_max REAL NOT NULL,
                humidity INTEGER NOT NULL,
                pressure INTEGER NOT NULL,
                wind_speed REAL NOT NULL,
                description TEXT NOT NULL,
                icon_code TEXT NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE CASCADE
            )
            """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(createCitiesTable);
            statement.execute(createWeatherHistoryTable);
            logger.info("Таблицы базы данных созданы/проверены");
        }
    }

    /**
     * Сохранение или обновление города
     */
    public void saveOrUpdateCity(City city) throws DatabaseException {
        String sql = """
            INSERT OR REPLACE INTO cities (name, country, state, latitude, longitude, last_selected)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, city.getName());
            pstmt.setString(2, city.getCountry());
            pstmt.setString(3, city.getState());
            pstmt.setDouble(4, city.getLatitude());
            pstmt.setDouble(5, city.getLongitude());
            pstmt.executeUpdate();

            logger.info("Город сохранен в базу данных: {}", city.getName());

        } catch (SQLException e) {
            logger.error("Ошибка при сохранении города", e);
            throw new DatabaseException("Не удалось сохранить город: " + city.getName(), e);
        }
    }

    /**
     * Получение последнего выбранного города
     */
    public City getLastSelectedCity() throws DatabaseException {
        String sql = """
            SELECT name, country, state, latitude, longitude
            FROM cities
            ORDER BY last_selected DESC
            LIMIT 1
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                City city = new City();
                city.setName(rs.getString("name"));
                city.setCountry(rs.getString("country"));
                city.setState(rs.getString("state"));
                city.setLatitude(rs.getDouble("latitude"));
                city.setLongitude(rs.getDouble("longitude"));

                logger.info("Загружен последний выбранный город: {}", city.getName());
                return city;
            }

            logger.info("Нет сохраненных городов в базе данных");
            return null;

        } catch (SQLException e) {
            logger.error("Ошибка при получении последнего города", e);
            throw new DatabaseException("Не удалось получить последний город", e);
        }
    }

    /**
     * Получение истории поиска городов (последние 10)
     */
    public List<City> getSearchHistory() throws DatabaseException {
        List<City> history = new ArrayList<>();
        String sql = """
            SELECT name, country, state, latitude, longitude
            FROM cities
            ORDER BY last_selected DESC
            LIMIT 10
            """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                City city = new City();
                city.setName(rs.getString("name"));
                city.setCountry(rs.getString("country"));
                city.setState(rs.getString("state"));
                city.setLatitude(rs.getDouble("latitude"));
                city.setLongitude(rs.getDouble("longitude"));
                history.add(city);
            }

            logger.info("Загружено {} городов из истории", history.size());
            return history;

        } catch (SQLException e) {
            logger.error("Ошибка при получении истории поиска", e);
            throw new DatabaseException("Не удалось получить историю поиска", e);
        }
    }

    /**
     * Сохранение погоды в историю
     */
    public void saveWeatherToHistory(com.teamweather.model.Weather weather, City city) throws DatabaseException {
        // Сначала получаем или сохраняем город
        saveOrUpdateCity(city);

        // Получаем ID города
        int cityId = getCityId(city);
        if (cityId == -1) {
            throw new DatabaseException("Не удалось получить ID города для сохранения погоды");
        }

        String sql = """
            INSERT INTO weather_history
            (city_id, temperature, feels_like, temp_min, temp_max, humidity,
             pressure, wind_speed, description, icon_code)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, cityId);
            pstmt.setDouble(2, weather.getTemperature());
            pstmt.setDouble(3, weather.getFeelsLike());
            pstmt.setDouble(4, weather.getTempMin());
            pstmt.setDouble(5, weather.getTempMax());
            pstmt.setInt(6, weather.getHumidity());
            pstmt.setInt(7, weather.getPressure());
            pstmt.setDouble(8, weather.getWindSpeed());
            pstmt.setString(9, weather.getDescription());
            pstmt.setString(10, weather.getIconCode());
            pstmt.executeUpdate();

            logger.info("Погода сохранена в историю для города: {}", city.getName());

        } catch (SQLException e) {
            logger.error("Ошибка при сохранении погоды в историю", e);
            throw new DatabaseException("Не удалось сохранить погоду в историю", e);
        }
    }

    /**
     * Получение ID города из базы данных
     */
    private int getCityId(City city) throws SQLException {
        String sql = "SELECT id FROM cities WHERE name = ? AND latitude = ? AND longitude = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, city.getName());
            pstmt.setDouble(2, city.getLatitude());
            pstmt.setDouble(3, city.getLongitude());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        return -1;
    }

    /**
     * Закрытие подключения к базе данных
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Подключение к базе данных закрыто");
            } catch (SQLException e) {
                logger.error("Ошибка при закрытии подключения к базе данных", e);
            }
        }
    }

    /**
     * Получение истории погоды для города
     */
    public List<com.teamweather.model.Weather> getWeatherHistory(City city, int limit) throws DatabaseException {
        List<com.teamweather.model.Weather> history = new ArrayList<>();
        String sql = """
            SELECT wh.temperature, wh.feels_like, wh.temp_min, wh.temp_max,
                   wh.humidity, wh.pressure, wh.wind_speed,
                   wh.description, wh.icon_code, wh.timestamp
            FROM weather_history wh
            JOIN cities c ON wh.city_id = c.id
            WHERE c.name = ? AND c.latitude = ? AND c.longitude = ?
            ORDER BY wh.timestamp DESC
            LIMIT ?
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, city.getName());
            pstmt.setDouble(2, city.getLatitude());
            pstmt.setDouble(3, city.getLongitude());
            pstmt.setInt(4, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    com.teamweather.model.Weather weather = new com.teamweather.model.Weather();
                    weather.setTemperature(rs.getDouble("temperature"));
                    weather.setFeelsLike(rs.getDouble("feels_like"));
                    weather.setTempMin(rs.getDouble("temp_min"));
                    weather.setTempMax(rs.getDouble("temp_max"));
                    weather.setHumidity(rs.getInt("humidity"));
                    weather.setPressure(rs.getInt("pressure"));
                    weather.setWindSpeed(rs.getDouble("wind_speed"));
                    weather.setDescription(rs.getString("description"));
                    weather.setIconCode(rs.getString("icon_code"));

                    // Преобразуем timestamp
                    Timestamp timestamp = rs.getTimestamp("timestamp");
                    if (timestamp != null) {
                        weather.setTimestamp(timestamp.getTime() / 1000);
                    }

                    history.add(weather);
                }
            }

            logger.info("Загружено {} записей истории погоды для {}", history.size(), city.getName());
            return history;

        } catch (SQLException e) {
            logger.error("Ошибка при получении истории погоды", e);
            throw new DatabaseException("Не удалось получить историю погоды", e);
        }
    }
}