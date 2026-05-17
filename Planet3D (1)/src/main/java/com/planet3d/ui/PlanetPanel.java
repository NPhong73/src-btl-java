package com.planet3d.ui;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.planet3d.model.Planet;
import com.planet3d.model.RoutingResult;
import com.planet3d.model.Satellite;
import com.planet3d.physics.OrbitalMechanics;
import com.planet3d.physics.SatellitePosition;
import com.planet3d.renderer.PlanetRenderer;
import com.planet3d.renderer.RouteRenderer;
import com.planet3d.renderer.SatelliteRenderer;

import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.glu.GLUquadric;

/**
 * Panel OpenGL chính - Hiển thị hành tinh 3D, vệ tinh và định tuyến.
 */
public class PlanetPanel extends GLJPanel implements GLEventListener {

    // OpenGL objects
    private GLU glu;
    private FPSAnimator animator;

    // Renderers
    private final PlanetRenderer planetRenderer = new PlanetRenderer();
    private final SatelliteRenderer satelliteRenderer = new SatelliteRenderer();
    private final RouteRenderer routeRenderer = new RouteRenderer();

    // Simulation state
    private final SatellitePosition positionUpdater = new SatellitePosition(300.0);
    private Planet currentPlanet;
    private List<Satellite> satellites = new ArrayList<>();
    private RoutingResult currentRoute = null;
    private int selectedSatelliteId = -1;

    // Ground station positions and labels
    private double[] groundAPos = null;
    private double[] groundBPos = null;
    private String   groundALabel = null;
    private String   groundBLabel = null;

    // Camera state
    private float rotX = 20.0f, rotY = 30.0f;
    private float zoom = -4.0f;
    private int lastMouseX, lastMouseY;

    // Options
    private boolean showOrbits = true;
    private boolean showLinks = false;
    private boolean animating = true;

    // Planet rotation
    private float planetRotAngle = 0;
    private long lastFrameTime = System.currentTimeMillis();

    // Background stars
    private float[][] stars;
    private Texture spaceTexture;

    public PlanetPanel(GLCapabilities caps) {
        super(caps);
        setOpaque(true);
        setBackground(new Color(5, 10, 25));
        addGLEventListener(this);
        setupMouseControls();
        generateStars(300);
    }

    private void generateStars(int count) {
        stars = new float[count][3];
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < count; i++) {
            // Random points on a large sphere
            double theta = rng.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * rng.nextDouble() - 1);
            float r = 80.0f;
            stars[i][0] = (float)(r * Math.sin(phi) * Math.cos(theta));
            stars[i][1] = (float)(r * Math.sin(phi) * Math.sin(theta));
            stars[i][2] = (float)(r * Math.cos(phi));
        }
    }

    // ===================== GL EVENT LISTENER =====================

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        glu = new GLU();

        gl.glClearColor(0.02f, 0.02f, 0.08f, 1.0f); // Deep space blue-black
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        // Smooth rendering
        gl.glEnable(GL2.GL_LINE_SMOOTH);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glHint(GL2.GL_POINT_SMOOTH_HINT, GL2.GL_NICEST);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

        // Lighting setup
        setupLighting(gl);

        // Load texture
        if (currentPlanet != null) {
            planetRenderer.loadTexture(gl, currentPlanet.getTextureFile());
        }

        // Load space texture
        try {
            spaceTexture = TextureIO.newTexture(new File("docs/space.jpg"), true);
            spaceTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
            spaceTexture.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
            spaceTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
            spaceTexture.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        } catch (Exception e) {
            System.err.println("[GL] Lỗi tải space texture: " + e.getMessage());
        }

        // Start animator
        animator = new FPSAnimator(this, 60, true);
        animator.start();

        System.out.println("[GL] OpenGL khởi tạo: " + gl.glGetString(GL2.GL_RENDERER));
    }

    private void setupLighting(GL2 gl) {
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);

        // Ánh sáng mặt trời từ phía phải-trên
        float[] lightPos  = {5.0f, 3.0f, 5.0f, 0.0f}; // Directional
        float[] ambient   = {0.1f, 0.1f, 0.15f, 1.0f};
        float[] diffuse   = {1.0f, 0.98f, 0.95f, 1.0f};
        float[] specular  = {0.5f, 0.5f, 0.5f, 1.0f};

        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT,  ambient,  0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE,  diffuse,  0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, specular, 0);

        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE);

        float[] matSpecular = {0.3f, 0.3f, 0.3f, 1.0f};
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, matSpecular, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 32.0f);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        // Update timing
        long now = System.currentTimeMillis();
        double dt = (now - lastFrameTime) / 1000.0;
        lastFrameTime = now;

        // Update satellite positions
        if (animating && currentPlanet != null && !satellites.isEmpty()) {
            positionUpdater.update(satellites, currentPlanet, dt);
        }

        // Camera setup
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(0, 0, -zoom, 0, 0, 0, 0, 1, 0);

        // Draw stars (background, no depth write)
        gl.glDepthMask(false);
        drawStars(gl);
        gl.glDepthMask(true);

        // Apply camera rotation
        gl.glRotatef(rotX, 1, 0, 0);
        gl.glRotatef(rotY, 0, 1, 0);

        // Planet rotation
        if (animating && currentPlanet != null) {
            double rotSpeed = 360.0 / currentPlanet.getRotationPeriodS();
            planetRotAngle += (float)(rotSpeed * dt * positionUpdater.getTimeMultiplier());
            planetRotAngle %= 360.0f;
        }

        // Render planet and ground stations (integrated for perfect rotation sync)
        if (currentPlanet != null) {
            planetRenderer.render(gl, glu, currentPlanet, planetRotAngle, 
                                 routeRenderer, groundAPos, groundBPos, 
                                 groundALabel, groundBLabel);
        }

        // Render satellite links
        satelliteRenderer.renderLinks(gl, satellites, showLinks);

        // Render satellites
        satelliteRenderer.render(gl, glu, satellites, selectedSatelliteId, showOrbits);

        // Calculate rotated (current world-space) positions for routing LINES only
        double[] rotatedAPosForRoute = getRotatedPosition(groundAPos, planetRotAngle);
        double[] rotatedBPosForRoute = getRotatedPosition(groundBPos, planetRotAngle);

        // Render route (đường định tuyến - world space lines)
        if (currentRoute != null && currentRoute.isSuccess()) {
            routeRenderer.render(gl, currentRoute, satellites, rotatedAPosForRoute, rotatedBPosForRoute);
        }

        // Overlay HUD
        drawHUD(gl, drawable);
    }

    /** Tính tọa độ đã được quay theo trục của hành tinh (World-space) */
    private double[] getRotatedPosition(double[] pos, float rotAngle) {
        if (pos == null) return null;
        
        // 1. Tự quay quanh trục Y (Spin) - Xoay trước
        // Hệ tọa độ Lon0=+X, Lon90=+Z: Xoay CCW quanh Y:
        // x' = x*cos - z*sin
        // z' = x*sin + z*cos
        double spinRad = Math.toRadians(rotAngle);
        double sc = Math.cos(spinRad);
        double ss = Math.sin(spinRad);
        double x1 = pos[0] * sc - pos[2] * ss;
        double y1 = pos[1];
        double z1 = pos[0] * ss + pos[2] * sc;
        
        // 2. Nghiêng trục quanh Z (Axial Tilt) - Nghiêng sau
        double tiltRad = Math.toRadians(-23.5);
        double tc = Math.cos(tiltRad);
        double ts = Math.sin(tiltRad);
        double x2 = x1 * tc - y1 * ts;
        double y2 = x1 * ts + y1 * tc;
        double z2 = z1;
        
        return new double[]{x2, y2, z2};
    }

    private void drawStars(GL2 gl) {
        if (spaceTexture != null) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_TEXTURE_2D);
            spaceTexture.enable(gl);
            spaceTexture.bind(gl);
            gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            
            gl.glMatrixMode(GL2.GL_TEXTURE);
            gl.glPushMatrix();
            gl.glScalef(6.0f, 6.0f, 1.0f); // Thu nhỏ ảnh (lặp lại 6 lần)
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            
            GLUquadric quad = glu.gluNewQuadric();
            glu.gluQuadricTexture(quad, true);
            glu.gluQuadricNormals(quad, GLU.GLU_SMOOTH);
            glu.gluQuadricOrientation(quad, GLU.GLU_INSIDE);
            gl.glPushMatrix();
            gl.glRotatef(90.0f, 1, 0, 0);
            glu.gluSphere(quad, 100.0, 32, 32);
            gl.glPopMatrix();
            glu.gluDeleteQuadric(quad);
            
            gl.glMatrixMode(GL2.GL_TEXTURE);
            gl.glPopMatrix();
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            
            spaceTexture.disable(gl);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glEnable(GL2.GL_LIGHTING);
        } else {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glColor4f(1.0f, 1.0f, 1.0f, 0.8f);
            gl.glPointSize(1.5f);
            gl.glBegin(GL2.GL_POINTS);
            for (float[] star : stars) {
                gl.glVertex3f(star[0], star[1], star[2]);
            }
            gl.glEnd();
            gl.glPointSize(1.0f);
            gl.glEnable(GL2.GL_LIGHTING);
        }
    }

    private void drawHUD(GL2 gl, GLAutoDrawable drawable) {
        // Simple text overlay using immediate mode (simplified)
        // In production would use JOGL TextRenderer
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (height == 0) height = 1;
        float aspect = (float) width / height;

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0, aspect, 0.1, 200.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        if (animator != null) animator.stop();
        GL2 gl = drawable.getGL().getGL2();
        planetRenderer.destroy(gl);
        routeRenderer.dispose();
    }

    // ===================== MOUSE CONTROLS =====================

    private void setupMouseControls() {
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;
                if (SwingUtilities.isLeftMouseButton(e)) {
                    rotY += dx * 0.5f;
                    rotX += dy * 0.5f;
                    rotX = Math.max(-90, Math.min(90, rotX));
                }
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addMouseWheelListener(e -> {
            zoom += (float)(e.getWheelRotation() * 0.3);
            zoom = Math.max(-10.0f, Math.min(-2.0f, zoom));
        });
    }

    // ===================== PUBLIC API =====================

    public void setPlanet(Planet planet) {
        this.currentPlanet = planet;
        this.satellites.clear();
        this.currentRoute = null;
        this.groundAPos = null;
        this.groundBPos = null;
        // Reload texture in next frame
        if (planetRenderer != null) {
            planetRotAngle = 0;
        }
    }

    public void setSatellites(List<Satellite> sats) {
        this.satellites = new ArrayList<>(sats);
        if (currentPlanet != null) {
            for (Satellite sat : this.satellites) {
                OrbitalMechanics.initializeSatellite(sat, currentPlanet);
            }
        }
    }

    public void setCurrentRoute(RoutingResult route, double[] aPos, double[] bPos) {
        this.currentRoute = route;
        this.groundAPos   = aPos;
        this.groundBPos   = bPos;
    }

    /** Đặt vị trí trạm mặt đất (hiển thị ngay, trước khi tính định tuyến). */
    public void setGroundStations(double[] aPos, String aLabel, double[] bPos, String bLabel) {
        this.groundAPos   = aPos;
        this.groundALabel = aLabel;
        this.groundBPos   = bPos;
        this.groundBLabel = bLabel;
    }

    public void clearRoute() {
        this.currentRoute  = null;
        this.groundAPos    = null;
        this.groundBPos    = null;
        this.groundALabel  = null;
        this.groundBLabel  = null;
    }

    public void setSelectedSatellite(int id) { this.selectedSatelliteId = id; }
    public void setShowOrbits(boolean show) { this.showOrbits = show; }
    public void setShowLinks(boolean show) { this.showLinks = show; }
    public void setAnimating(boolean anim) { this.animating = anim; }
    public void setTimeMultiplier(double mult) { positionUpdater.setTimeMultiplier(mult); }
    public boolean isAnimating() { return animating; }
    public Planet getCurrentPlanet() { return currentPlanet; }

    public void reloadTexture(String texturePath) {
        // Will reload on next display() frame
        if (currentPlanet != null) {
            currentPlanet.setTextureFile(texturePath);
        }
        // Force reload by queuing on GL thread
        this.invoke(false, drawable -> {
            GL2 gl = drawable.getGL().getGL2();
            planetRenderer.loadTexture(gl, texturePath);
            return true;
        });
    }

    public void resetView() {
        rotX = 20.0f;
        rotY = 30.0f;
        zoom = -4.0f;
    }
}
