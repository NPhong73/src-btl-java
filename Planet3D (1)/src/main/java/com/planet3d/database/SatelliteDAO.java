package com.planet3d.database;

import com.planet3d.model.Planet;
import com.planet3d.model.RoutingResult;
import com.planet3d.model.Satellite;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object cho vệ tinh và kết quả định tuyến.
 */
public class SatelliteDAO {

    private final DatabaseManager dbManager;
    private final Gson gson = new Gson();

    public SatelliteDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }

    // ===================== PLANET OPERATIONS =====================

    public List<Planet> getAllPlanets() {
        List<Planet> list = new ArrayList<>();
        String sql = "SELECT id, name, mass_kg, radius_m, texture_file, rotation_period_s FROM planets ORDER BY id";
        try (Statement st = dbManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Planet p = new Planet();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setMassKg(rs.getDouble("mass_kg"));
                p.setRadiusM(rs.getDouble("radius_m"));
                p.setTextureFile(rs.getString("texture_file"));
                p.setRotationPeriodS(rs.getDouble("rotation_period_s"));
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Planet getPlanetById(int planetId) {
        String sql = "SELECT id, name, mass_kg, radius_m, texture_file, rotation_period_s FROM planets WHERE id=?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, planetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Planet p = new Planet();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setMassKg(rs.getDouble("mass_kg"));
                p.setRadiusM(rs.getDouble("radius_m"));
                p.setTextureFile(rs.getString("texture_file"));
                p.setRotationPeriodS(rs.getDouble("rotation_period_s"));
                return p;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ===================== SATELLITE OPERATIONS =====================

    public List<Satellite> getSatellitesByPlanet(int planetId) {
        List<Satellite> list = new ArrayList<>();
        String sql = "SELECT * FROM satellites WHERE planet_id=? AND active=1 ORDER BY id";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, planetId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapSatellite(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public Satellite getSatelliteById(int id) {
        String sql = "SELECT * FROM satellites WHERE id=?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapSatellite(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int insertSatellite(Satellite sat) {
        String sql = "INSERT INTO satellites (name, planet_id, latitude, longitude, altitude_m, " +
                     "orbital_velocity, orbital_period_s, satellite_type, color_hex, active) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sat.getName());
            ps.setInt(2, sat.getPlanetId());
            ps.setDouble(3, sat.getLatitude());
            ps.setDouble(4, sat.getLongitude());
            ps.setDouble(5, sat.getAltitudeM());
            ps.setDouble(6, sat.getOrbitalVelocity());
            ps.setDouble(7, sat.getOrbitalPeriodS());
            ps.setString(8, sat.getSatelliteType());
            ps.setString(9, sat.getColorHex());
            ps.setInt(10, sat.isActive() ? 1 : 0);
            ps.execute();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                sat.setId(id);
                return id;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean updateSatellite(Satellite sat) {
        String sql = "UPDATE satellites SET name=?, latitude=?, longitude=?, altitude_m=?, " +
                     "orbital_velocity=?, orbital_period_s=?, satellite_type=?, color_hex=?, active=? WHERE id=?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, sat.getName());
            ps.setDouble(2, sat.getLatitude());
            ps.setDouble(3, sat.getLongitude());
            ps.setDouble(4, sat.getAltitudeM());
            ps.setDouble(5, sat.getOrbitalVelocity());
            ps.setDouble(6, sat.getOrbitalPeriodS());
            ps.setString(7, sat.getSatelliteType());
            ps.setString(8, sat.getColorHex());
            ps.setInt(9, sat.isActive() ? 1 : 0);
            ps.setInt(10, sat.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteSatellite(int id) {
        String sql = "UPDATE satellites SET active=0 WHERE id=?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean hardDeleteSatellite(int id) {
        String sql = "DELETE FROM satellites WHERE id=?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Satellite mapSatellite(ResultSet rs) throws SQLException {
        Satellite s = new Satellite();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setPlanetId(rs.getInt("planet_id"));
        s.setLatitude(rs.getDouble("latitude"));
        s.setLongitude(rs.getDouble("longitude"));
        s.setAltitudeM(rs.getDouble("altitude_m"));
        s.setOrbitalVelocity(rs.getDouble("orbital_velocity"));
        s.setOrbitalPeriodS(rs.getDouble("orbital_period_s"));
        s.setSatelliteType(rs.getString("satellite_type"));
        s.setColorHex(rs.getString("color_hex"));
        s.setActive(rs.getInt("active") == 1);
        return s;
    }

    // ===================== ROUTING RESULTS =====================

    public void saveRoutingResult(RoutingResult result) {
        String sql = "INSERT INTO routing_results (planet_id, source_lat, source_lon, source_label, " +
                     "dest_lat, dest_lon, dest_label, path_json, total_distance_km, hop_count, " +
                     "estimated_latency_ms, satellite_count, computation_time_ms, timestamp, success) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, result.getPlanetId());
            ps.setDouble(2, result.getSourceLat());
            ps.setDouble(3, result.getSourceLon());
            ps.setString(4, result.getSourceLabel());
            ps.setDouble(5, result.getDestLat());
            ps.setDouble(6, result.getDestLon());
            ps.setString(7, result.getDestLabel());
            ps.setString(8, gson.toJson(result.getSatellitePath()));
            ps.setDouble(9, result.getTotalDistanceKm());
            ps.setInt(10, result.getHopCount());
            ps.setDouble(11, result.getEstimatedLatencyMs());
            ps.setInt(12, result.getSatelliteCount());
            ps.setLong(13, result.getComputationTimeMs());
            ps.setString(14, result.getTimestamp());
            ps.setInt(15, result.isSuccess() ? 1 : 0);
            ps.execute();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) result.setId(keys.getInt(1));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<RoutingResult> getRoutingHistory(int planetId, int limit) {
        List<RoutingResult> list = new ArrayList<>();
        String sql = "SELECT * FROM routing_results WHERE planet_id=? ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, planetId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                RoutingResult r = new RoutingResult();
                r.setId(rs.getInt("id"));
                r.setPlanetId(rs.getInt("planet_id"));
                r.setSourceLat(rs.getDouble("source_lat"));
                r.setSourceLon(rs.getDouble("source_lon"));
                r.setSourceLabel(rs.getString("source_label"));
                r.setDestLat(rs.getDouble("dest_lat"));
                r.setDestLon(rs.getDouble("dest_lon"));
                r.setDestLabel(rs.getString("dest_label"));
                r.setTotalDistanceKm(rs.getDouble("total_distance_km"));
                r.setHopCount(rs.getInt("hop_count"));
                r.setEstimatedLatencyMs(rs.getDouble("estimated_latency_ms"));
                r.setSatelliteCount(rs.getInt("satellite_count"));
                r.setComputationTimeMs(rs.getLong("computation_time_ms"));
                r.setTimestamp(rs.getString("timestamp"));
                r.setSuccess(rs.getInt("success") == 1);
                String pathJson = rs.getString("path_json");
                if (pathJson != null) {
                    r.setSatellitePath(gson.fromJson(pathJson, new TypeToken<List<Integer>>(){}.getType()));
                }
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void clearRoutingHistory(int planetId) {
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(
                "DELETE FROM routing_results WHERE planet_id=?")) {
            ps.setInt(1, planetId);
            ps.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
