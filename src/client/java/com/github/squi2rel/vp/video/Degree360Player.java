package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientVideoScreen;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.github.squi2rel.vp.VideoPlayerClient.config;

public class Degree360Player extends VideoPlayer implements MetaListener {
    protected float[] vertices;
    protected int vertexCount;

    public Degree360Player(ClientVideoScreen screen, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4) {
        super(screen, p1, p2, p3, p4);
    }

    @Override
    public void draw(Matrix4f mat, ClientVideoScreen s) {
        super.draw(mat, s);
        int gray = (int) (config.brightness / 100.0 * 255);
        int color = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < vertexCount; i++) {
            int idx = i * 5;
            bufferBuilder.vertex(mat, vertices[idx], vertices[idx + 1], vertices[idx + 2]).texture(vertices[idx + 3], vertices[idx + 4]).color(color);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    @Override
    public void onMetaChanged() {
        int pos = screen.meta.getOrDefault("pos", 537395712);
        float x = p1.x + (pos & 1023) - 512;
        float y = p1.y + (pos >> 10 & 1023) - 512;
        float z = p1.z + (pos >> 20 & 1023) - 512;
        vertices = genVertices(
                screen.meta.getOrDefault("radius", 10),
                screen.meta.getOrDefault("lat", 32),
                screen.meta.getOrDefault("lon", 32),
                x, y, z
        );
        vertexCount = vertices.length / 5;
    }

    protected static float[] genVertices(float radius, int latSegments, int lonSegments, float ox, float oy, float oz) {
        int vertexCount = latSegments * (lonSegments + 1) * 2;
        float[] data = new float[vertexCount * 5];

        int idx = 0;
        for (int lat = 0; lat < latSegments; lat++) {
            double theta1 = Math.PI * lat / latSegments;
            double theta2 = Math.PI * (lat + 1) / latSegments;
            for (int lon = 0; lon <= lonSegments; lon++) {
                double phi = 2 * Math.PI * lon / lonSegments;
                float y1 = (float) (radius * Math.cos(theta1));
                float y2 = (float) (radius * Math.cos(theta2));
                float r1 = (float) (radius * Math.sin(theta1));
                float r2 = (float) (radius * Math.sin(theta2));
                float x1 = (float) (r1 * Math.cos(phi));
                float x2 = (float) (r2 * Math.cos(phi));
                float z1 = (float) (r1 * Math.sin(phi));
                float z2 = (float) (r2 * Math.sin(phi));
                float u = (float) lon / lonSegments;
                float v1 = (float) lat / latSegments;
                float v2 = (float) (lat + 1) / latSegments;
                data[idx++] = x1 + ox;
                data[idx++] = y1 + oy;
                data[idx++] = z1 + oz;
                data[idx++] = u;
                data[idx++] = v1;
                data[idx++] = x2 + ox;
                data[idx++] = y2 + oy;
                data[idx++] = z2 + oz;
                data[idx++] = u;
                data[idx++] = v2;
            }
        }

        return data;
    }
}
