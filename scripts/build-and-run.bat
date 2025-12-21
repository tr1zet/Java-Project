@echo off
echo =========================================
echo      Weather App - Сборка и запуск
echo =========================================
echo.

echo 🔍 Проверка Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Java не установлена!
    echo    Скачайте: https://adoptium.net/
    pause
    exit /b 1
)
echo ✅ Java установлена

echo 🔍 Проверка Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ Maven не установлен!
    echo    Скачайте: https://maven.apache.org/
    pause
    exit /b 1
)
echo ✅ Maven установлен

echo 🔍 Проверка конфигурации...
if not exist "src\main\resources\config.properties" (
    echo ❌ Файл config.properties не найден!
    pause
    exit /b 1
)

for /f "tokens=2 delims==" %%i in ('findstr "api.key" src\main\resources\config.properties') do set API_KEY=%%i
if "%API_KEY%"=="" (
    echo ⚠️  API ключ не настроен!
    echo    Отредактируйте: src\main\resources\config.properties
    echo    Получите ключ: https://openweathermap.org/api
    set /p CONTINUE=Продолжить без API ключа? (y/N):
    if /i not "%CONTINUE%"=="y" (
        pause
        exit /b 1
    )
) else if "%API_KEY%"=="ВАШ_API_КЛЮЧ_ЗДЕСЬ" (
    echo ⚠️  API ключ не настроен!
    echo    Отредактируйте: src\main\resources\config.properties
    echo    Получите ключ: https://openweathermap.org/api
    set /p CONTINUE=Продолжить без API ключа? (y/N):
    if /i not "%CONTINUE%"=="y" (
        pause
        exit /b 1
    )
) else (
    echo ✅ API ключ настроен
)

echo.
echo 🛠️  Сборка проекта...
call mvn clean compile

if errorlevel 1 (
    echo ❌ Ошибка сборки!
    pause
    exit /b 1
)
echo ✅ Проект успешно собран

echo.
echo 🧪 Запуск тестов...
call mvn test

if errorlevel 1 (
    echo ⚠️  Некоторые тесты не прошли
    set /p CONTINUE_TESTS=Продолжить запуск приложения? (y/N):
    if /i not "%CONTINUE_TESTS%"=="y" (
        pause
        exit /b 1
    )
) else (
    echo ✅ Все тесты пройдены успешно
)

echo.
echo 🚀 Запуск приложения...
echo =========================================
call mvn exec:java -Dexec.mainClass="com.teamweather.MainApp"

pause