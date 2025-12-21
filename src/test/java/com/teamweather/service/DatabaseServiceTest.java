package test.java.com.teamweather.service;


import com.teamweather.exception.DatabaseException;
import com.teamweather.model.City;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseServiceTest {

    private DatabaseService databaseService;
    private static final String TEST_DB_PATH = "test_weather.db";

    @BeforeEach
    void setUp() throws DatabaseException {
        // Удаляем старый тестовый файл если существует
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }

        // Используем тестовую базу данных
        System.setProperty("db.url", "jdbc:sqlite:" + TEST_DB_PATH);
        databaseService = new DatabaseService();
    }

    @AfterEach
    void tearDown() {
        if (databaseService != null) {
            databaseService.close();
        }

        // Удаляем тестовый файл базы данных
        File dbFile = new File(TEST_DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    void testSaveAndRetrieveCity() throws DatabaseException {
        // Создаем тестовый город
        City city = new City("TestCity", 55.7558, 37.6173, "RU", "TestRegion");

        // Сохраняем город
        databaseService.saveOrUpdateCity(city);

        // Получаем последний выбранный город
        City retrievedCity = databaseService.getLastSelectedCity();

        assertNotNull(retrievedCity);
        assertEquals("TestCity", retrievedCity.getName());
        assertEquals(55.7558, retrievedCity.getLatitude(), 0.0001);
        assertEquals(37.6173, retrievedCity.getLongitude(), 0.0001);
        assertEquals("RU", retrievedCity.getCountry());
        assertEquals("TestRegion", retrievedCity.getState());
    }

    @Test
    void testGetLastSelectedCity_EmptyDatabase_ReturnsNull() throws DatabaseException {
        // С новой базой данных последний город должен быть null
        City city = databaseService.getLastSelectedCity();
        assertNull(city);
    }

    @Test
    void testSaveMultipleCities_RetrieveHistory() throws DatabaseException {
        // Сохраняем несколько городов
        City city1 = new City("City1", 55.7558, 37.6173, "RU");
        City city2 = new City("City2", 59.9343, 30.3351, "RU");
        City city3 = new City("City3", 51.5074, -0.1278, "GB");

        databaseService.saveOrUpdateCity(city1);
        databaseService.saveOrUpdateCity(city2);
        databaseService.saveOrUpdateCity(city3);

        // Получаем историю поиска
        List<City> history = databaseService.getSearchHistory();

        assertNotNull(history);
        assertTrue(history.size() >= 3);

        // Проверяем что города в истории
        List<String> cityNames = history.stream()
                .map(City::getName)
                .toList();

        assertTrue(cityNames.contains("City1"));
        assertTrue(cityNames.contains("City2"));
        assertTrue(cityNames.contains("City3"));
    }

    @Test
    void testUpdateExistingCity() throws DatabaseException {
        // Сохраняем город
        City city = new City("SameCity", 55.7558, 37.6173, "RU");
        databaseService.saveOrUpdateCity(city);

        // Обновляем с теми же координатами
        City updatedCity = new City("SameCity", 55.7558, 37.6173, "RU", "UpdatedRegion");
        databaseService.saveOrUpdateCity(updatedCity);

        // Проверяем что город обновился
        City retrievedCity = databaseService.getLastSelectedCity();
        assertNotNull(retrievedCity);
        assertEquals("SameCity", retrievedCity.getName());
        assertEquals("UpdatedRegion", retrievedCity.getState());
    }

    @Test
    void testDatabaseService_Close() {
        // Просто проверяем что close не выбрасывает исключений
        assertDoesNotThrow(() -> databaseService.close());
    }
}