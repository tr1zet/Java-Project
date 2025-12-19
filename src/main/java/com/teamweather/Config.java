package main.java.com.teamweather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Properties properties = new Properties();

    // Статический блок загружает конфигурацию при старте
    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = Config.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Конфигурация успешно загружена");
            } else {
                logger.warn("Файл конфигурации не найден, используются значения по умолчанию");
            }
        } catch (Exception e) {
            logger.error("Ошибка при загрузке конфигурации", e);
        }
    }

    // Основные методы для получения настроек
    public static String get(String key) {
        return properties.getProperty(key);
    }

    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    // Специальные методы для часто используемых настроек
    public static String getApiKey() {
        String key = get("api.key");
        if (key == null || key.trim().isEmpty() || key.contains("ВАШ_API_КЛЮЧ")) {
            logger.error("API ключ не настроен!");
            return null;
        }
        return key.trim();
    }

    public static String getUnits() {
        return get("units", "metric"); // metric - градусы Цельсия
    }

    public static String getLanguage() {
        return get("lang", "ru"); // Русский язык по умолчанию
    }

    public static String getDefaultCity() {
        return get("default.city", "Moscow"); // Москва по умолчанию
    }

    public static int getCacheTtlMinutes() {
        try {
            return Integer.parseInt(get("cache.ttl.minutes", "30")); // 30 минут кэш
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    public static int getRetryCount() {
        try {
            return Integer.parseInt(get("retry.count", "3")); // 3 попытки при ошибках
        } catch (NumberFormatException e) {
            return 3;
        }
    }
}