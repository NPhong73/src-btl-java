package com.planet3d.renderer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.planet3d.model.Planet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Render hành tinh 3D với texture và lighting.
 */
public class PlanetRenderer {

    private Texture planetTexture;
    private Texture atmosphereTexture;
    private boolean textureLoaded = false;
    private String loadedTexturePath = "";

    private static final int SPHERE_SLICES = 64;
    private static final int SPHERE_STACKS = 64;

    /**
     * Tải texture từ file hoặc từ resources (built-in).
     *
     * @param gl          OpenGL context
     * @param texturePath Đường dẫn file texture (null = dùng built-in)
     */
    public void loadTexture(GL2 gl, String texturePath) {
        // Nếu texture đã được tải với cùng path → skip
        if (textureLoaded && loadedTexturePath.equals(texturePath != null ? texturePath : "")) return;

        // Giải phóng texture cũ
        if (planetTexture != null) {
            planetTexture.destroy(gl);
            planetTexture = null;
        }
        textureLoaded = false;

        if (texturePath != null && !texturePath.isBlank()) {
            // 1. Thử đường dẫn tuyệt đối / đúng nguyên văn
            File f = new File(texturePath);
            if (!f.exists()) {
                // 2. Thử tương đối so với thư mục chứa JAR (app root)
                try {
                    File jarDir = new File(
                        PlanetRenderer.class.getProtectionDomain()
                            .getCodeSource().getLocation().toURI()
                    ).getParentFile();
                    // JAR nằm trong target/ → app root là cha của target/
                    // Thử: <jarDir>/<texturePath> và <jarDir>/../<texturePath>
                    File candidate = new File(jarDir, texturePath);
                    if (!candidate.exists()) candidate = new File(jarDir.getParentFile(), texturePath);
                    if (candidate.exists()) f = candidate;
                } catch (Exception ignored) {}
            }
            if (!f.exists()) {
                // 3. Thử tương đối so với thư mục làm việc hiện tại
                f = new File(System.getProperty("user.dir"), texturePath);
            }

            if (f.exists()) {
                try {
                    planetTexture = TextureIO.newTexture(f, true);
                    configureTexture(gl);
                    textureLoaded = true;
                    loadedTexturePath = texturePath;
                    System.out.println("[Renderer] Đã tải texture: " + f.getCanonicalPath());
                    return;
                } catch (IOException e) {
                    System.err.println("[Renderer] Lỗi tải texture: " + e.getMessage());
                }
            }
        }

        // 4. Thử tải từ classpath resources (built-in)
        String[] resourceNames = {
            "/textures/8k_earth_daymap.jpg",
            "/textures/earth_daymap.jpg",
            "/textures/earth.jpg",
        };
        for (String resName : resourceNames) {
            try {
                InputStream is = getClass().getResourceAsStream(resName);
                if (is != null) {
                    String suffix = resName.endsWith(".png") ? "png" : "jpg";
                    planetTexture = TextureIO.newTexture(is, true, suffix);
                    configureTexture(gl);
                    textureLoaded = true;
                    loadedTexturePath = texturePath != null ? texturePath : "";
                    System.out.println("[Renderer] Đã tải texture từ resources: " + resName);
                    return;
                }
            } catch (Exception e) {
                // Thử resource tiếp theo
            }
        }

        System.out.println("[Renderer] Không tìm thấy texture, dùng màu mặc định.");
    }

    private void configureTexture(GL2 gl) {
        planetTexture.enable(gl);
        planetTexture.bind(gl);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
    }

    /**
     * Vẽ hành tinh và các trạm mặt đất đi kèm (để đảm bảo đồng bộ xoay).
     */
    public void render(GL2 gl, GLU glu, Planet planet, float rotAngle, 
                       RouteRenderer routeRenderer, double[] aPos, double[] bPos, 
                       String aLabel, String bLabel) {
        gl.glPushMatrix();

        // 1. Nghiêng trục (axial tilt) - Trái Đất ~23.5°
        gl.glRotatef(-23.5f, 0, 0, 1);

        // 2. Tự quay quanh trục đã nghiêng
        gl.glRotatef(rotAngle, 0, 1, 0);

        if (textureLoaded && planetTexture != null) {
            gl.glEnable(GL2.GL_TEXTURE_2D);
            planetTexture.enable(gl);
            planetTexture.bind(gl);
            gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

            GLUquadric quad = glu.gluNewQuadric();
            glu.gluQuadricTexture(quad, true);
            glu.gluQuadricNormals(quad, GLU.GLU_SMOOTH);
            
            gl.glPushMatrix();
            gl.glRotatef(-90.0f, 1, 0, 0); // North to +Y
            gl.glRotatef(-90.0f, 0, 0, 1); 
            glu.gluSphere(quad, 1.0, SPHERE_SLICES, SPHERE_STACKS);
            gl.glPopMatrix();
            
            glu.gluDeleteQuadric(quad);
            planetTexture.disable(gl);
            gl.glDisable(GL2.GL_TEXTURE_2D);
        } else {
            gl.glDisable(GL2.GL_TEXTURE_2D);
            setDefaultPlanetColor(gl, planet);
            GLUquadric quad = glu.gluNewQuadric();
            glu.gluQuadricNormals(quad, GLU.GLU_SMOOTH);
            gl.glPushMatrix();
            gl.glRotatef(-90.0f, 1, 0, 0);
            gl.glRotatef(-90.0f, 0, 0, 1);
            glu.gluSphere(quad, 1.0, SPHERE_SLICES, SPHERE_STACKS);
            gl.glPopMatrix();
            glu.gluDeleteQuadric(quad);
        }

        // Vẽ lưới kinh tuyến/vĩ tuyến mờ
        drawGrid(gl, glu);

        // Render trạm mặt đất NGAY TẠI ĐÂY (Trong hệ tọa độ của hành tinh)
        if (routeRenderer != null) {
            routeRenderer.renderGroundStations(gl, aPos, bPos, aLabel, bLabel);
        }

        gl.glPopMatrix();
    }

    /** Vẽ lưới kinh/vĩ tuyến */
    private void drawGrid(GL2 gl, GLU glu) {
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 0.08f);
        gl.glLineWidth(0.5f);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        GLUquadric wireQuad = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(wireQuad, GLU.GLU_LINE);
        gl.glPushMatrix();
        gl.glRotatef(-90.0f, 1, 0, 0);
        glu.gluSphere(wireQuad, 1.001, 18, 18);
        gl.glPopMatrix();
        glu.gluDeleteQuadric(wireQuad);

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glLineWidth(1.0f);
    }

    /** Màu hành tinh mặc định khi không có texture */
    private void setDefaultPlanetColor(GL2 gl, Planet planet) {
        String name = planet.getName().toLowerCase();
        if (name.contains("earth")) {
            // Xanh dương-xanh lá (đại dương và đất)
            gl.glColor3f(0.1f, 0.4f, 0.8f);
        } else if (name.contains("mars")) {
            // Đỏ nâu
            gl.glColor3f(0.75f, 0.3f, 0.15f);
        } else if (name.contains("moon")) {
            // Xám
            gl.glColor3f(0.6f, 0.6f, 0.6f);
        } else if (name.contains("jupiter")) {
            // Cam nhạt
            gl.glColor3f(0.85f, 0.65f, 0.45f);
        } else {
            gl.glColor3f(0.5f, 0.5f, 0.7f);
        }
    }

    public boolean isTextureLoaded() { return textureLoaded; }

    public void destroy(GL2 gl) {
        if (planetTexture != null) {
            planetTexture.destroy(gl);
            planetTexture = null;
        }
        textureLoaded = false;
    }
}
