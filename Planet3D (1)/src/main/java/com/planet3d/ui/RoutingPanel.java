package com.planet3d.ui;

import com.planet3d.database.SatelliteDAO;
import com.planet3d.model.Planet;
import com.planet3d.model.RoutingResult;
import com.planet3d.model.Satellite;
import com.planet3d.physics.OrbitalMechanics;
import com.planet3d.routing.DijkstraRouter;
import com.planet3d.routing.SatelliteGraph;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel định tuyến thông tin từ điểm A đến điểm B qua mạng vệ tinh.
 */
public class RoutingPanel extends JPanel {

    private final SatelliteDAO dao = new SatelliteDAO();
    private Planet currentPlanet;
    private PlanetPanel planetPanel;

    // Input fields
    private JTextField tfALat, tfALon, tfAName;
    private JTextField tfBLat, tfBLon, tfBName;

    // Result display
    private JTextArea taResult;
    private DefaultTableModel historyModel;
    private JTable historyTable;

    // Status
    private JLabel lblStatus;
    
    // Preset panels
    private JPanel presetPanelA;
    private JPanel presetPanelB;

    public RoutingPanel(PlanetPanel planetPanel) {
        this.planetPanel = planetPanel;
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(18, 22, 36));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        buildUI();
    }

    private void buildUI() {
        // === INPUT PANEL ===
        JPanel inputPanel = buildInputPanel();

        // === RESULT PANEL ===
        taResult = new JTextArea(8, 0);
        taResult.setEditable(false);
        taResult.setFont(new Font("Consolas", Font.PLAIN, 12));
        taResult.setBackground(new Color(12, 18, 30));
        taResult.setForeground(new Color(80, 200, 120));
        taResult.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        taResult.setText("Nhập tọa độ điểm A và B, sau đó nhấn \"Tính Định Tuyến\".");
        JScrollPane resultScroll = new JScrollPane(taResult);
        resultScroll.setBorder(createDarkTitledBorder("Kết quả Định tuyến"));

        // === HISTORY TABLE ===
        String[] histCols = {"#", "Nguồn", "Đích", "Vệ tinh", "Hop", "Khoảng cách (km)", "Độ trễ (ms)", "Trạng thái"};
        historyModel = new DefaultTableModel(histCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setBackground(new Color(20, 26, 45));
        historyTable.setForeground(Color.WHITE);
        historyTable.setSelectionBackground(new Color(60, 100, 180));
        historyTable.setGridColor(new Color(40, 55, 90));
        historyTable.setFont(new Font("Consolas", Font.PLAIN, 11));
        historyTable.getTableHeader().setBackground(new Color(30, 40, 70));
        historyTable.getTableHeader().setForeground(new Color(150, 180, 255));
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        historyTable.setRowHeight(20);

        JScrollPane histScroll = new JScrollPane(historyTable);
        histScroll.setPreferredSize(new Dimension(0, 150));
        histScroll.setBorder(createDarkTitledBorder(" Lịch sử Định tuyến"));
        histScroll.getViewport().setBackground(new Color(20, 26, 45));

        // Button bar
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnRoute  = createStyledButton("Tính Định Tuyến", new Color(40, 130, 200));
        JButton btnClear  = createStyledButton("Xóa Route", new Color(130, 50, 50));
        JButton btnHistory= createStyledButton(" Tải Lịch sử", new Color(60, 70, 110));

        btnRoute.addActionListener(e -> runRouting());
        btnClear.addActionListener(e -> clearRoute());
        btnHistory.addActionListener(e -> loadHistory());

        lblStatus = new JLabel("Sẵn sàng");
        lblStatus.setForeground(new Color(100, 200, 130));
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));

        btnBar.add(btnRoute);
        btnBar.add(btnClear);
        btnBar.add(btnHistory);
        btnBar.add(lblStatus);

        // Layout
        JPanel topArea = new JPanel(new BorderLayout(5, 5));
        topArea.setOpaque(false);
        topArea.add(inputPanel, BorderLayout.NORTH);
        topArea.add(btnBar, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultScroll, histScroll);
        split.setDividerLocation(200);
        split.setOpaque(false);
        split.setBorder(null);

        add(topArea, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setOpaque(false);

        // Ground Station A
        JPanel panelA = new JPanel(new GridBagLayout());
        panelA.setBackground(new Color(20, 35, 55));
        panelA.setBorder(createDarkTitledBorder(" Điểm A (Nguồn)  [Màu xanh lá]"));
        GridBagConstraints gA = new GridBagConstraints();
        gA.insets = new Insets(3, 5, 3, 5);
        gA.anchor = GridBagConstraints.WEST;

        tfAName = darkTextField("Hà Nội");
        tfALat  = darkTextField("21.028");
        tfALon  = darkTextField("105.834");
        addFormRow(panelA, gA, 0, "Tên:", tfAName);
        addFormRow(panelA, gA, 1, "Vĩ độ (°):", tfALat);
        addFormRow(panelA, gA, 2, "Kinh độ (°):", tfALon);

        // Preset buttons for A
        presetPanelA = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        presetPanelA.setOpaque(false);
        addPresetButton(presetPanelA, "Hà Nội", 21.028, 105.834, tfAName, tfALat, tfALon);
        addPresetButton(presetPanelA, "HCM",    10.762, 106.660, tfAName, tfALat, tfALon);
        addPresetButton(presetPanelA, "London", 51.507,  -0.127, tfAName, tfALat, tfALon);
        addPresetButton(presetPanelA, "NY",     40.712, -74.006, tfAName, tfALat, tfALon);
        gA.gridx = 0; gA.gridy = 3; gA.gridwidth = 2; gA.fill = GridBagConstraints.HORIZONTAL;
        panelA.add(presetPanelA, gA);

        // Ground Station B
        JPanel panelB = new JPanel(new GridBagLayout());
        panelB.setBackground(new Color(35, 20, 40));
        panelB.setBorder(createDarkTitledBorder(" Điểm B (Đích)  [Màu đỏ]"));
        GridBagConstraints gB = new GridBagConstraints();
        gB.insets = new Insets(3, 5, 3, 5);
        gB.anchor = GridBagConstraints.WEST;

        tfBName = darkTextField("Tokyo");
        tfBLat  = darkTextField("35.689");
        tfBLon  = darkTextField("139.692");
        addFormRow(panelB, gB, 0, "Tên:", tfBName);
        addFormRow(panelB, gB, 1, "Vĩ độ (°):", tfBLat);
        addFormRow(panelB, gB, 2, "Kinh độ (°):", tfBLon);

        // Preset buttons for B
        presetPanelB = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        presetPanelB.setOpaque(false);
        addPresetButton(presetPanelB, "Tokyo",  35.689,  139.692, tfBName, tfBLat, tfBLon);
        addPresetButton(presetPanelB, "Sydney", -33.868, 151.209, tfBName, tfBLat, tfBLon);
        addPresetButton(presetPanelB, "LA",     34.052, -118.243, tfBName, tfBLat, tfBLon);
        addPresetButton(presetPanelB, "Dubai",  25.204,   55.270, tfBName, tfBLat, tfBLon);
        gB.gridx = 0; gB.gridy = 3; gB.gridwidth = 2; gB.fill = GridBagConstraints.HORIZONTAL;
        panelB.add(presetPanelB, gB);

        panel.add(panelA);
        panel.add(panelB);

        // --- Live preview: cập nhật marker trên 3D khi nhập tọa độ ---
        DocumentListener livePreview = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateGroundStationPreview(); }
            public void removeUpdate(DocumentEvent e)  { updateGroundStationPreview(); }
            public void changedUpdate(DocumentEvent e) { updateGroundStationPreview(); }
        };
        tfALat.getDocument().addDocumentListener(livePreview);
        tfALon.getDocument().addDocumentListener(livePreview);
        tfAName.getDocument().addDocumentListener(livePreview);
        tfBLat.getDocument().addDocumentListener(livePreview);
        tfBLon.getDocument().addDocumentListener(livePreview);
        tfBName.getDocument().addDocumentListener(livePreview);

        // Hiển thị ngay giá trị mặc định
        SwingUtilities.invokeLater(this::updateGroundStationPreview);

        return panel;
    }

    private void addPresetButton(JPanel container, String city, double lat, double lon,
                                  JTextField nameF, JTextField latF, JTextField lonF) {
        JButton btn = new JButton(city);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btn.setBackground(new Color(45, 60, 100));
        btn.setForeground(new Color(180, 200, 255));
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(2, 6, 2, 6));
        btn.addActionListener(e -> {
            nameF.setText(city);
            latF.setText(String.valueOf(lat));
            lonF.setText(String.valueOf(lon));
            // updateGroundStationPreview() được kích hoạt tự động qua DocumentListener
        });
        container.add(btn);
    }

    /**
     * Đọc tọa độ từ các ô nhập liệu và cập nhật marker trạm mặt đất
     * trên 3D ngay lập tức (live preview không cần bấm Tính Định Tuyến).
     */
    private void updateGroundStationPreview() {
        if (currentPlanet == null) return;
        double radius = currentPlanet.getRadiusM();

        double[] aPos = null;
        double[] bPos = null;
        String aLabel = tfAName.getText().trim();
        String bLabel = tfBName.getText().trim();

        try {
            double aLat = Double.parseDouble(tfALat.getText().trim());
            double aLon = Double.parseDouble(tfALon.getText().trim());
            aPos = OrbitalMechanics.latLonAltToCartesian(aLat, aLon, 0, radius);
        } catch (NumberFormatException ignored) {}

        try {
            double bLat = Double.parseDouble(tfBLat.getText().trim());
            double bLon = Double.parseDouble(tfBLon.getText().trim());
            bPos = OrbitalMechanics.latLonAltToCartesian(bLat, bLon, 0, radius);
        } catch (NumberFormatException ignored) {}

        planetPanel.setGroundStations(aPos, aLabel.isEmpty() ? "A" : aLabel,
                                      bPos, bLabel.isEmpty() ? "B" : bLabel);
    }

    // ===================== ROUTING LOGIC =====================

    private void runRouting() {
        if (currentPlanet == null) { showError("Chưa chọn hành tinh!"); return; }

        try {
            double aLat = Double.parseDouble(tfALat.getText().trim());
            double aLon = Double.parseDouble(tfALon.getText().trim());
            double bLat = Double.parseDouble(tfBLat.getText().trim());
            double bLon = Double.parseDouble(tfBLon.getText().trim());
            String aName = tfAName.getText().trim().isEmpty() ? "A" : tfAName.getText().trim();
            String bName = tfBName.getText().trim().isEmpty() ? "B" : tfBName.getText().trim();

            List<Satellite> satellites = dao.getSatellitesByPlanet(currentPlanet.getId());
            if (satellites.isEmpty()) {
                showError("Không có vệ tinh nào trên hành tinh này!\nHãy thêm vệ tinh trong tab 'Vệ tinh'.");
                return;
            }

            // Initialize satellite positions
            for (Satellite sat : satellites) {
                OrbitalMechanics.initializeSatellite(sat, currentPlanet);
            }

            lblStatus.setText(" Đang tính...");
            lblStatus.setForeground(new Color(255, 180, 60));

            // Run on background thread
            SwingWorker<RoutingResult, Void> worker = new SwingWorker<>() {
                @Override
                protected RoutingResult doInBackground() {
                    SatelliteGraph graph = new SatelliteGraph(satellites, currentPlanet);
                    graph.setGroundA(aLat, aLon, aName);
                    graph.setGroundB(bLat, bLon, bName);
                    graph.build();

                    DijkstraRouter router = new DijkstraRouter(graph, currentPlanet);
                    RoutingResult result = router.route(satellites);
                    result.setSourceLat(aLat); result.setSourceLon(aLon); result.setSourceLabel(aName);
                    result.setDestLat(bLat);   result.setDestLon(bLon);   result.setDestLabel(bName);
                    result.setSatelliteCount(satellites.size());

                    // Save to DB
                    dao.saveRoutingResult(result);
                    return result;
                }

                @Override
                protected void done() {
                    try {
                        RoutingResult result = get();
                        displayRoutingResult(result, satellites, aLat, aLon, bLat, bLon);
                        loadHistory();
                        lblStatus.setText("Hoàn thành");
                        lblStatus.setForeground(new Color(80, 220, 130));
                    } catch (Exception ex) {
                        lblStatus.setText("Lỗi");
                        lblStatus.setForeground(new Color(255, 80, 80));
                        ex.printStackTrace();
                    }
                }
            };
            worker.execute();

        } catch (NumberFormatException e) {
            showError("Nhập đúng tọa độ số thập phân!");
        }
    }

    private void displayRoutingResult(RoutingResult r, List<Satellite> satellites,
                                       double aLat, double aLon, double bLat, double bLon) {
        StringBuilder sb = new StringBuilder();
        sb.append("════════════════════════════════════════\n");
        sb.append("           KẾT QUẢ ĐỊNH TUYẾN\n");
        sb.append("════════════════════════════════════════\n");

        if (!r.isSuccess()) {
            sb.append("THẤT BẠI: ").append(r.getFailureReason()).append("\n");
            sb.append(" Gợi ý: Thêm nhiều vệ tinh hơn để cải thiện độ phủ sóng.\n");
            taResult.setText(sb.toString());
            taResult.setForeground(new Color(255, 100, 100));
            planetPanel.clearRoute();
            return;
        }

        taResult.setForeground(new Color(80, 220, 130));

        sb.append(String.format(" Nguồn:        %s (%.3f°, %.3f°)\n", r.getSourceLabel(), r.getSourceLat(), r.getSourceLon()));
        sb.append(String.format(" Đích:          %s (%.3f°, %.3f°)\n", r.getDestLabel(), r.getDestLat(), r.getDestLon()));
        sb.append("────────────────────────────────────────\n");
        sb.append(String.format(" Số vệ tinh mạng: %d\n", r.getSatelliteCount()));
        sb.append(String.format("Số hop (relay):  %d\n", r.getHopCount()));
        sb.append(String.format(" Tổng khoảng cách: %.2f km\n", r.getTotalDistanceKm()));
        sb.append(String.format("️  Độ trễ ước tính: %.2f ms\n", r.getEstimatedLatencyMs()));
        sb.append(String.format("Thời gian tính:   %d ms\n", r.getComputationTimeMs()));
        sb.append("────────────────────────────────────────\n");
        sb.append("️  Đường đi:\n  ");

        if (r.getSatelliteNames() != null) {
            sb.append(String.join("\n  → ", r.getSatelliteNames()));
        }
        sb.append("\n════════════════════════════════════════");
        taResult.setText(sb.toString());

        // Update 3D view
        double[] aPos = OrbitalMechanics.latLonAltToCartesian(aLat, aLon, 0, currentPlanet.getRadiusM());
        double[] bPos = OrbitalMechanics.latLonAltToCartesian(bLat, bLon, 0, currentPlanet.getRadiusM());
        planetPanel.setCurrentRoute(r, aPos, bPos);
        planetPanel.setSatellites(satellites);
    }

    private void clearRoute() {
        planetPanel.clearRoute();
        taResult.setText("Route đã xóa.");
        taResult.setForeground(new Color(150, 150, 180));
        lblStatus.setText("Sẵn sàng");
        lblStatus.setForeground(new Color(100, 200, 130));
        // Vẫn giữ markers trạm mặt đất (chỉ xóa route, không xóa preview)
        updateGroundStationPreview();
    }

    private void loadHistory() {
        if (currentPlanet == null) return;
        historyModel.setRowCount(0);
        List<RoutingResult> history = dao.getRoutingHistory(currentPlanet.getId(), 30);
        int n = 1;
        for (RoutingResult r : history) {
            historyModel.addRow(new Object[]{
                n++,
                r.getSourceLabel() != null ? r.getSourceLabel() : String.format("%.2f,%.2f", r.getSourceLat(), r.getSourceLon()),
                r.getDestLabel() != null ? r.getDestLabel() : String.format("%.2f,%.2f", r.getDestLat(), r.getDestLon()),
                r.getSatelliteCount(),
                r.isSuccess() ? r.getHopCount() : "-",
                r.isSuccess() ? String.format("%.1f", r.getTotalDistanceKm()) : "-",
                r.isSuccess() ? String.format("%.1f", r.getEstimatedLatencyMs()) : "-",
                r.isSuccess() ? "" : ""
            });
        }
    }

    public void setPlanet(Planet planet) {
        // Chỉ xóa text field nếu là hành tinh khác
        if (this.currentPlanet != null && this.currentPlanet.getId() != planet.getId()) {
            SwingUtilities.invokeLater(() -> {
                tfAName.setText(""); tfALat.setText(""); tfALon.setText("");
                tfBName.setText(""); tfBLat.setText(""); tfBLon.setText("");
                taResult.setText("Đã chuyển hành tinh. Nhập tọa độ điểm A và B để định tuyến.");
            });
        }
        
        this.currentPlanet = planet;
        
        // Ẩn/hiện các nút preset tùy theo hành tinh
        boolean isEarth = planet.getName().equalsIgnoreCase("Earth");
        if (presetPanelA != null) presetPanelA.setVisible(isEarth);
        if (presetPanelB != null) presetPanelB.setVisible(isEarth);
        
        historyModel.setRowCount(0);
        loadHistory();
        // Cập nhật marker trạm ngay khi chuyển hành tinh
        SwingUtilities.invokeLater(this::updateGroundStationPreview);
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

    private JTextField darkTextField(String text) {
        JTextField tf = new JTextField(text, 10);
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

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(bg); }
        });
        return btn;
    }

    private TitledBorder createDarkTitledBorder(String title) {
        return BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 80, 140), 1),
            title,
            TitledBorder.LEFT, TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(130, 170, 255)
        );
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
