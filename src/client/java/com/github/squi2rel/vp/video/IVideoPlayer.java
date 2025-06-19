package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.provider.VideoInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.github.squi2rel.vp.VideoPlayerClient.config;

public interface IVideoPlayer {
    Vector3f tmp1 = new Vector3f(), tmp2 = new Vector3f(), tmp3 = new Vector3f(), tmp4 = new Vector3f();

    VideoScreen getScreen();

    VideoScreen getTrackingScreen();

    boolean canPause();

    void init();

    void play(VideoInfo info);

    void cleanup();

    int getTextureId();

    void stop();

    void pause(boolean pause);

    boolean isPaused();

    void setVolume(int volume);

    boolean canSetProgress();

    void setProgress(long progress);

    long getProgress();

    long getTotalProgress();

    void setTargetTime(long targetTime);

    void updateTexture();

    void draw(Matrix4f mat);

    default void draw(Matrix4f mat, int id, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, float u1, float v1, float u2, float v2) {
        int old = RenderSystem.getShaderTexture(0);
        RenderSystem.setShaderTexture(0, id);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        int gray = (int) (config.brightness / 100.0 * 255);
        int color = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
        bufferBuilder.vertex(mat, p1.x, p1.y, p1.z).texture(u1, v1).color(color);
        bufferBuilder.vertex(mat, p2.x, p2.y, p2.z).texture(u1, v2).color(color);
        bufferBuilder.vertex(mat, p3.x, p3.y, p3.z).texture(u2, v2).color(color);
        bufferBuilder.vertex(mat, p4.x, p4.y, p4.z).texture(u2, v1).color(color);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderTexture(0, old);
    }
}
