package com.planet3d.routing;

import com.planet3d.model.Planet;
import com.planet3d.model.RoutingResult;
import com.planet3d.model.Satellite;
import com.planet3d.physics.OrbitalMechanics;

import java.util.*;

/**
 * Thuật toán Dijkstra định tuyến thông tin qua mạng vệ tinh.
 * Tìm đường đi ngắn nhất (khoảng cách) từ Ground Station A đến B.
 */
public class DijkstraRouter {

    private final SatelliteGraph graph;
    private final Planet planet;

    public DijkstraRouter(SatelliteGraph graph, Planet planet) {
        this.graph = graph;
        this.planet = planet;
    }

    /**
     * Chạy thuật toán Dijkstra và trả về kết quả định tuyến.
     *
     * @param satellites Danh sách vệ tinh hiện tại
     * @return RoutingResult chứa đường đi và thông số
     */
    public RoutingResult route(List<Satellite> satellites) {
        long startTime = System.currentTimeMillis();

        RoutingResult result = new RoutingResult();
        result.setPlanetId(planet.getId());
        result.setSatelliteCount(satellites.size());

        Map<Integer, List<SatelliteGraph.Edge>> adj = graph.getAdjacency();

        // Dijkstra: dist[nodeId] = khoảng cách ngắn nhất từ A
        Map<Integer, Double> dist = new HashMap<>();
        Map<Integer, Integer> prev = new HashMap<>();
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> dist.getOrDefault(a[0], Double.MAX_VALUE)));

        // Khởi tạo
        for (int nodeId : adj.keySet()) {
            dist.put(nodeId, Double.MAX_VALUE);
            prev.put(nodeId, null);
        }
        dist.put(SatelliteGraph.GROUND_A_ID, 0.0);
        pq.offer(new int[]{SatelliteGraph.GROUND_A_ID});

        // Chạy Dijkstra
        Set<Integer> visited = new HashSet<>();
        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int u = current[0];

            if (visited.contains(u)) continue;
            visited.add(u);

            if (u == SatelliteGraph.GROUND_B_ID) break; // Đã đến đích

            List<SatelliteGraph.Edge> neighbors = adj.getOrDefault(u, Collections.emptyList());
            for (SatelliteGraph.Edge edge : neighbors) {
                int v = edge.targetId;
                if (visited.contains(v)) continue;

                double newDist = dist.get(u) + edge.distanceKm;
                if (newDist < dist.getOrDefault(v, Double.MAX_VALUE)) {
                    dist.put(v, newDist);
                    prev.put(v, u);
                    pq.offer(new int[]{v});
                }
            }
        }

        long endTime = System.currentTimeMillis();
        result.setComputationTimeMs(endTime - startTime);

        // Kiểm tra có đường không
        double totalDist = dist.getOrDefault(SatelliteGraph.GROUND_B_ID, Double.MAX_VALUE);
        if (totalDist == Double.MAX_VALUE) {
            result.setSuccess(false);
            result.setFailureReason("Không có đường đi: các vệ tinh không đủ độ phủ sóng giữa A và B");
            return result;
        }

        // Truy vết đường đi
        List<Integer> path = new ArrayList<>();
        List<String> pathNames = new ArrayList<>();

        Integer current = SatelliteGraph.GROUND_B_ID;
        while (current != null) {
            path.add(0, current);
            if (current == SatelliteGraph.GROUND_A_ID) {
                pathNames.add(0, graph.getGroundALabel());
            } else if (current == SatelliteGraph.GROUND_B_ID) {
                pathNames.add(0, graph.getGroundBLabel());
            } else {
                Satellite sat = graph.findSatellite(current);
                pathNames.add(0, sat != null ? sat.getName() : "SAT-" + current);
            }
            current = prev.get(current);
        }

        result.setSuccess(true);
        result.setSatellitePath(path);
        result.setSatelliteNames(pathNames);
        result.setTotalDistanceKm(totalDist);
        result.setHopCount(path.size() - 2); // Trừ 2 ground stations
        result.calculateLatency();

        return result;
    }

    /**
     * Chạy benchmark định tuyến với nhiều kịch bản vệ tinh khác nhau.
     *
     * @param allSatellites Danh sách đầy đủ vệ tinh
     * @param counts        Danh sách số lượng vệ tinh cần thử
     * @param planet        Hành tinh
     * @param aLat, aLon   Ground Station A
     * @param bLat, bLon   Ground Station B
     * @return Danh sách kết quả benchmark
     */
    public static List<RoutingResult> benchmark(
            List<Satellite> allSatellites, int[] counts, Planet planet,
            double aLat, double aLon, double bLat, double bLon) {

        List<RoutingResult> results = new ArrayList<>();

        for (int count : counts) {
            // Lấy tối đa 'count' vệ tinh từ danh sách
            List<Satellite> subset = allSatellites.subList(0, Math.min(count, allSatellites.size()));

            SatelliteGraph benchGraph = new SatelliteGraph(subset, planet);
            benchGraph.setGroundA(aLat, aLon, String.format("A(%.1f°,%.1f°)", aLat, aLon));
            benchGraph.setGroundB(bLat, bLon, String.format("B(%.1f°,%.1f°)", bLat, bLon));
            benchGraph.build();

            DijkstraRouter router = new DijkstraRouter(benchGraph, planet);
            RoutingResult r = router.route(subset);
            r.setSatelliteCount(subset.size());
            r.setSourceLat(aLat);
            r.setSourceLon(aLon);
            r.setSourceLabel(benchGraph.getGroundALabel());
            r.setDestLat(bLat);
            r.setDestLon(bLon);
            r.setDestLabel(benchGraph.getGroundBLabel());

            results.add(r);
        }

        return results;
    }
}
