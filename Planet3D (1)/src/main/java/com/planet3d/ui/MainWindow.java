package com.planet3d.ui;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.planet3d.database.DatabaseManager;
import com.planet3d.database.SatelliteDAO;
import com.planet3d.model.Planet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

/**
 * Cửa sổ chính của ứng dụng Planet3D.
 * Bố cục: 3D Canvas bên trái, Tab panel bên phải.
 */
public class MainWindow extends JFrame {

    private PlanetPanel planetPanel;
    private SatellitePanel satellitePanel;
    private RoutingPanel routingPanel;
    private ReportPanel reportPanel;

    private SatelliteDAO dao = new SatelliteDAO();
    private List<Planet> planets;
    private Planet currentPlanet;

    // Toolbar controls
    private JComboBox<Planet> cbPlanets;
    private JLabel lblPlanetInfo;
    private JToggleButton btnAnimate;
    private JSlider sliderSpeed;

    public MainWindow() {
        super("Planet3D - Mô phỏng Hành tinh & Định tuyến Vệ tinh");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 850);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        // Set app icon (guard against missing resource)
        try {
            java.net.URL iconUrl = getClass().getResource("/icons/planet.png");
            if (iconUrl != null) {
                Image icon = Toolkit.getDefaultToolkit().createImage(iconUrl);
                setIconImage(icon);
            }
        } catch (Exception ignored) {}

        initGL();
        buildUI();
        loadPlanets();

        // Shutdown hook
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                DatabaseManager.getInstance().close();
            }
        });
    }

    private void initGL() {
        GLProfile.initSingleton();
        GLProfile profile;
        try {
            profile = GLProfile.get(GLProfile.GL2);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Không thể khởi tạo OpenGL!\nHệ thống của bạn cần hỗ trợ OpenGL 2.0+\n\nLỗi: " + e.getMessage(),
                "Lỗi OpenGL", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        // Build GL capabilities
        // NOTE: Windows WGL driver always allocates alpha=8; we must request alpha=8
        // to avoid the JOGL "Unable to determine GraphicsConfiguration" mismatch bug.
        GLCapabilities caps = new GLCapabilities(profile);
        caps.setAlphaBits(8);          // Request alpha for Windows compatibility
        caps.setDepthBits(24);         // 24-bit depth is standard
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);

        planetPanel = new PlanetPanel(caps);
        planetPanel.setPreferredSize(new Dimension(900, 700));
    }

    private void buildUI() {
        // ========== MENU BAR ==========
        setJMenuBar(buildMenuBar());

        // ========== TOOLBAR ==========
        JToolBar toolbar = buildToolbar();

        // ========== RIGHT PANEL (Tabs) ==========
        satellitePanel = new SatellitePanel(planetPanel);
        routingPanel   = new RoutingPanel(planetPanel);
        reportPanel    = new ReportPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(new Color(22, 28, 48));
        tabs.setForeground(new Color(170, 200, 255));
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabs.addTab("Vệ tinh", satellitePanel);
        tabs.addTab("Định tuyến", routingPanel);
        tabs.addTab("Báo cáo", reportPanel);

        tabs.setPreferredSize(new Dimension(480, 700));

        // ========== STATUS BAR ==========
        JPanel statusBar = buildStatusBar();

        // ========== MAIN LAYOUT ==========
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, planetPanel, tabs);
        mainSplit.setDividerLocation(870);
        mainSplit.setDividerSize(5);
        mainSplit.setContinuousLayout(true);

        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setOpaque(true);
        content.setBackground(new Color(12, 16, 28));
        content.add(toolbar, BorderLayout.NORTH);
        content.add(mainSplit, BorderLayout.CENTER);
        content.add(statusBar, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(new Color(20, 26, 45));
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 65, 110)));

        // File menu
        JMenu menuFile = createMenu("File");
        JMenuItem miLoadTexture = createMenuItem("Tải Texture Hành tinh...");
        JMenuItem miExportDB    = createMenuItem("Xuất CSDL");
        JMenuItem miExit        = createMenuItem("Thoát");

        miLoadTexture.addActionListener(e -> loadTextureDialog());
        miExit.addActionListener(e -> { DatabaseManager.getInstance().close(); System.exit(0); });

        menuFile.add(miLoadTexture);
        menuFile.add(miExportDB);
        menuFile.addSeparator();
        menuFile.add(miExit);

        // View menu
        JMenu menuView = createMenu("Hiển thị");
        JCheckBoxMenuItem miOrbits = new JCheckBoxMenuItem("Hiện quỹ đạo", true);
        JCheckBoxMenuItem miLinks  = new JCheckBoxMenuItem("Hiện liên kết vệ tinh", false);
        miOrbits.setForeground(new Color(200, 215, 255));
        miLinks.setForeground(new Color(200, 215, 255));
        miOrbits.addActionListener(e -> planetPanel.setShowOrbits(miOrbits.isSelected()));
        miLinks.addActionListener(e -> planetPanel.setShowLinks(miLinks.isSelected()));
        menuView.add(miOrbits);
        menuView.add(miLinks);
        menuView.addSeparator();
        JMenuItem miReset = createMenuItem("Reset Camera");
        miReset.addActionListener(e -> planetPanel.resetView());
        menuView.add(miReset);

        // Help menu
        JMenu menuHelp = createMenu("Trợ giúp");
        JMenuItem miGuide  = createMenuItem("Hướng dẫn Sử dụng");
        JMenuItem miAbout  = createMenuItem("Giới thiệu");
        miGuide.addActionListener(e -> showUserGuide());
        miAbout.addActionListener(e -> showAbout());
        menuHelp.add(miGuide);
        menuHelp.add(miAbout);

        menuBar.add(menuFile);
        menuBar.add(menuView);
        menuBar.add(menuHelp);
        return menuBar;
    }

    private JToolBar buildToolbar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.setBackground(new Color(22, 28, 50));
        tb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(45, 60, 100)));
        tb.setMargin(new Insets(4, 8, 4, 8));

        // Planet selector
        tb.add(Box.createRigidArea(new Dimension(15, 0))); // Thêm khoảng trống bên trái
        JLabel lblSelect = new JLabel("Hành tinh: ");
        lblSelect.setForeground(new Color(160, 185, 230));
        lblSelect.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tb.add(lblSelect);

        cbPlanets = new JComboBox<>();
        cbPlanets.setBackground(new Color(30, 40, 70));
        cbPlanets.setForeground(Color.WHITE);
        cbPlanets.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbPlanets.setMaximumSize(new Dimension(180, 32));
        cbPlanets.setPreferredSize(new Dimension(150, 32));
        cbPlanets.addActionListener(e -> {
            Planet sel = (Planet) cbPlanets.getSelectedItem();
            if (sel != null) switchPlanet(sel);
        });
        tb.add(cbPlanets);

        tb.addSeparator(new Dimension(20, 0));

        // Animate toggle
        btnAnimate = new JToggleButton("|| Đang chạy", true);
        btnAnimate.setBackground(new Color(40, 120, 60));
        btnAnimate.setForeground(Color.WHITE);
        btnAnimate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAnimate.setBorderPainted(false);
        btnAnimate.setFocusPainted(false);
        btnAnimate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAnimate.setMargin(new Insets(4, 12, 4, 12));
        btnAnimate.addActionListener(e -> {
            boolean anim = btnAnimate.isSelected();
            planetPanel.setAnimating(anim);
            btnAnimate.setText(anim ? "|| Đang chạy" : "X Dừng");
            btnAnimate.setBackground(anim ? new Color(40, 120, 60) : new Color(100, 60, 30));
        });
        tb.add(btnAnimate);

        tb.addSeparator(new Dimension(15, 0));

        // Speed slider
        JLabel lblSpeed = new JLabel("Tốc độ: ");
        lblSpeed.setForeground(new Color(160, 185, 230));
        lblSpeed.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tb.add(lblSpeed);

        sliderSpeed = new JSlider(1, 2000, 300);
        sliderSpeed.setBackground(new Color(22, 28, 50));
        sliderSpeed.setForeground(new Color(100, 150, 255));
        sliderSpeed.setPreferredSize(new Dimension(160, 28));
        sliderSpeed.setMaximumSize(new Dimension(200, 30));
        sliderSpeed.setToolTipText("Tốc độ mô phỏng (x thực tế)");
        sliderSpeed.addChangeListener(e -> {
            planetPanel.setTimeMultiplier(sliderSpeed.getValue());
        });
        tb.add(sliderSpeed);

        tb.add(Box.createRigidArea(new Dimension(30, 0))); // Khoảng cách cố định giữa slider và nút chức năng

        // Texture button
        JButton btnTexture = new JButton("Texture");
        styleToolbarButton(btnTexture, new Color(60, 80, 140));
        btnTexture.addActionListener(e -> loadTextureDialog());
        tb.add(btnTexture);
        tb.add(Box.createRigidArea(new Dimension(5, 0)));

        // Orbits toggle
        JToggleButton btnOrbits = new JToggleButton("Quỹ đạo", true);
        styleToolbarButton(btnOrbits, new Color(40, 70, 140));
        btnOrbits.addActionListener(e -> planetPanel.setShowOrbits(btnOrbits.isSelected()));
        tb.add(btnOrbits);
        tb.add(Box.createRigidArea(new Dimension(5, 0)));

        // Links toggle
        JToggleButton btnLinks = new JToggleButton("Links", false);
        styleToolbarButton(btnLinks, new Color(50, 80, 100));
        btnLinks.addActionListener(e -> planetPanel.setShowLinks(btnLinks.isSelected()));
        tb.add(btnLinks);
        
        // Đẩy phần thông tin hành tinh (R M GM) sát sang góc phải
        tb.add(Box.createHorizontalGlue());

        // Planet info label
        lblPlanetInfo = new JLabel("Chọn hành tinh để bắt đầu");
        lblPlanetInfo.setForeground(new Color(200, 220, 255));
        lblPlanetInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPlanetInfo.setHorizontalAlignment(SwingConstants.RIGHT);
        lblPlanetInfo.setBorder(new EmptyBorder(0, 0, 0, 15));
        
        // Cần đảm bảo JLabel không bị giãn vô hạn về bên trái, 
        // hoặc nếu có giãn thì text vẫn nằm bên phải.
        // Box.createHorizontalGlue() phía trước sẽ đẩy nó sát lề phải.
        tb.add(lblPlanetInfo);

        return tb;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        bar.setBackground(new Color(15, 20, 35));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(40, 55, 90)));

        JLabel lbl = new JLabel("Planet3D v1.0  |  Chuột trái: Xoay  |  Cuộn: Zoom  |  © 2025");
        lbl.setForeground(new Color(80, 100, 150));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        bar.add(lbl);
        return bar;
    }

    private void loadPlanets() {
        planets = dao.getAllPlanets();
        cbPlanets.removeAllItems();
        for (Planet p : planets) cbPlanets.addItem(p);

        if (!planets.isEmpty()) {
            switchPlanet(planets.get(0));
        }
    }

    private void switchPlanet(Planet planet) {
        this.currentPlanet = planet;
        planetPanel.setPlanet(planet);
        satellitePanel.setPlanet(planet);
        routingPanel.setPlanet(planet);
        reportPanel.setPlanet(planet);

        // Load texture
        if (planet.getTextureFile() != null) {
            planetPanel.reloadTexture(planet.getTextureFile());
        }

        // Update info label
        lblPlanetInfo.setText(String.format(
            "<html><b style='color:#ffffff; font-size:13px'>%s</b> &nbsp;|&nbsp; " +
            "<span style='color:#99c2ff'>R:</span> <span style='color:#e6f0ff'>%,.0f km</span> &nbsp;|&nbsp; " +
            "<span style='color:#99c2ff'>M:</span> <span style='color:#e6f0ff'>%.2e kg</span> &nbsp;|&nbsp; " +
            "<span style='color:#99c2ff'>GM:</span> <span style='color:#e6f0ff'>%.3e m³/s²</span></html>",
            planet.getName(), planet.getRadiusM()/1000, planet.getMassKg(), planet.getGM()
        ));

        // Update satellite list in routing panel
        satellitePanel.refreshTable();
    }

    private void loadTextureDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn file texture hành tinh (JPG/PNG)");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Ảnh (*.jpg, *.jpeg, *.png)", "jpg", "jpeg", "png"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            planetPanel.reloadTexture(selected.getAbsolutePath());
            if (currentPlanet != null) {
                currentPlanet.setTextureFile(selected.getAbsolutePath());
                lblPlanetInfo.setText("Texture: " + selected.getName());
            }
        }
    }

    private void showUserGuide() {
        String guide =
            "╔══════════════════════════════════════════════════╗\n" +
            "║         HƯỚNG DẪN SỬ DỤNG PLANET3D              ║\n" +
            "╚══════════════════════════════════════════════════╝\n\n" +
            "1. ĐIỀU HƯỚNG 3D:\n" +
            "   • Chuột trái + kéo:  Xoay hành tinh\n" +
            "   • Cuộn chuột:        Phóng to/thu nhỏ\n" +
            "   • Menu View → Reset Camera: Đặt lại góc nhìn\n\n" +
            "2. QUẢN LÝ VỆ TINH (Tab 'Vệ tinh'):\n" +
            "   • Nhập: Tên, Vĩ độ (-90→90), Kinh độ (-180→180), Độ cao (km)\n" +
            "   • Nhấn 'Tính vận tốc' để tính vận tốc quỹ đạo tự động\n" +
            "   • Nhấn 'Thêm' để thêm vệ tinh vào CSDL và hiển thị 3D\n" +
            "   • Click vào dòng trong bảng để chọn vệ tinh\n\n" +
            "3. ĐỊNH TUYẾN VỆ TINH (Tab 'Định tuyến'):\n" +
            "   • Nhập tọa độ Điểm A (nguồn) và Điểm B (đích)\n" +
            "   • Dùng nút preset thành phố để điền tự động\n" +
            "   • Nhấn 'Tính Định Tuyến' để chạy thuật toán Dijkstra\n" +
            "   • Đường định tuyến sẽ hiển thị màu vàng trên 3D\n\n" +
            "4. BÁO CÁO (Tab 'Báo cáo'):\n" +
            "   • Nhập danh sách số vệ tinh cách nhau bằng dấu phẩy\n" +
            "   • Nhấn 'Chạy Benchmark' để đánh giá với từng kịch bản\n" +
            "   • Xem biểu đồ và nhận xét tự động\n" +
            "   • Nhấn 'Xuất Báo cáo' để lưu file txt\n\n" +
            "5. TEXTURE:\n" +
            "   • Tải texture từ: https://www.solarsystemscope.com/textures/\n" +
            "   • File → Tải Texture Hành tinh → chọn file JPG/PNG\n" +
            "   • Không cần cài đặt thêm, texture tự động áp dụng\n";

        JTextArea ta = new JTextArea(guide);
        ta.setEditable(false);
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        ta.setBackground(new Color(18, 22, 36));
        ta.setForeground(new Color(200, 215, 255));
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(600, 500));
        JOptionPane.showMessageDialog(this, sp, "Hướng dẫn Sử dụng", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "<html><div style='text-align:center;padding:20px'>" +
            "<h2>Planet3D v1.0</h2>" +
            "<p>Ứng dụng mô phỏng hành tinh 3D và định tuyến vệ tinh</p>" +
            "<hr>" +
            "<p><b>Công nghệ:</b> Java 11 + JOGL 2.4 (OpenGL) + SQLite</p>" +
            "<p><b>Thuật toán:</b> Dijkstra (định tuyến) + Rodrigues (quỹ đạo)</p>" +
            "<p><b>CSDL:</b> SQLite (planet3d.db)</p>" +
            "</div></html>",
            "Giới thiệu", JOptionPane.INFORMATION_MESSAGE);
    }

    // ===================== UI HELPERS =====================

    private JMenu createMenu(String text) {
        JMenu menu = new JMenu(text);
        menu.setForeground(new Color(200, 215, 255));
        menu.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return menu;
    }

    private JMenuItem createMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(new Color(22, 28, 50));
        item.setForeground(new Color(200, 215, 255));
        item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return item;
    }

    private void styleToolbarButton(AbstractButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(4, 10, 4, 10));
    }
}
