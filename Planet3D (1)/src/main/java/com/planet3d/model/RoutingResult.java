package com.planet3d.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kết quả định tuyến từ điểm A đến điểm B qua mạng vệ tinh.
 */
public class RoutingResult {
    private int id;
    private int planetId;

    // Điểm nguồn (A)
    private double sourceLat;
    private double sourceLon;
    private String sourceLabel;

    // Điểm đích (B)
    private double destLat;
    private double destLon;
    private String destLabel;

    // Kết quả
    private List<Integer> satellitePath;    // Danh sách ID vệ tinh theo thứ tự
    private List<String> satelliteNames;    // Tên vệ tinh theo thứ tự
    private double totalDistanceKm;         // Tổng khoảng cách (km)
    private int hopCount;                   // Số lần chuyển tiếp
    private double estimatedLatencyMs;      // Độ trễ ước tính (ms)
    private int satelliteCount;             // Tổng số vệ tinh trong mạng
    private long computationTimeMs;         // Thời gian tính (ms)
    private String timestamp;
    private boolean success;
    private String failureReason;

    public RoutingResult() {
        this.timestamp = LocalDateTime.now().toString();
        this.success = false;
    }

    /** Tốc độ ánh sáng trong không gian (km/s) */
    private static final double LIGHT_SPEED_KM_S = 299_792.458;

    /** Tính độ trễ từ tổng khoảng cách (giả định truyền qua sóng radio) */
    public void calculateLatency() {
        // Radio signals travel at ~speed of light, add processing delay per hop
        double propagationMs = (totalDistanceKm / LIGHT_SPEED_KM_S) * 1000.0;
        double processingMs = hopCount * 5.0; // 5ms processing per hop
        this.estimatedLatencyMs = propagationMs + processingMs;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPlanetId() { return planetId; }
    public void setPlanetId(int planetId) { this.planetId = planetId; }

    public double getSourceLat() { return sourceLat; }
    public void setSourceLat(double sourceLat) { this.sourceLat = sourceLat; }

    public double getSourceLon() { return sourceLon; }
    public void setSourceLon(double sourceLon) { this.sourceLon = sourceLon; }

    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }

    public double getDestLat() { return destLat; }
    public void setDestLat(double destLat) { this.destLat = destLat; }

    public double getDestLon() { return destLon; }
    public void setDestLon(double destLon) { this.destLon = destLon; }

    public String getDestLabel() { return destLabel; }
    public void setDestLabel(String destLabel) { this.destLabel = destLabel; }

    public List<Integer> getSatellitePath() { return satellitePath; }
    public void setSatellitePath(List<Integer> satellitePath) { this.satellitePath = satellitePath; }

    public List<String> getSatelliteNames() { return satelliteNames; }
    public void setSatelliteNames(List<String> satelliteNames) { this.satelliteNames = satelliteNames; }

    public double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }

    public int getHopCount() { return hopCount; }
    public void setHopCount(int hopCount) { this.hopCount = hopCount; }

    public double getEstimatedLatencyMs() { return estimatedLatencyMs; }
    public void setEstimatedLatencyMs(double estimatedLatencyMs) { this.estimatedLatencyMs = estimatedLatencyMs; }

    public int getSatelliteCount() { return satelliteCount; }
    public void setSatelliteCount(int satelliteCount) { this.satelliteCount = satelliteCount; }

    public long getComputationTimeMs() { return computationTimeMs; }
    public void setComputationTimeMs(long computationTimeMs) { this.computationTimeMs = computationTimeMs; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    @Override
    public String toString() {
        if (!success) return "Routing FAILED: " + failureReason;
        return String.format("Route: %s → %s | Hops: %d | Dist: %.1f km | Latency: %.1f ms",
                sourceLabel, destLabel, hopCount, totalDistanceKm, estimatedLatencyMs);
    }
}
