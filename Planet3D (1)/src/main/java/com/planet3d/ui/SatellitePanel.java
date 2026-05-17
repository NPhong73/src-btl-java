package com.planet3d.ui;

import com.planet3d.database.SatelliteDAO;
import com.planet3d.model.Planet;
import com.planet3d.model.Satellite;
import com.planet3d.physics.OrbitalMechanics;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Panel quản lý vệ tinh: xem, thêm, sửa, xóa vệ tinh trong CSDL.
 */
public class SatellitePanel extends JPanel {

    private final SatelliteDAO dao = new SatelliteDAO();
    private Planet currentPlanet;
    private PlanetPanel planetPanel;

    // Table
    private DefaultTableModel tableModel;
    private JTable table;

    // Form fields
    private JTextField tfName, tfLat, tfLon, tfAlt;
    private JComboBox<String> cbType;
    private JButton btnColor;
    private Color selectedColor = new Color(0, 255, 136);

    // Info display
    private JTextArea taInfo;

    // Listener callback
    private Runnable onSatelliteChanged;

    private static final String[] COLUMNS = {"ID", "Tên", "Loại", "Vĩ độ", "Kinh độ", "Độ cao (km)", "Vận tốc (m/s)"};
    private static final String[] SAT_TYPES = {"COMMUNICATION", "OBSERVATION", "WEATHER", "GPS", "MILITARY"};

    public SatellitePanel(PlanetPanel planetPanel) {
        this.planetPanel = planetPanel;
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(18, 22, 36));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buildUI();
    }

    private void buildUI() {
        // === TABLE PANEL ===
        String[] cols = COLUMNS;
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setBackground(new Color(25, 30, 50));
        table.setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(60, 100, 180));
        table.setSelectionForeground(Color.WHITE);
        table.setGridColor(new Color(40, 50, 80));
        table.setFont(new Font("Consolas", Font.PLAIN, 12));
        table.getTableHeader().setBackground(new Color(30, 40, 70));
        table.getTableHeader().setForeground(new Color(150, 180, 255));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setRowHeight(22);

        // Hide ID column
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(0, 200));
        scrollPane.setBorder(createDarkTitledBorder("Danh sách Vệ tinh"));
        scrollPane.getViewport().setBackground(new Color(25, 30, 50));

        // === FORM PANEL ===
        JPanel formPanel = createFormPanel();

        // === INFO PANEL ===
        taInfo = new JTextArea(6, 0);
        taInfo.setEditable(false);
        taInfo.setFont(new Font("Consolas", Font.PLAIN, 11));
        taInfo.setBackground(new Color(15, 20, 35));
        taInfo.setForeground(new Color(100, 220, 130));
        taInfo.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane infoScroll = new JScrollPane(taInfo);
        infoScroll.setBorder(createDarkTitledBorder("Thông tin quỹ đạo"));

        // === BUTTON BAR ===
        JPanel btnPanel = createButtonPanel();

        // Layout
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setOpaque(false);
        topPanel.add(scrollPane, BorderLayout.CENTER);
        topPanel.add(btnPanel, BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, formPanel);
        split.setDividerLocation(200);
        split.setOpaque(false);
        split.setBorder(null);

        add(split, BorderLayout.CENTER);
        add(infoScroll, BorderLayout.SOUTH);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(20, 26, 45));
        panel.setBorder(createDarkTitledBorder("Thêm / Sửa Vệ tinh"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.anchor = GridBagConstraints.WEST;

        tfName = darkTextField("VT-001");
        tfLat  = darkTextField("0.0");
        tfLon  = darkTextField("0.0");
        tfAlt  = darkTextField("550");
        cbType = new JComboBox<>(SAT_TYPES);
        cbType.setBackground(new Color(30, 40, 70));
        cbType.setForeground(Color.WHITE);
        cbType.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        btnColor = new JButton("Màu");
        btnColor.setBackground(selectedColor);
        btnColor.setForeground(Color.WHITE);
        btnColor.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnColor.setBorderPainted(false);
        btnColor.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnColor.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, "Chọn màu vệ tinh", selectedColor);
            if (c != null) { selectedColor = c; btnColor.setBackground(c); }
        });

        // Add form fields
        addFormRow(panel, gbc, 0, "Tên:", tfName);
        addFormRow(panel, gbc, 1, "Vĩ độ (°):", tfLat);
        addFormRow(panel, gbc, 2, "Kinh độ (°):", tfLon);
        addFormRow(panel, gbc, 3, "Độ cao (km):", tfAlt);
        addFormRow(panel, gbc, 4, "Loại:", cbType);
        addFormRow(panel, gbc, 5, "Màu:", btnColor);

        // Velocity display
        JButton btnCalc = new JButton("Tính vận tốc");
        styleButton(btnCalc, new Color(50, 120, 200));
        btnCalc.addActionListener(e -> calculateAndShowVelocity());

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(btnCalc, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 0, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        JButton btnAdd    = createIconButton("Thêm",  new Color(40, 130, 70));
        JButton btnUpdate = createIconButton("Sửa",   new Color(60, 100, 180));
        JButton btnDelete = createIconButton("Xóa",   new Color(160, 40, 40));
        JButton btnRefresh= createIconButton("Làm mới", new Color(60, 70, 100));

        btnAdd.addActionListener(e -> addSatellite());
        btnUpdate.addActionListener(e -> updateSatellite());
        btnDelete.addActionListener(e -> deleteSatellite());
        btnRefresh.addActionListener(e -> refreshTable());

        panel.add(btnAdd);
        panel.add(btnUpdate);
        panel.add(btnDelete);
        panel.add(btnRefresh);
        return panel;
    }

    // ===================== OPERATIONS =====================

    private void addSatellite() {
        if (currentPlanet == null) { showError("Chưa chọn hành tinh!"); return; }
        Satellite sat = buildFromForm();
        if (sat == null) return;

        OrbitalMechanics.initializeSatellite(sat, currentPlanet);
        dao.insertSatellite(sat);
        refreshTable();
        notifyChanged();
        taInfo.setText("Đã thêm vệ tinh: " + sat.getName() + "\n" +
                OrbitalMechanics.getSatelliteInfo(sat, currentPlanet));
    }

    private void updateSatellite() {
        int row = table.getSelectedRow();
        if (row < 0) { showError("Chọn vệ tinh cần sửa!"); return; }
        int id = (int) tableModel.getValueAt(row, 0);

        Satellite sat = buildFromForm();
        if (sat == null) return;
        sat.setId(id);
        sat.setPlanetId(currentPlanet.getId());

        OrbitalMechanics.initializeSatellite(sat, currentPlanet);
        dao.updateSatellite(sat);
        refreshTable();
        notifyChanged();
        taInfo.setText("Đã cập nhật: " + sat.getName());
    }

    private void deleteSatellite() {
        int row = table.getSelectedRow();
        if (row < 0) { showError("Chọn vệ tinh cần xóa!"); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa vệ tinh \"" + name + "\"?", "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        dao.hardDeleteSatellite(id);
        refreshTable();
        notifyChanged();
        taInfo.setText("Đã xóa vệ tinh: " + name);
    }

    private void calculateAndShowVelocity() {
        if (currentPlanet == null) { showError("Chưa chọn hành tinh!"); return; }
        try {
            double alt = Double.parseDouble(tfAlt.getText().trim()) * 1000;
            double v = OrbitalMechanics.calculateCircularVelocity(currentPlanet, alt);
            double T = OrbitalMechanics.calculateOrbitalPeriod(currentPlanet, alt);
            double g = OrbitalMechanics.calculateGravitationalAcceleration(currentPlanet, alt);

            taInfo.setText(String.format(
                "=== TÍNH TOÁN VẬN TỐC QUỸ ĐẠO ===\n" +
                "Hành tinh: %s\n" +
                "Độ cao: %.1f km\n" +
                "Bán kính quỹ đạo: %.1f km\n\n" +
                "Vận tốc quỹ đạo: %.2f m/s (%.2f km/h)\n" +
                "Chu kỳ quỹ đạo: %.2f phút\n" +
                "Gia tốc hấp dẫn: %.4f m/s²\n\n" +
                "Công thức: v = √(GM/r)\n" +
                "GM = %.4e m³/s²",
                currentPlanet.getName(),
                alt / 1000, (currentPlanet.getRadiusM() + alt) / 1000,
                v, v * 3.6,
                T / 60.0,
                g,
                currentPlanet.getGM()
            ));
        } catch (NumberFormatException e) {
            showError("Nhập đúng định dạng số!");
        }
    }

    private void onRowSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        String type = (String) tableModel.getValueAt(row, 2);
        String latStr = tableModel.getValueAt(row, 3).toString();
        String lonStr = tableModel.getValueAt(row, 4).toString();
        String altStr = tableModel.getValueAt(row, 5).toString();

        tfName.setText(name);
        tfLat.setText(latStr);
        tfLon.setText(lonStr);
        // altStr is in km
        tfAlt.setText(altStr);
        cbType.setSelectedItem(type);

        planetPanel.setSelectedSatellite(id);

        // Show info
        Satellite sat = dao.getSatelliteById(id);
        if (sat != null && currentPlanet != null) {
            OrbitalMechanics.initializeSatellite(sat, currentPlanet);
            taInfo.setText(OrbitalMechanics.getSatelliteInfo(sat, currentPlanet));
        }
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        if (currentPlanet == null) return;

        List<Satellite> sats = dao.getSatellitesByPlanet(currentPlanet.getId());
        for (Satellite s : sats) {
            OrbitalMechanics.initializeSatellite(s, currentPlanet);
            tableModel.addRow(new Object[]{
                s.getId(),
                s.getName(),
                s.getSatelliteType(),
                String.format("%.2f", s.getLatitude()),
                String.format("%.2f", s.getLongitude()),
                String.format("%.1f", s.getAltitudeM() / 1000.0),
                String.format("%.2f", s.getOrbitalVelocity())
            });
        }

        // Update 3D view
        planetPanel.setSatellites(sats);
    }

    public void setPlanet(Planet planet) {
        this.currentPlanet = planet;
        refreshTable();
    }

    private Satellite buildFromForm() {
        try {
            String name = tfName.getText().trim();
            if (name.isEmpty()) { showError("Tên vệ tinh không được rỗng!"); return null; }

            double lat = Double.parseDouble(tfLat.getText().trim());
            double lon = Double.parseDouble(tfLon.getText().trim());
            double alt = Double.parseDouble(tfAlt.getText().trim()) * 1000; // km → m

            if (lat < -90 || lat > 90) { showError("Vĩ độ phải trong khoảng -90 đến 90!"); return null; }
            if (lon < -180 || lon > 180) { showError("Kinh độ phải trong khoảng -180 đến 180!"); return null; }
            if (alt < 100000) { showError("Độ cao tối thiểu 100 km!"); return null; }

            String type = (String) cbType.getSelectedItem();
            String colorHex = String.format("#%02X%02X%02X",
                    selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());

            Satellite sat = new Satellite(name, currentPlanet.getId(), lat, lon, alt, type, colorHex);
            return sat;
        } catch (NumberFormatException e) {
            showError("Nhập đúng định dạng số cho các trường tọa độ!");
            return null;
        }
    }

    private void notifyChanged() {
        if (onSatelliteChanged != null) onSatelliteChanged.run();
    }

    public void setOnSatelliteChanged(Runnable callback) {
        this.onSatelliteChanged = callback;
    }

    // ===================== UI HELPERS =====================

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent comp) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        JLabel lbl = new JLabel(label);
        lbl.setForeground(new Color(160, 185, 230));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(comp, gbc);
        gbc.weightx = 0;
    }

    private JTextField darkTextField(String placeholder) {
        JTextField tf = new JTextField(placeholder, 10);
        tf.setBackground(new Color(30, 38, 65));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("Consolas", Font.PLAIN, 12));
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 80, 140)),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        return tf;
    }

    private JButton createIconButton(String text, Color bg) {
        JButton btn = new JButton(text);
        styleButton(btn, bg);
        return btn;
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btn.addMouseListener(new MouseAdapter() {
            Color orig = bg;
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.brighter());
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(orig);
            }
        });
    }

    private TitledBorder createDarkTitledBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 80, 140), 1),
            title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(130, 170, 255)
        );
        return border;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
