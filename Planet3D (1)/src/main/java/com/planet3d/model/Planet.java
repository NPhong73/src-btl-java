package com.planet3d.model;

/**
 * Mô hình dữ liệu hành tinh.
 * Chứa các thông số vật lý của hành tinh để tính toán quỹ đạo.
 */
public class Planet {
    private int id;
    private String name;
    private double massKg;          // Khối lượng (kg)
    private double radiusM;         // Bán kính (m)
    private String textureFile;     // Đường dẫn file texture
    private double rotationPeriodS; // Chu kỳ tự quay (giây)

    // Hằng số hấp dẫn G * M cho các hành tinh phổ biến
    public static final double G = 6.6743e-11; // m³/(kg·s²)

    public Planet() {}

    public Planet(String name, double massKg, double radiusM, String textureFile, double rotationPeriodS) {
        this.name = name;
        this.massKg = massKg;
        this.radiusM = radiusM;
        this.textureFile = textureFile;
        this.rotationPeriodS = rotationPeriodS;
    }

    /** Tính GM (tham số hấp dẫn tiêu chuẩn) */
    public double getGM() {
        return G * massKg;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getMassKg() { return massKg; }
    public void setMassKg(double massKg) { this.massKg = massKg; }

    public double getRadiusM() { return radiusM; }
    public void setRadiusM(double radiusM) { this.radiusM = radiusM; }

    public String getTextureFile() { return textureFile; }
    public void setTextureFile(String textureFile) { this.textureFile = textureFile; }

    public double getRotationPeriodS() { return rotationPeriodS; }
    public void setRotationPeriodS(double rotationPeriodS) { this.rotationPeriodS = rotationPeriodS; }

    @Override
    public String toString() {
        return name;
    }

    // ===================== DỮ LIỆU HÀNH TINH MẶC ĐỊNH =====================

    /** Trái Đất - texture 8K từ thư mục docs */
    public static Planet EARTH() {
        return new Planet("Earth", 5.972e24, 6_371_000, "docs/8k_earth_daymap.jpg", 86_164.1);
    }

    /** Sao Hỏa */
    public static Planet MARS() {
        return new Planet("Mars", 6.39e23, 3_389_500, "docs/mars.jpg", 88_642.0);
    }

    /** Sao Mộc */
    public static Planet JUPITER() {
        return new Planet("Jupiter", 1.898e27, 69_911_000, "jupiter.jpg", 35_730.0);
    }

    /** Mặt Trăng */
    public static Planet MOON() {
        return new Planet("Moon", 7.342e22, 1_737_400, "docs/moon.jpg", 2_360_591.5);
    }
}
