package com.planet3d.database;

import com.planet3d.model.Planet;

import java.sql.*;

/**
 * Quản lý kết nối và khởi tạo CSDL SQLite.
 */
public class DatabaseManager {

    private static final String DB_FILE = "planet3d.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        initialize();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            // Enable WAL mode for better performance
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA foreign_keys=ON");
            }
            createTables();
            seedDefaultData();
            System.out.println("[DB] Kết nối SQLite thành công: " + DB_FILE);
        } catch (Exception e) {
            throw new RuntimeException("Không thể khởi tạo CSDL: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kết nối CSDL", e);
        }
        return connection;
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {

            // Bảng hành tinh
            st.execute("""
                CREATE TABLE IF NOT EXISTS planets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    mass_kg REAL NOT NULL,
                    radius_m REAL NOT NULL,
                    texture_file TEXT,
                    rotation_period_s REAL DEFAULT 86400
                )
            """);

            // Bảng vệ tinh
            st.execute("""
                CREATE TABLE IF NOT EXISTS satellites (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    planet_id INTEGER NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    altitude_m REAL NOT NULL,
                    orbital_velocity REAL,
                    orbital_period_s REAL,
                    satellite_type TEXT DEFAULT 'COMMUNICATION',
                    color_hex TEXT DEFAULT '#00FF88',
                    active INTEGER DEFAULT 1,
                    FOREIGN KEY (planet_id) REFERENCES planets(id)
                )
            """);

            // Bảng kết quả định tuyến
            st.execute("""
                CREATE TABLE IF NOT EXISTS routing_results (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    planet_id INTEGER,
                    source_lat REAL, source_lon REAL, source_label TEXT,
                    dest_lat REAL, dest_lon REAL, dest_label TEXT,
                    path_json TEXT,
                    total_distance_km REAL,
                    hop_count INTEGER,
                    estimated_latency_ms REAL,
                    satellite_count INTEGER,
                    computation_time_ms INTEGER,
                    timestamp TEXT,
                    success INTEGER DEFAULT 1,
                    FOREIGN KEY (planet_id) REFERENCES planets(id)
                )
            """);

            System.out.println("[DB] Đã tạo các bảng CSDL");
        }
    }

    private void seedDefaultData() throws SQLException {
        // Cập nhật hoặc thêm dữ liệu mặc định
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM planets")) {
            // Không return sớm, vẫn chạy xuống dưới để cập nhật texture
        }

        System.out.println("[DB] Đang thêm dữ liệu mặc định...");

        // Thêm các hành tinh mặc định
        insertPlanet(Planet.EARTH());
        insertPlanet(Planet.MARS());
        insertPlanet(Planet.MOON());

        // Thêm vệ tinh mặc định cho Trái Đất (planet_id = 1)
        // Mô phỏng mạng LEO (Low Earth Orbit) ~550km như Starlink
        String[][] defaultSats = {
            {"SAT-A1", "1", "28.5",   "80.0",  "550000", "COMMUNICATION", "#00FFAA"},
            {"SAT-A2", "1", "0.0",    "0.0",   "550000", "COMMUNICATION", "#00FFAA"},
            {"SAT-A3", "1", "-28.5",  "-80.0", "550000", "COMMUNICATION", "#00FFAA"},
            {"SAT-B1", "1", "51.5",   "-0.1",  "560000", "COMMUNICATION", "#FF8800"},
            {"SAT-B2", "1", "40.7",   "-74.0", "560000", "COMMUNICATION", "#FF8800"},
            {"SAT-B3", "1", "-33.9",  "151.2", "560000", "COMMUNICATION", "#FF8800"},
            {"GPS-1",  "1", "55.0",   "37.6",  "20200000","GPS",          "#FFFF00"},
            {"GPS-2",  "1", "-20.0",  "100.0", "20200000","GPS",          "#FFFF00"},
            {"WS-1",   "1", "10.0",   "30.0",  "35786000","WEATHER",      "#88AAFF"},
        };

        // Bỏ qua thêm vệ tinh nếu bảng satellites đã có dữ liệu
        boolean hasSatellites = false;
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM satellites")) {
            if (rs.getInt(1) > 0) hasSatellites = true;
        }

        if (!hasSatellites) {
            String sql = "INSERT INTO satellites (name, planet_id, latitude, longitude, altitude_m, " +
                         "satellite_type, color_hex) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (String[] sat : defaultSats) {
                    ps.setString(1, sat[0]);
                    ps.setInt(2, Integer.parseInt(sat[1]));
                    ps.setDouble(3, Double.parseDouble(sat[2]));
                    ps.setDouble(4, Double.parseDouble(sat[3]));
                    ps.setDouble(5, Double.parseDouble(sat[4]));
                    ps.setString(6, sat[5]);
                    ps.setString(7, sat[6]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            System.out.println("[DB] Đã thêm " + defaultSats.length + " vệ tinh mặc định");
        }
    }

    private void insertPlanet(Planet p) throws SQLException {
        String sql = "INSERT OR IGNORE INTO planets (name, mass_kg, radius_m, texture_file, rotation_period_s) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setDouble(2, p.getMassKg());
            ps.setDouble(3, p.getRadiusM());
            ps.setString(4, p.getTextureFile());
            ps.setDouble(5, p.getRotationPeriodS());
            ps.execute();
        }
        
        // Cập nhật lại đường dẫn texture phòng trường hợp đã đổi trong code
        String updateSql = "UPDATE planets SET texture_file = ? WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setString(1, p.getTextureFile());
            ps.setString(2, p.getName());
            ps.execute();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[DB] Đã đóng kết nối CSDL");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
