package com.planet3d.physics;

import com.planet3d.model.Planet;
import com.planet3d.model.Satellite;

import java.util.List;

/**
 * Cập nhật vị trí vệ tinh theo thời gian mô phỏng.
 * Sử dụng rotation matrix để di chuyển vệ tinh trên quỹ đạo tròn.
 */
public class SatellitePosition {

    private double simulationTimeS = 0;
    private double timeMultiplier = 500.0; // Mô phỏng nhanh hơn thực tế 500 lần

    public SatellitePosition() {}

    public SatellitePosition(double timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
    }

    /**
     * Cập nhật vị trí của tất cả vệ tinh.
     *
     * @param satellites Danh sách vệ tinh cần cập nhật
     * @param planet     Hành tinh
     * @param dtSeconds  Delta time thực tế (giây)
     */
    public void update(List<Satellite> satellites, Planet planet, double dtSeconds) {
        simulationTimeS += dtSeconds * timeMultiplier;

        for (Satellite sat : satellites) {
            if (!sat.isActive() || sat.getOrbitalPeriodS() <= 0) continue;
            updateSatellitePosition(sat, planet);
        }
    }

    /**
     * Cập nhật vị trí một vệ tinh theo thời gian mô phỏng hiện tại.
     */
    public void updateSatellitePosition(Satellite sat, Planet planet) {
        if (sat.getOrbitalPeriodS() <= 0) {
            OrbitalMechanics.initializeSatellite(sat, planet);
        }

        // Góc quỹ đạo hiện tại (rad)
        double angle = (2.0 * Math.PI * simulationTimeS) / sat.getOrbitalPeriodS();
        sat.setCurrentAngle(angle);

        // Vị trí trên quỹ đạo: xoay vectơ ban đầu quanh trục orbit normal
        double[] initialPos = OrbitalMechanics.latLonAltToCartesian(
                sat.getLatitude(), sat.getLongitude(),
                sat.getAltitudeM(), planet.getRadiusM()
        );

        double[] normal = sat.getOrbitNormal();
        double[] rotated = rotateAroundAxis(initialPos, normal, angle);
        sat.setPosition3D(rotated);
    }

    /**
     * Xoay vector v quanh trục axis một góc angle (Rodrigues' rotation formula).
     *
     * @param v     Vector cần xoay
     * @param axis  Trục xoay (đơn vị)
     * @param angle Góc xoay (radian)
     * @return Vector đã xoay
     */
    private double[] rotateAroundAxis(double[] v, double[] axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        // k × v (cross product)
        double cx = axis[1] * v[2] - axis[2] * v[1];
        double cy = axis[2] * v[0] - axis[0] * v[2];
        double cz = axis[0] * v[1] - axis[1] * v[0];

        // k · v (dot product)
        double dot = axis[0] * v[0] + axis[1] * v[1] + axis[2] * v[2];

        // Rodrigues: v*cos + (k×v)*sin + k*(k·v)*(1-cos)
        double rx = v[0] * cos + cx * sin + axis[0] * dot * (1 - cos);
        double ry = v[1] * cos + cy * sin + axis[1] * dot * (1 - cos);
        double rz = v[2] * cos + cz * sin + axis[2] * dot * (1 - cos);

        return new double[]{rx, ry, rz};
    }

    /**
     * Reset thời gian mô phỏng về 0.
     */
    public void reset() {
        simulationTimeS = 0;
    }

    public double getSimulationTimeS() { return simulationTimeS; }
    public void setSimulationTimeS(double t) { this.simulationTimeS = t; }

    public double getTimeMultiplier() { return timeMultiplier; }
    public void setTimeMultiplier(double multiplier) { this.timeMultiplier = multiplier; }
}
