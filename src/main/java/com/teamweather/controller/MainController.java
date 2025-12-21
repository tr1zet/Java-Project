package com.teamweather.controller;

import com.teamweather.exception.CityNotFoundException;
import com.teamweather.exception.WeatherException;
import com.teamweather.model.City;
import com.teamweather.model.Weather;
import com.teamweather.service.DatabaseService;
import com.teamweather.service.WeatherService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // Сервисы
    private WeatherService weatherService;
    private DatabaseService databaseService;

    // FXML элементы
    @FXML private TextField searchField;
    @FXML private ListView<String> suggestionsListView;
    @FXML private Label cityNameLabel;
    @FXML private Label temperatureLabel;
    @FXML private Label feelsLikeLabel;
    @FXML private Label humidityLabel;
    @FXML private Label pressureLabel;
    @FXML private Label windLabel;
    @FXML private Label descriptionLabel;
    @FXML private ImageView weatherIcon;
    @FXML private VBox forecastContainer;
    @FXML private ProgressIndicator loadingIndicator;

    // Списки для автодополнения
    private ObservableList<String> suggestions;
    private List<City> foundCities;

    /**
     * Инициализация контроллера
     */
    @FXML
    public void initialize() {
        logger.info("Инициализация MainController");

        try {
            // Инициализируем сервисы
            weatherService = new WeatherService();
            databaseService = new DatabaseService();

            // Настраиваем UI
            setupUI();

            // Загружаем последний выбранный город
            loadLastCity();

            logger.info("MainController инициализирован успешно");

        } catch (Exception e) {
            logger.error("Ошибка при инициализации контроллера", e);
            showError("Ошибка инициализации",
                    "Не удалось инициализировать приложение: " + e.getMessage());
        }
    }

    /**
     * Настройка UI элементов
     */
    private void setupUI() {
        // Настройка списка предложений
        suggestions = FXCollections.observableArrayList();
        suggestionsListView.setItems(suggestions);
        suggestionsListView.setVisible(false);

        // Обработчик изменений в поле поиска
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() >= 2) {
                searchCitiesAsync(newValue);
            } else {
                suggestionsListView.setVisible(false);
            }
        });

        // Обработчик выбора города из списка
        suggestionsListView.setOnMouseClicked(event -> {
            String selected = suggestionsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                searchField.setText(selected);
                suggestionsListView.setVisible(false);
                loadWeatherForSelectedCity(selected);
            }
        });

        // Обработчик нажатия Enter в поле поиска
        searchField.setOnAction(event -> handleSearch());

        // Скрываем индикатор загрузки по умолчанию
        loadingIndicator.setVisible(false);
    }

    /**
     * Асинхронный поиск городов для автодополнения
     */
    private void searchCitiesAsync(String query) {
        Task<List<City>> searchTask = new Task<>() {
            @Override
            protected List<City> call() throws Exception {
                return weatherService.searchCities(query);
            }
        };

        searchTask.setOnSucceeded(event -> {
            try {
                foundCities = searchTask.getValue();
                updateSuggestionsList();
            } catch (Exception e) {
                logger.error("Ошибка при обновлении списка городов", e);
            }
        });

        searchTask.setOnFailed(event -> {
            // Не показываем ошибку для автодополнения - просто скрываем список
            suggestionsListView.setVisible(false);
        });

        new Thread(searchTask).start();
    }

    /**
     * Обновление списка предложений
     */
    private void updateSuggestionsList() {
        Platform.runLater(() -> {
            suggestions.clear();
            if (foundCities != null && !foundCities.isEmpty()) {
                for (City city : foundCities) {
                    suggestions.add(city.getDisplayName());
                }
                suggestionsListView.setVisible(true);
            } else {
                suggestionsListView.setVisible(false);
            }
        });
    }

    /**
     * Загрузка последнего выбранного города
     */
    private void loadLastCity() {
        try {
            City lastCity = databaseService.getLastSelectedCity();
            if (lastCity != null) {
                searchField.setText(lastCity.getName());
                loadWeatherForCity(lastCity);
            }
        } catch (Exception e) {
            logger.warn("Не удалось загрузить последний город", e);
        }
    }

    /**
     * Обработчик кнопки поиска
     */
    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (!query.isEmpty()) {
            loadWeatherForQuery(query);
        }
    }

    /**
     * Загрузка погоды по запросу
     */
    private void loadWeatherForQuery(String query) {
        showLoading(true);

        Task<Weather> weatherTask = new Task<>() {
            @Override
            protected Weather call() throws Exception {
                return weatherService.getCurrentWeather(query);
            }
        };

        weatherTask.setOnSucceeded(event -> {
            try {
                Weather weather = weatherTask.getValue();
                updateWeatherUI(weather);
                saveCityToDatabase(weather);
                loadForecastAsync(weather.getCityName());
            } catch (Exception e) {
                logger.error("Ошибка при обновлении UI", e);
                showError("Ошибка", "Не удалось обновить данные: " + e.getMessage());
            } finally {
                showLoading(false);
            }
        });

        weatherTask.setOnFailed(event -> {
            showLoading(false);
            Throwable exception = weatherTask.getException();

            if (exception instanceof CityNotFoundException) {
                showError("Город не найден", exception.getMessage());
            } else if (exception instanceof WeatherException) {
                showError("Ошибка погоды", exception.getMessage());
            } else {
                showError("Ошибка", "Не удалось получить данные: " + exception.getMessage());
            }

            logger.error("Ошибка при получении погоды", exception);
        });

        new Thread(weatherTask).start();
    }

    /**
     * Загрузка погоды для выбранного города из списка
     */
    private void loadWeatherForSelectedCity(String selectedDisplayName) {
        if (foundCities != null) {
            for (City city : foundCities) {
                if (city.getDisplayName().equals(selectedDisplayName)) {
                    loadWeatherForCity(city);
                    break;
                }
            }
        }
    }

    /**
     * Загрузка погоды для объекта города
     */
    private void loadWeatherForCity(City city) {
        showLoading(true);

        Task<Weather> weatherTask = new Task<>() {
            @Override
            protected Weather call() throws Exception {
                return weatherService.getCurrentWeather(city.getName());
            }
        };

        weatherTask.setOnSucceeded(event -> {
            try {
                Weather weather = weatherTask.getValue();
                updateWeatherUI(weather);
                saveCityToDatabase(weather);
                loadForecastAsync(weather.getCityName());
            } catch (Exception e) {
                logger.error("Ошибка при обновлении UI", e);
                showError("Ошибка", "Не удалось обновить данные: " + e.getMessage());
            } finally {
                showLoading(false);
            }
        });

        weatherTask.setOnFailed(event -> {
            showLoading(false);
            Throwable exception = weatherTask.getException();
            showError("Ошибка", "Не удалось получить данные: " + exception.getMessage());
            logger.error("Ошибка при получении погоды", exception);
        });

        new Thread(weatherTask).start();
    }

    /**
     * Асинхронная загрузка прогноза
     */
    private void loadForecastAsync(String cityName) {
        Task<List<Weather>> forecastTask = new Task<>() {
            @Override
            protected List<Weather> call() throws Exception {
                return weatherService.getForecast(cityName, 4);
            }
        };

        forecastTask.setOnSucceeded(event -> {
            try {
                List<Weather> forecast = forecastTask.getValue();
                updateForecastUI(forecast);
            } catch (Exception e) {
                logger.error("Ошибка при загрузке прогноза", e);
            }
        });

        forecastTask.setOnFailed(event -> {

            logger.warn("Не удалось загрузить прогноз", forecastTask.getException());
        });

        new Thread(forecastTask).start();
    }

    /**
     * Обновление UI с данными о погоде
     */
    private void updateWeatherUI(Weather weather) {
        Platform.runLater(() -> {
            cityNameLabel.setText(weather.getCityName());
            temperatureLabel.setText(weather.getFormattedTemperature());
            feelsLikeLabel.setText("Ощущается: " + weather.getFormattedFeelsLike());
            humidityLabel.setText("Влажность: " + weather.getHumidity() + "%");
            pressureLabel.setText("Давление: " + weather.getPressure() + " гПа");
            windLabel.setText("Ветер: " + weather.getFormattedWindSpeed() + " " + weather.getWindDirection());
            descriptionLabel.setText(weather.getDescription());

            // Загрузка иконки погоды
            loadWeatherIcon(weather.getIconUrl());

            logger.info("UI обновлен для города: {}", weather.getCityName());
        });
    }

    /**
     * Загрузка иконки погоды
     */
    private void loadWeatherIcon(String iconUrl) {
        Task<Image> iconTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                return new Image(iconUrl, 100, 100, true, true);
            }
        };

        iconTask.setOnSucceeded(event -> {
            weatherIcon.setImage(iconTask.getValue());
        });

        iconTask.setOnFailed(event -> {
            logger.warn("Не удалось загрузить иконку погоды", iconTask.getException());
            // Можно установить иконку по умолчанию
        });

        new Thread(iconTask).start();
    }

    /**
     * Обновление UI с прогнозом
     */
    private void updateForecastUI(List<Weather> forecast) {
        Platform.runLater(() -> {
            forecastContainer.getChildren().clear();

            if (forecast == null || forecast.isEmpty()) {
                Label noForecastLabel = new Label("Прогноз временно недоступен");
                noForecastLabel.getStyleClass().add("forecast-text");
                forecastContainer.getChildren().add(noForecastLabel);
                return;
            }

            for (int i = 0; i < forecast.size(); i++) {
                Weather day = forecast.get(i);
                HBox dayForecast = createForecastDay(day, i == 0 ? "Сегодня" :
                        i == 1 ? "Завтра" : day.getFormattedDate().split(" ")[0]);
                forecastContainer.getChildren().add(dayForecast);
            }

            logger.info("Прогноз обновлен: {} дней", forecast.size());
        });
    }

    /**
     * Создание элемента прогноза на день
     */
    private HBox createForecastDay(Weather weather, String dayName) {
        HBox hbox = new HBox(10);
        hbox.getStyleClass().add("forecast-day");

        Label dayLabel = new Label(dayName);
        dayLabel.getStyleClass().add("forecast-day-label");

        Label tempLabel = new Label(weather.getFormattedTemperature());
        tempLabel.getStyleClass().add("forecast-temp");

        Label descLabel = new Label(weather.getDescription());
        descLabel.getStyleClass().add("forecast-desc");

        hbox.getChildren().addAll(dayLabel, tempLabel, descLabel);
        return hbox;
    }

    /**
     * Сохранение города в базу данных
     */
    private void saveCityToDatabase(Weather weather) {
        try {
            City city = new City(
                    weather.getCityName(),
                    weather.getLatitude(),
                    weather.getLongitude(),
                    "" // Страну можно получить из API при необходимости
            );
            databaseService.saveOrUpdateCity(city);
            logger.info("Город сохранен в БД: {}", weather.getCityName());
        } catch (Exception e) {
            logger.warn("Не удалось сохранить город в БД", e);
        }
    }

    /**
     * Показать/скрыть индикатор загрузки
     */
    private void showLoading(boolean show) {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(show);
            searchField.setDisable(show);
        });
    }

    /**
     * Показать диалог с ошибкой
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Очистка ресурсов при закрытии
     */
    public void cleanup() {
        if (databaseService != null) {
            databaseService.close();
        }
    }
}