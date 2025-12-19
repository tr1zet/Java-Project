package main.java.com.teamweather.model;

public class City {
    private int id; // ID в базе данных
    private String name; // Название города
    private double latitude; // Широта
    private double longitude; // Долгота
    private String country; // Страна
    private String state; // Регион/штат

    // Конструкторы
    public City() {}

    public City(String name, double latitude, double longitude, String country) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.country = country;
    }

    // Геттеры
    public int getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getCountry() { return country; }
    public String getState() { return state; }

    // Сеттеры
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setCountry(String country) { this.country = country; }
    public void setState(String state) { this.state = state; }

    // Форматированный вывод
    @Override
    public String toString() {
        if (state != null && !state.isEmpty()) {
            return name + ", " + state + ", " + country;
        }
        return name + ", " + country;
    }

    // Красивое отображение для UI
    public String getDisplayName() {
        if (state != null && !state.isEmpty()) {
            return name + " (" + state + ", " + country + ")";
        }
        return name + " (" + country + ")";
    }
}