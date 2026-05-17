package com.planet3d.model;

/**
 * Mô hình dữ liệu vệ tinh.
 * Chứa vị trí ban đầu, thông số quỹ đạo và trạng thái hiện tại.
 */
public class Satellite {
    private int id;
    private String name;
    private int planetId;

    // Vị trí ban đầu (geodetic coordinates)
    private double latitude;    // Vĩ độ (độ, -90 đến +90)
    private double longitude;   // Kinh độ (độ, -180 đến +180)
    private double altitudeM;   // Độ cao so với bề mặt (m)

    // Thông số quỹ đạo
    private double orbitalVelocity; // Vận tốc quỹ đạo (m/s)
    private double orbitalPeriodS;  // Chu kỳ quỹ đạo (giây)

    // Loại vệ tinh
    private String satelliteType;   // COMMUNICATION, OBSERVATION, WEATHER, GPS
    private String colorHex;        // Màu hiển thị (hex: #RRGGBB)
    private boolean active;

    // Vị trí 3D hiện tại (tính toán runtime, không lưu DB)
    private double currentAngle = 0; // Góc quỹ đạo hiện tại (radian)
    private double[] position3D = new double[3]; // [x, y, z] trong không gian 3D

    // Trục quỹ đạo (tính từ lat/lon ban đầu)
    private double[] orbitNormal = new double[3];

    public Satellite() {
        this.active = true;
        this.colorHex = "#00FF88";
        this.satelliteType = "COMMUNICATION";
    }

    public Satellite(String name, int planetId, double latitude, double longitude,
                     double altitudeM, String satelliteType, String colorHex) {
        this();
        this.name = name;
        this.planetId = planetId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeM = altitudeM;
        this.satelliteType = satelliteType;
        this.colorHex = colorHex;
    }

    /** Chuyển màu hex thành float[] [r, g, b] (0.0 - 1.0) */
    public float[] getColorRGB() {
        String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new float[]{r / 255.0f, g / 255.0f, b / 255.0f};
        } catch (Exception e) {
            return new float[]{0.0f, 1.0f, 0.5f};
        }
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPlanetId() { return planetId; }
    public void setPlanetId(int planetId) { this.planetId = planetId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getAltitudeM() { return altitudeM; }
    public void setAltitudeM(double altitudeM) { this.altitudeM = altitudeM; }

    public double getOrbitalVelocity() { return orbitalVelocity; }
    public void setOrbitalVelocity(double orbitalVelocity) { this.orbitalVelocity = orbitalVelocity; }

    public double getOrbitalPeriodS() { return orbitalPeriodS; }
    public void setOrbitalPeriodS(double orbitalPeriodS) { this.orbitalPeriodS = orbitalPeriodS; }

    public String getSatelliteType() { return satelliteType; }
    public void setSatelliteType(String satelliteType) { this.satelliteType = satelliteType; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public double getCurrentAngle() { return currentAngle; }
    public void setCurrentAngle(double currentAngle) { this.currentAngle = currentAngle; }

    public double[] getPosition3D() { return position3D; }
    public void setPosition3D(double[] position3D) { this.position3D = position3D; }

    public double[] getOrbitNormal() { return orbitNormal; }
    public void setOrbitNormal(double[] orbitNormal) { this.orbitNormal = orbitNormal; }

    @Override
    public String toString() {
        return String.format("%s [%s] alt=%.0fkm v=%.0fm/s",
                name, satelliteType, altitudeM / 1000.0, orbitalVelocity);
    }
}
