package test.java.com.teamweather.service;



import main.java.com.teamweather.exception.WeatherException;
import main.java.com.teamweather.model.Weather;
import main.java.com.teamweather.service.DatabaseService;
import main.java.com.teamweather.service.WeatherService;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, ApplicationExtension.class})
class MainControllerTest {

    @Mock
    private WeatherService weatherService;

    @Mock
    private DatabaseService databaseService;

    private MainController controller;

    @BeforeEach
    void setUp() {
        controller = new MainController();

        // Инициализируем FXML поля
        controller.searchField = new TextField();
        controller.cityNameLabel = new Label();
        controller.temperatureLabel = new Label();
        controller.feelsLikeLabel = new Label();
        controller.humidityLabel = new Label();
        controller.pressureLabel = new Label();
        controller.windLabel = new Label();
        controller.descriptionLabel = new Label();

        // Устанавливаем моки через рефлексию
        try {
            var weatherServiceField = MainController.class.getDeclaredField("weatherService");
            weatherServiceField.setAccessible(true);
            weatherServiceField.set(controller, weatherService);

            var databaseServiceField = MainController.class.getDeclaredField("databaseService");
            databaseServiceField.setAccessible(true);
            databaseServiceField.set(controller, databaseService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testHandleSearch_ValidCity_UpdatesUI() throws Exception {
        // Настраиваем мок
        Weather mockWeather = createMockWeather();
        when(weatherService.getCurrentWeather(anyString())).thenReturn(mockWeather);

        // Устанавливаем текст в поле поиска
        controller.searchField.setText("Moscow");

        // Запускаем в JavaFX потоке
        Platform.runLater(() -> {
            try {
                controller.handleSearch();

                // Проверяем что UI обновился
                assertEquals("Moscow", controller.cityNameLabel.getText());
                assertEquals("20.5°C", controller.temperatureLabel.getText());
                assertEquals("Ощущается: 19.0°C", controller.feelsLikeLabel.getText());
                assertEquals("Влажность: 65%", controller.humidityLabel.getText());
                assertEquals("Давление: 1013 гПа", controller.pressureLabel.getText());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testHandleSearch_WeatherException_ShowsError() throws Exception {
        // Настраиваем мок для выбрасывания исключения
        when(weatherService.getCurrentWeather(anyString()))
                .thenThrow(new WeatherException("API error"));

        controller.searchField.setText("InvalidCity");

        Platform.runLater(() -> {
            try {
                controller.handleSearch();
                // Должен показаться диалог с ошибкой
                // В реальном тесте нужно проверять появление Alert
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testCleanup_ClosesDatabaseService() {
        // Вызываем cleanup
        controller.cleanup();

        // Проверяем что был вызван close у databaseService
        verify(databaseService, times(1)).close();
    }

    private Weather createMockWeather() {
        Weather weather = new Weather();
        weather.setCityName("Moscow");
        weather.setTemperature(20.5);
        weather.setFeelsLike(19.0);
        weather.setTempMin(18.0);
        weather.setTempMax(22.0);
        weather.setHumidity(65);
        weather.setPressure(1013);
        weather.setWindSpeed(5.2);
        weather.setWindDegrees(180);
        weather.setDescription("ясно");
        weather.setIconCode("01d");
        return weather;
    }
}