package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientVideoScreen;
import com.github.squi2rel.vp.VideoRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.GlUsage;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

import static com.github.squi2rel.vp.VideoPlayerClient.config;

public class Degree360Player extends VideoPlayer implements MetaListener {
    protected float[] vertices;
    protected int vertexCount;
    protected VertexBuffer buffer;
    protected boolean dirty = true;
    protected int old;
    protected float x, y, z;
    protected boolean skybox = false;

    public Degree360Player(ClientVideoScreen screen, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4) {
        super(screen, p1, p2, p3, p4);
    }

    @Override
    public synchronized void init() {
        super.init();
        buffer = new VertexBuffer(GlUsage.DYNAMIC_WRITE);
    }

    @Override
    public void draw(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, ClientVideoScreen s) {
        super.draw(matrices, immediate, s);
        matrices.push();
        VideoRenderer.rotateMatrix(matrices);
        if (skybox) {
            VideoRenderer.skybox = true;
        } else {
            matrices.translate(x - VideoRenderer.cameraX, y - VideoRenderer.cameraY, z - VideoRenderer.cameraZ);
        }
        if (vertices == null) return;
        int gray = (int) (config.brightness / 100.0 * 255);
        if (old != gray) {
            dirty = true;
            old = gray;
        }
        int color = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        buffer.bind();
        if (dirty) {
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_TEXTURE_COLOR);
            for (int i = 0; i < vertexCount; i++) {
                int idx = i * 5;
                bufferBuilder.vertex(vertices[idx], vertices[idx + 1], vertices[idx + 2]).texture(vertices[idx + 3], vertices[idx + 4]).color(color);
            }
            buffer.upload(bufferBuilder.end());
            dirty = false;
        }
        VideoRenderer.VIDEO_TRIANGLES.startDrawing();
        buffer.draw(matrices.peek().getPositionMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VideoRenderer.VIDEO_TRIANGLES.endDrawing();
        matrices.pop();
    }

    @Override
    public void onMetaChanged() {
        x = p1.x + Float.intBitsToFloat(screen.meta.getOrDefault("x", 0));
        y = p1.y + Float.intBitsToFloat(screen.meta.getOrDefault("y", 0));
        z = p1.z + Float.intBitsToFloat(screen.meta.getOrDefault("z", 0));
        vertices = genVertices(
                screen.meta.getOrDefault("radius", 10),
                screen.meta.getOrDefault("lat", 32),
                screen.meta.getOrDefault("lon", 32)
        );
        skybox = screen.meta.getOrDefault("skybox", 0) != 0;
        vertexCount = vertices.length / 5;
        dirty = true;
    }

    @Override
    public synchronized void cleanup() {
        super.cleanup();
        buffer.close();
    }

    protected static float[] genVertices(float radius, int latSegments, int lonSegments) {
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
                data[idx++] = x1;
                data[idx++] = y1;
                data[idx++] = z1;
                data[idx++] = u;
                data[idx++] = v1;
                data[idx++] = x2;
                data[idx++] = y2;
                data[idx++] = z2;
                data[idx++] = u;
                data[idx++] = v2;
            }
        }

        return data;
    }
}
