package com.teamweather;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) {
        try {
            // Загружаем интерфейс из FXML файла
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(root, 900, 700);

            // Настраиваем главное окно
            primaryStage.setTitle("Weather App - Прогноз погоды");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();

            logger.info("Приложение успешно запущено");
        } catch (Exception e) {
            logger.error("Ошибка при запуске приложения", e);
            showErrorAlert("Ошибка приложения", "Не удалось запустить приложение: " + e.getMessage());
        }
    }

    // Метод для показа ошибок
    private void showErrorAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        logger.info("Запуск Weather App...");
        launch(args);
    }
}