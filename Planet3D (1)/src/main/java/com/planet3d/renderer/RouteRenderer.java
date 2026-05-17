package com.planet3d.renderer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.planet3d.model.RoutingResult;
import com.planet3d.model.Satellite;

import java.awt.Font;
import java.util.List;

/**
 * Render đường định tuyến giữa các vệ tinh và trạm mặt đất.
 */
public class RouteRenderer {

    private TextRenderer textRenderer;
    private final Font labelFont = new Font("SansSerif", Font.BOLD, 13);

    // Màu trạm A (xanh lá) và B (đỏ cam)
    private static final float[] COLOR_A = {0.0f, 1.0f, 0.3f};
    private static final float[] COLOR_B = {1.0f, 0.25f, 0.0f};

    public RouteRenderer() {
        textRenderer = new TextRenderer(labelFont, true, true);
    }

    /**
     * Vẽ đường định tuyến từ kết quả Dijkstra.
     */
    public void render(GL2 gl, RoutingResult result, List<Satellite> satellites,
                       double[] aPos, double[] bPos) {
        if (result == null || !result.isSuccess()) return;
        if (result.getSatellitePath() == null || result.getSatellitePath().isEmpty()) return;

        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        List<Integer> path = result.getSatellitePath();
        double[][] positions = new double[path.size()][];

        for (int i = 0; i < path.size(); i++) {
            int nodeId = path.get(i);
            if (nodeId == com.planet3d.routing.SatelliteGraph.GROUND_A_ID) {
                positions[i] = aPos;
            } else if (nodeId == com.planet3d.routing.SatelliteGraph.GROUND_B_ID) {
                positions[i] = bPos;
            } else {
                Satellite sat = satellites.stream()
                        .filter(s -> s.getId() == nodeId).findFirst().orElse(null);
                positions[i] = sat != null ? sat.getPosition3D() : null;
            }
        }

        // --- Đường route: 2 lớp (glow ngoài + lõi sáng) ---
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0) {
                // Lớp glow mờ bên ngoài
                gl.glLineWidth(5.0f);
                gl.glColor4f(1.0f, 0.7f, 0.0f, 0.25f);
            } else {
                // Lõi sáng
                gl.glLineWidth(2.0f);
            }
            for (int i = 0; i < positions.length - 1; i++) {
                if (positions[i] == null || positions[i+1] == null) continue;
                if (pass == 1) {
                    float alpha = 0.7f + 0.3f * i / Math.max(1, positions.length - 1);
                    gl.glColor4f(1.0f, 0.85f, 0.1f, alpha);
                }
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex3d(positions[i][0],   positions[i][1],   positions[i][2]);
                gl.glVertex3d(positions[i+1][0], positions[i+1][1], positions[i+1][2]);
                gl.glEnd();
            }
        }

        // --- Relay nodes (vệ tinh trung gian trên đường đi) ---
        gl.glColor4f(1.0f, 0.9f, 0.1f, 1.0f);
        gl.glPointSize(9.0f);
        gl.glBegin(GL2.GL_POINTS);
        for (int i = 1; i < positions.length - 1; i++) {
            if (positions[i] != null)
                gl.glVertex3d(positions[i][0], positions[i][1], positions[i][2]);
        }
        gl.glEnd();

        gl.glPointSize(1.0f);
        gl.glLineWidth(1.0f);
        gl.glEnable(GL2.GL_LIGHTING);
    }

    /**
     * Vẽ cả hai trạm mặt đất (A và B) — gọi luôn luôn trong display loop
     * để các trạm hiển thị ngay cả khi chưa có route.
     *
     * @param gl   OpenGL context
     * @param aPos Vị trí 3D trạm A (normalized), null nếu chưa đặt
     * @param bPos Vị trí 3D trạm B (normalized), null nếu chưa đặt
     * @param aLabel Nhãn trạm A (có thể null)
     * @param bLabel Nhãn trạm B (có thể null)
     */
    public void renderGroundStations(GL2 gl, double[] aPos, double[] bPos,
                                     String aLabel, String bLabel) {
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        if (aPos != null) {
            drawStationIcon(gl, aPos, COLOR_A);
            drawLabel(gl, aPos, aLabel, COLOR_A);
        }
        if (bPos != null) {
            drawStationIcon(gl, bPos, COLOR_B);
            drawLabel(gl, bPos, bLabel, COLOR_B);
        }

        gl.glLineWidth(1.0f);
        gl.glPointSize(1.0f);
        gl.glEnable(GL2.GL_LIGHTING);
    }

    /** Vẽ nhãn tên trạm (Text) */
    private void drawLabel(GL2 gl, double[] pos, String label, float[] color) {
        if (label == null || label.isEmpty()) return;

        // Project 3D to 2D screen coords manually (simplified)
        float[] modelview = new float[16];
        float[] projection = new float[16];
        int[] viewport = new int[4];
        gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
        gl.glGetFloatv(GL2.GL_PROJECTION_MATRIX, projection, 0);
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);

        float[] screenPos = new float[3];
        if (project((float)pos[0]*1.1f, (float)pos[1]*1.1f, (float)pos[2]*1.1f, 
                    modelview, projection, viewport, screenPos)) {
            
            textRenderer.beginRendering(viewport[2], viewport[3]);
            // Draw shadow/outline
            textRenderer.setColor(0, 0, 0, 0.8f);
            textRenderer.draw(label, (int)screenPos[0] + 1, (int)screenPos[1] - 1);
            // Draw main text
            textRenderer.setColor(color[0], color[1], color[2], 1.0f);
            textRenderer.draw(label, (int)screenPos[0], (int)screenPos[1]);
            textRenderer.endRendering();
        }
    }

    /** Simple GluProject replacement */
    private boolean project(float objX, float objY, float objZ, 
                            float[] model, float[] proj, int[] view, float[] win) {
        float[] in = {objX, objY, objZ, 1.0f};
        float[] out = new float[4];

        // Multiply: Out = Proj * Model * In
        float[] modelIn = new float[4];
        for (int i = 0; i < 4; i++) {
            modelIn[i] = model[i] * in[0] + model[i+4] * in[1] + model[i+8] * in[2] + model[i+12] * in[3];
        }
        for (int i = 0; i < 4; i++) {
            out[i] = proj[i] * modelIn[0] + proj[i+4] * modelIn[1] + proj[i+8] * modelIn[2] + proj[i+12] * modelIn[3];
        }

        if (out[3] == 0.0) return false;
        out[0] /= out[3];
        out[1] /= out[3];
        out[2] /= out[3];

        win[0] = view[0] + (1.0f + out[0]) * view[2] / 2.0f;
        win[1] = view[1] + (1.0f + out[1]) * view[3] / 2.0f;
        win[2] = (1.0f + out[2]) / 2.0f;
        return true;
    }

    /**
     * Vẽ icon trạm mặt đất: điểm lớn + tia phát sóng + mũi tên lên.
     */
    private void drawStationIcon(GL2 gl, double[] pos, float[] color) {
        // Offset nhỏ ra ngoài bề mặt hành tinh
        double ox = pos[0] * 1.004;
        double oy = pos[1] * 1.004;
        double oz = pos[2] * 1.004;

        // --- Điểm trung tâm (bright core) ---
        gl.glPointSize(14.0f);
        gl.glColor4f(color[0], color[1], color[2], 1.0f);
        gl.glBegin(GL2.GL_POINTS);
        gl.glVertex3d(ox, oy, oz);
        gl.glEnd();

        // --- Hào quang (glow) ---
        gl.glPointSize(22.0f);
        gl.glColor4f(color[0], color[1], color[2], 0.3f);
        gl.glBegin(GL2.GL_POINTS);
        gl.glVertex3d(ox, oy, oz);
        gl.glEnd();

        // --- Cột/mũi tên hướng ra ngoài ---
        double spikeLen = 0.07;
        double ex = ox + pos[0] * spikeLen;
        double ey = oy + pos[1] * spikeLen;
        double ez = oz + pos[2] * spikeLen;

        gl.glLineWidth(2.5f);
        gl.glColor4f(color[0], color[1], color[2], 0.9f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3d(ox, oy, oz);
        gl.glVertex3d(ex, ey, ez);
        gl.glEnd();

        // --- Vòng tròn tín hiệu xung quanh (dấu + trong 2 mặt phẳng) ---
        float size = 0.045f;
        // Compute 2 tangent vectors perpendicular to the radial direction
        double[] n = normalize(new double[]{pos[0], pos[1], pos[2]});
        double[] t1 = perp(n);
        double[] t2 = cross(n, t1);

        gl.glLineWidth(1.5f);
        gl.glColor4f(color[0], color[1], color[2], 0.7f);
        // Ring (32 segments) in the tangent plane
        gl.glBegin(GL2.GL_LINE_LOOP);
        int segs = 32;
        for (int i = 0; i < segs; i++) {
            double a = 2.0 * Math.PI * i / segs;
            double rx = ox + size * (Math.cos(a) * t1[0] + Math.sin(a) * t2[0]);
            double ry = oy + size * (Math.cos(a) * t1[1] + Math.sin(a) * t2[1]);
            double rz = oz + size * (Math.cos(a) * t1[2] + Math.sin(a) * t2[2]);
            gl.glVertex3d(rx, ry, rz);
        }
        gl.glEnd();

        // Cross inside the ring
        gl.glLineWidth(1.2f);
        gl.glColor4f(color[0], color[1], color[2], 0.55f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3d(ox - size * t1[0], oy - size * t1[1], oz - size * t1[2]);
        gl.glVertex3d(ox + size * t1[0], oy + size * t1[1], oz + size * t1[2]);
        gl.glVertex3d(ox - size * t2[0], oy - size * t2[1], oz - size * t2[2]);
        gl.glVertex3d(ox + size * t2[0], oy + size * t2[1], oz + size * t2[2]);
        gl.glEnd();
    }

    // ===================== HELPERS =====================

    /** @deprecated Giữ tương thích — dùng renderGroundStations() thay thế */
    public void renderGroundMarker(GL2 gl, double[] pos, float[] color, float size) {
        if (pos == null) return;
        drawStationIcon(gl, pos, color);
    }

    private static double[] normalize(double[] v) {
        double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        if (len < 1e-9) return new double[]{0, 1, 0};
        return new double[]{v[0]/len, v[1]/len, v[2]/len};
    }

    private static double[] perp(double[] n) {
        // Chọn vector nào không song song nhất với n
        double[] ref = (Math.abs(n[0]) < 0.9) ? new double[]{1,0,0} : new double[]{0,1,0};
        return normalize(cross(n, ref));
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{
            a[1]*b[2] - a[2]*b[1],
            a[2]*b[0] - a[0]*b[2],
            a[0]*b[1] - a[1]*b[0]
        };
    }

    public void dispose() {
        if (textRenderer != null) {
            textRenderer.dispose();
            textRenderer = null;
        }
    }
}
