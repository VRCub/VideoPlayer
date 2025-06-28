package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientVideoScreen;
import com.github.squi2rel.vp.provider.VideoInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.github.squi2rel.vp.VideoPlayerClient.config;

@SuppressWarnings("unused")
public interface IVideoPlayer {
    Vector3f tmp1 = new Vector3f(), tmp2 = new Vector3f(), tmp3 = new Vector3f(), tmp4 = new Vector3f();

    @Nullable ClientVideoScreen screen();

    @Nullable ClientVideoScreen getTrackingScreen();

    boolean canPause();

    void init();

    static boolean accept(VideoInfo info) {
        return false;
    }

    int getWidth();

    int getHeight();

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

    default void swapTexture() {
    }

    void updateTexture();

    default boolean isPostUpdate() {
        return false;
    }

    default boolean flippedX() {
        return false;
    }

    default boolean flippedY() {
        return false;
    }

    default void draw(Matrix4f mat, ClientVideoScreen s) {
        ClientVideoScreen screen = screen();
        if (screen == null || screen.player == null) return;
        Vector3f p1 = s.p1, p2 = s.p2, p3 = s.p3, p4 = s.p4;
        float sx = p1.sub(p4, tmp1).length() / (getWidth() * Math.abs(s.u1 - s.u2));
        float sy = p1.sub(p2, tmp1).length() / (getHeight() * Math.abs(s.v1 - s.v2));
        boolean vertical;
        float scale;
        if (sx < sy) {
            scale = sx / sy;
            vertical = true;
        } else {
            scale = sy / sx;
            vertical = false;
        }
        boolean fx = flippedX();
        boolean fy = flippedY();
        if (scale == 1) {
            draw(mat, getTextureId(), p1, p2, p3, p4, fx ? s.u2 : s.u1, fy ? s.v2 : s.v1, fx ? s.u1 : s.u2, fy ? s.v1 : s.v2);
            return;
        }
        float inv = (1 - scale) / 2;
        if (vertical) {
            draw(mat, s, p1, p2, p1, p2, fx, fy, inv, p3.lerp(p4, inv, tmp3), p4.lerp(p3, inv, tmp4));
        } else {
            draw(mat, s, p1, p2, p3, p4, fx, fy, inv, p3.lerp(p2, inv, tmp3), p4.lerp(p1, inv, tmp4));
        }
    }

    private void draw(Matrix4f mat, ClientVideoScreen s, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, boolean fx, boolean fy, float inv, Vector3f lerp, Vector3f lerp2) {
        draw(mat, getTextureId(), p1.lerp(p4, inv, tmp1), p2.lerp(p3, inv, tmp2), lerp, lerp2, fx ? s.u2 : s.u1, fy ? s.v2 : s.v1, fx ? s.u1 : s.u2, fy ? s.v1 : s.v2);
    }

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
