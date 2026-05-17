package com.planet3d.renderer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.planet3d.model.Satellite;
import com.planet3d.physics.OrbitalMechanics;

import java.util.List;

/**
 * Render các vệ tinh, quỹ đạo và inter-satellite links.
 */
public class SatelliteRenderer {

    private static final double SAT_SIZE = 0.025;
    private static final int ORBIT_POINTS = 120;

    /**
     * Vẽ tất cả vệ tinh và quỹ đạo.
     */
    public void render(GL2 gl, GLU glu, List<Satellite> satellites, int selectedId, boolean showOrbits) {
        gl.glDisable(GL2.GL_TEXTURE_2D);
        for (Satellite sat : satellites) {
            if (!sat.isActive()) continue;
            double[] pos = sat.getPosition3D();
            if (pos == null || (pos[0] == 0 && pos[1] == 0 && pos[2] == 0)) continue;
            boolean isSelected = sat.getId() == selectedId;
            if (showOrbits) drawOrbit(gl, sat);
            drawGroundTrack(gl, sat);
            drawSatellite(gl, glu, sat, isSelected);
        }
    }

    private void drawSatellite(GL2 gl, GLU glu, Satellite sat, boolean selected) {
        double[] pos = sat.getPosition3D();
        float[] color = sat.getColorRGB();

        gl.glPushMatrix();
        gl.glTranslated(pos[0], pos[1], pos[2]);
        gl.glDisable(GL2.GL_LIGHTING);

        if (selected) {
            gl.glColor4f(1.0f, 1.0f, 0.0f, 0.3f);
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            GLUquadric halo = glu.gluNewQuadric();
            glu.gluSphere(halo, SAT_SIZE * 2.0, 12, 12);
            glu.gluDeleteQuadric(halo);
        }

        gl.glColor3f(color[0], color[1], color[2]);
        GLUquadric quad = glu.gluNewQuadric();
        glu.gluSphere(quad, selected ? SAT_SIZE * 1.5 : SAT_SIZE, 12, 12);
        glu.gluDeleteQuadric(quad);

        // Anten symbol
        double s = SAT_SIZE * 1.2;
        gl.glColor4f(color[0], color[1], color[2], 0.8f);
        gl.glLineWidth(1.5f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3d(-s, 0, 0); gl.glVertex3d(s, 0, 0);
        gl.glVertex3d(0, -s, 0); gl.glVertex3d(0, s, 0);
        gl.glEnd();
        gl.glLineWidth(1.0f);

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopMatrix();
    }

    private void drawOrbit(GL2 gl, Satellite sat) {
        double[] normal = sat.getOrbitNormal();
        double[] pos = sat.getPosition3D();
        if (normal == null || pos == null) return;

        float[] color = sat.getColorRGB();
        gl.glColor4f(color[0], color[1], color[2], 0.25f);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glLineWidth(1.0f);
        gl.glDisable(GL2.GL_LIGHTING);

        double r = Math.sqrt(pos[0]*pos[0] + pos[1]*pos[1] + pos[2]*pos[2]);
        double[] u = normalize(pos);
        double[] v = normalize(cross(normal, u));

        gl.glBegin(GL2.GL_LINE_LOOP);
        for (int i = 0; i < ORBIT_POINTS; i++) {
            double angle = 2.0 * Math.PI * i / ORBIT_POINTS;
            double x = r * (u[0] * Math.cos(angle) + v[0] * Math.sin(angle));
            double y = r * (u[1] * Math.cos(angle) + v[1] * Math.sin(angle));
            double z = r * (u[2] * Math.cos(angle) + v[2] * Math.sin(angle));
            gl.glVertex3d(x, y, z);
        }
        gl.glEnd();
        gl.glEnable(GL2.GL_LIGHTING);
    }

    private void drawGroundTrack(GL2 gl, Satellite sat) {
        double[] pos = sat.getPosition3D();
        if (pos == null) return;
        double len = Math.sqrt(pos[0]*pos[0] + pos[1]*pos[1] + pos[2]*pos[2]);
        if (len == 0) return;

        double[] surface = {pos[0]/len, pos[1]/len, pos[2]/len};
        float[] color = sat.getColorRGB();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glColor4f(color[0], color[1], color[2], 0.3f);
        gl.glLineWidth(0.8f);
        gl.glBegin(GL2.GL_LINES);
        gl.glVertex3d(surface[0], surface[1], surface[2]);
        gl.glVertex3d(pos[0], pos[1], pos[2]);
        gl.glEnd();

        gl.glColor4f(color[0], color[1], color[2], 0.7f);
        gl.glPointSize(3.0f);
        gl.glBegin(GL2.GL_POINTS);
        gl.glVertex3d(surface[0]*1.002, surface[1]*1.002, surface[2]*1.002);
        gl.glEnd();
        gl.glPointSize(1.0f);
        gl.glLineWidth(1.0f);
        gl.glEnable(GL2.GL_LIGHTING);
    }

    /** Vẽ inter-satellite links */
    public void renderLinks(GL2 gl, List<Satellite> satellites, boolean showLinks) {
        if (!showLinks || satellites.size() < 2) return;
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glColor4f(0.3f, 0.6f, 1.0f, 0.15f);
        gl.glLineWidth(0.6f);

        for (int i = 0; i < satellites.size(); i++) {
            Satellite a = satellites.get(i);
            if (!a.isActive() || a.getPosition3D() == null) continue;
            for (int j = i + 1; j < satellites.size(); j++) {
                Satellite b = satellites.get(j);
                if (!b.isActive() || b.getPosition3D() == null) continue;
                double[] pa = a.getPosition3D();
                double[] pb = b.getPosition3D();
                if (OrbitalMechanics.hasLineOfSight(pa, pb)) {
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex3d(pa[0], pa[1], pa[2]);
                    gl.glVertex3d(pb[0], pb[1], pb[2]);
                    gl.glEnd();
                }
            }
        }
        gl.glLineWidth(1.0f);
        gl.glEnable(GL2.GL_LIGHTING);
    }

    private double[] normalize(double[] v) {
        double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        if (len == 0) return new double[]{0, 1, 0};
        return new double[]{v[0]/len, v[1]/len, v[2]/len};
    }

    private double[] cross(double[] a, double[] b) {
        return new double[]{a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]};
    }
}
