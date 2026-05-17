package com.planet3d.physics;

import com.planet3d.model.Planet;
import com.planet3d.model.Satellite;

/**
 * Tính toán vật lý quỹ đạo vệ tinh.
 * Sử dụng cơ học Kepler cho quỹ đạo tròn.
 */
public class OrbitalMechanics {

    /**
     * Tính vận tốc quỹ đạo tròn.
     * v = sqrt(GM / r)
     *
     * @param planet   Hành tinh
     * @param altitudeM Độ cao so với bề mặt (m)
     * @return Vận tốc quỹ đạo (m/s)
     */
    public static double calculateCircularVelocity(Planet planet, double altitudeM) {
        double r = planet.getRadiusM() + altitudeM;
        return Math.sqrt(planet.getGM() / r);
    }

    /**
     * Tính chu kỳ quỹ đạo.
     * T = 2π * sqrt(r³ / GM)
     *
     * @param planet   Hành tinh
     * @param altitudeM Độ cao (m)
     * @return Chu kỳ quỹ đạo (giây)
     */
    public static double calculateOrbitalPeriod(Planet planet, double altitudeM) {
        double r = planet.getRadiusM() + altitudeM;
        return 2.0 * Math.PI * Math.sqrt(Math.pow(r, 3) / planet.getGM());
    }

    /**
     * Tính gia tốc rơi tự do tại độ cao.
     * g = GM / r²
     *
     * @param planet   Hành tinh
     * @param altitudeM Độ cao (m)
     * @return Gia tốc (m/s²)
     */
    public static double calculateGravitationalAcceleration(Planet planet, double altitudeM) {
        double r = planet.getRadiusM() + altitudeM;
        return planet.getGM() / (r * r);
    }

    /**
     * Chuyển tọa độ địa lý (lat, lon, alt) thành tọa độ Descartes 3D.
     * Hệ tọa độ: X = phải, Y = lên, Z = ra ngoài màn hình
     *
     * @param latDeg    Vĩ độ (độ)
     * @param lonDeg    Kinh độ (độ)
     * @param altitudeM Độ cao (m)
     * @param radiusM   Bán kính hành tinh (m)
     * @return double[3] {x, y, z} - tọa độ 3D (đơn vị: bán kính hành tinh = 1.0)
     */
    public static double[] latLonAltToCartesian(double latDeg, double lonDeg, double altitudeM, double radiusM) {
        double latRad = Math.toRadians(latDeg);
        double lonRad = Math.toRadians(lonDeg);
        double r = (radiusM + altitudeM) / radiusM; // Normalized radius

        // Hệ tọa độ đã điều chỉnh để khớp hoàn toàn với Texture 3D (Đông sang Phải)
        double x = -r * Math.cos(latRad) * Math.cos(lonRad);
        double y = r * Math.sin(latRad);
        double z = r * Math.cos(latRad) * Math.sin(lonRad);

        return new double[]{x, y, z};
    }

    /**
     * Tính khoảng cách 3D giữa 2 điểm trong không gian.
     *
     * @param p1 Điểm 1 [x, y, z]
     * @param p2 Điểm 2 [x, y, z]
     * @return Khoảng cách (cùng đơn vị với đầu vào)
     */
    public static double distance3D(double[] p1, double[] p2) {
        double dx = p1[0] - p2[0];
        double dy = p1[1] - p2[1];
        double dz = p1[2] - p2[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Tính khoảng cách 3D thực (km) giữa 2 vệ tinh.
     */
    public static double distanceKm(double[] p1, double[] p2, double radiusM) {
        double radiusKm = radiusM / 1000.0;
        double dx = (p1[0] - p2[0]) * radiusKm;
        double dy = (p1[1] - p2[1]) * radiusKm;
        double dz = (p1[2] - p2[2]) * radiusKm;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Kiểm tra Line-of-Sight giữa 2 vệ tinh.
     * Hai vệ tinh có thể liên lạc khi đường thẳng giữa chúng không đi qua hành tinh.
     *
     * @param p1      Vị trí vệ tinh 1 (normalized: bán kính hành tinh = 1.0)
     * @param p2      Vị trí vệ tinh 2 (normalized)
     * @return true nếu có tầm nhìn thẳng
     */
    public static boolean hasLineOfSight(double[] p1, double[] p2) {
        // Kiểm tra xem đoạn thẳng p1-p2 có giao với hình cầu đơn vị không
        double dx = p2[0] - p1[0];
        double dy = p2[1] - p1[1];
        double dz = p2[2] - p1[2];

        double a = dx * dx + dy * dy + dz * dz;
        double b = 2 * (p1[0] * dx + p1[1] * dy + p1[2] * dz);
        double c = p1[0] * p1[0] + p1[1] * p1[1] + p1[2] * p1[2] - 1.0;

        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) return true; // Không giao → có LOS

        double sqrtD = Math.sqrt(discriminant);
        double t1 = (-b - sqrtD) / (2 * a);
        double t2 = (-b + sqrtD) / (2 * a);

        // Nếu cả 2 giao điểm nằm trong đoạn [0,1] thì bị chặn
        return !(t1 > 0.001 && t1 < 0.999 && t2 > 0.001 && t2 < 0.999);
    }

    /**
     * Khởi tạo vị trí và thông số quỹ đạo cho vệ tinh.
     *
     * @param satellite Vệ tinh cần khởi tạo
     * @param planet    Hành tinh
     */
    public static void initializeSatellite(Satellite satellite, Planet planet) {
        double v = calculateCircularVelocity(planet, satellite.getAltitudeM());
        double T = calculateOrbitalPeriod(planet, satellite.getAltitudeM());

        satellite.setOrbitalVelocity(v);
        satellite.setOrbitalPeriodS(T);

        // Tính vị trí 3D ban đầu
        double[] pos = latLonAltToCartesian(
                satellite.getLatitude(),
                satellite.getLongitude(),
                satellite.getAltitudeM(),
                planet.getRadiusM()
        );
        satellite.setPosition3D(pos);

        // Tính trục quỹ đạo: vuông góc với vectơ vị trí trong mặt phẳng kinh tuyến
        double[] normal = computeOrbitNormal(satellite.getLatitude(), satellite.getLongitude());
        satellite.setOrbitNormal(normal);
    }

    /**
     * Tính vector pháp tuyến trục quỹ đạo từ lat/lon ban đầu.
     * Tạo mặt phẳng quỹ đạo nghiêng theo vĩ độ ban đầu.
     */
    private static double[] computeOrbitNormal(double latDeg, double lonDeg) {
        double incl = Math.toRadians(latDeg); // Góc nghiêng
        double lon = Math.toRadians(lonDeg);

        // Normal vector của mặt phẳng quỹ đạo (vuông góc với position và East vector)
        double nx = Math.sin(incl) * Math.cos(lon);
        double ny = Math.cos(incl);
        double nz = -Math.sin(incl) * Math.sin(lon);

        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) { nx /= len; ny /= len; nz /= len; }
        return new double[]{nx, ny, nz};
    }

    /**
     * Thông tin debug vệ tinh
     */
    public static String getSatelliteInfo(Satellite sat, Planet planet) {
        double v = sat.getOrbitalVelocity();
        double T = sat.getOrbitalPeriodS();
        double g = calculateGravitationalAcceleration(planet, sat.getAltitudeM());
        double r = planet.getRadiusM() + sat.getAltitudeM();
        return String.format(
            "=== %s ===\n" +
            "Hành tinh: %s\n" +
            "Vĩ độ: %.2f°  Kinh độ: %.2f°\n" +
            "Độ cao: %.1f km\n" +
            "Bán kính quỹ đạo: %.1f km\n" +
            "Vận tốc quỹ đạo: %.2f m/s (%.2f km/h)\n" +
            "Chu kỳ quỹ đạo: %.1f phút (%.2f giờ)\n" +
            "Gia tốc hấp dẫn: %.4f m/s²",
            sat.getName(),
            planet.getName(),
            sat.getLatitude(), sat.getLongitude(),
            sat.getAltitudeM() / 1000.0,
            r / 1000.0,
            v, v * 3.6,
            T / 60.0, T / 3600.0,
            g
        );
    }
}
