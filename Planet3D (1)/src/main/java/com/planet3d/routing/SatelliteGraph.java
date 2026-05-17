package com.planet3d.routing;

import com.planet3d.model.Planet;
import com.planet3d.model.Satellite;
import com.planet3d.physics.OrbitalMechanics;

import java.util.*;

/**
 * Xây dựng đồ thị mạng vệ tinh.
 * Node: vệ tinh + 2 ground stations (A và B)
 * Edge: khi 2 node có tầm nhìn thẳng (Line-of-Sight)
 */
public class SatelliteGraph {

    // ID đặc biệt cho ground stations
    public static final int GROUND_A_ID = -1;
    public static final int GROUND_B_ID = -2;

    private final List<Satellite> satellites;
    private final Planet planet;

    // Ground stations
    private double[] groundAPos;  // 3D position
    private double[] groundBPos;

    private String groundALabel = "Point A";
    private String groundBLabel = "Point B";

    // Adjacency list: nodeId → list of (neighborId, distanceKm)
    private final Map<Integer, List<Edge>> adjacency = new HashMap<>();

    public static class Edge {
        public final int targetId;
        public final double distanceKm;

        public Edge(int targetId, double distanceKm) {
            this.targetId = targetId;
            this.distanceKm = distanceKm;
        }
    }

    public SatelliteGraph(List<Satellite> satellites, Planet planet) {
        this.satellites = satellites;
        this.planet = planet;
    }

    /**
     * Đặt vị trí Ground Station A (điểm nguồn).
     */
    public void setGroundA(double latDeg, double lonDeg, String label) {
        this.groundAPos = OrbitalMechanics.latLonAltToCartesian(latDeg, lonDeg, 0, planet.getRadiusM());
        this.groundALabel = label;
    }

    /**
     * Đặt vị trí Ground Station B (điểm đích).
     */
    public void setGroundB(double latDeg, double lonDeg, String label) {
        this.groundBPos = OrbitalMechanics.latLonAltToCartesian(latDeg, lonDeg, 0, planet.getRadiusM());
        this.groundBLabel = label;
    }

    /**
     * Xây dựng đồ thị từ danh sách vệ tinh hiện tại.
     * Phải gọi setGroundA và setGroundB trước.
     */
    public void build() {
        adjacency.clear();

        if (groundAPos == null || groundBPos == null) {
            throw new IllegalStateException("Chưa đặt vị trí Ground Station A và B");
        }

        // Thêm tất cả node vào adjacency list
        for (Satellite sat : satellites) {
            if (sat.isActive()) {
                adjacency.put(sat.getId(), new ArrayList<>());
            }
        }
        adjacency.put(GROUND_A_ID, new ArrayList<>());
        adjacency.put(GROUND_B_ID, new ArrayList<>());

        // Danh sách các node với vị trí
        List<int[]> nodeIds = new ArrayList<>();
        for (Satellite sat : satellites) {
            if (sat.isActive()) nodeIds.add(new int[]{sat.getId()});
        }

        // Xây dựng edges: kiểm tra LOS giữa từng cặp
        List<NodeInfo> allNodes = new ArrayList<>();
        for (Satellite sat : satellites) {
            if (sat.isActive()) {
                allNodes.add(new NodeInfo(sat.getId(), sat.getPosition3D()));
            }
        }
        allNodes.add(new NodeInfo(GROUND_A_ID, groundAPos));
        allNodes.add(new NodeInfo(GROUND_B_ID, groundBPos));

        for (int i = 0; i < allNodes.size(); i++) {
            NodeInfo n1 = allNodes.get(i);
            for (int j = i + 1; j < allNodes.size(); j++) {
                NodeInfo n2 = allNodes.get(j);

                // Ground stations chỉ cần LOS với vệ tinh, không phải với nhau
                if (n1.id == GROUND_A_ID && n2.id == GROUND_B_ID) continue;
                if (n1.id == GROUND_B_ID && n2.id == GROUND_A_ID) continue;

                if (OrbitalMechanics.hasLineOfSight(n1.pos, n2.pos)) {
                    double distKm = OrbitalMechanics.distanceKm(n1.pos, n2.pos, planet.getRadiusM());

                    adjacency.get(n1.id).add(new Edge(n2.id, distKm));
                    adjacency.get(n2.id).add(new Edge(n1.id, distKm));
                }
            }
        }
    }

    public Map<Integer, List<Edge>> getAdjacency() { return adjacency; }

    public String getGroundALabel() { return groundALabel; }
    public String getGroundBLabel() { return groundBLabel; }

    public int getSatelliteCount() { return satellites.size(); }
    public int getEdgeCount() {
        return adjacency.values().stream().mapToInt(List::size).sum() / 2;
    }

    /** Tìm vệ tinh theo ID */
    public Satellite findSatellite(int id) {
        return satellites.stream().filter(s -> s.getId() == id).findFirst().orElse(null);
    }

    private static class NodeInfo {
        final int id;
        final double[] pos;
        NodeInfo(int id, double[] pos) { this.id = id; this.pos = pos; }
    }
}
