package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientVideoScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.util.Pool;
import net.minecraft.client.util.Window;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class AbstractCameraPlayer implements IVideoPlayer, MetaListener {
    protected ClientVideoScreen screen;
    protected Framebuffer framebuffer, framebuffer1, framebuffer2, entityOutlineFramebuffer;
    protected boolean first = true;
    protected Pool pool;
    protected float aspect = 16f / 9f;
    protected int targetWidth = 16, targetHeight = 9;
    protected float u = 0, v = 0;

    public AbstractCameraPlayer(ClientVideoScreen screen) {
        this.screen = screen;
    }

    @Override
    public @Nullable ClientVideoScreen screen() {
        return screen;
    }

    @Override
    public @Nullable ClientVideoScreen getTrackingScreen() {
        return screen;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public void init() {
        framebuffer1 = new SimpleFramebuffer(1, 1, true);
        framebuffer2 = new SimpleFramebuffer(1, 1, true);
        entityOutlineFramebuffer = new SimpleFramebuffer(1, 1, true);
        pool = new Pool(3);
    }

    @Override
    public void cleanup() {
        framebuffer1.delete();
        framebuffer2.delete();
        entityOutlineFramebuffer.delete();
        pool.clear();
    }

    @Override
    public void swapTexture() {
        framebuffer = first ? framebuffer1 : framebuffer2;
        first = !first;
    }

    @Override
    public void updateTexture() {
        Window window = MinecraftClient.getInstance().getWindow();
        int windowWidth = window.getFramebufferWidth();
        int windowHeight = window.getFramebufferHeight();
        int width = windowWidth;
        int height = Math.round(width / aspect);

        if (height > windowHeight) {
            height = windowHeight;
            width = Math.round(height * aspect);
        }

        targetWidth = width;
        targetHeight = height;

        u = (float) (framebuffer.textureWidth - width) / framebuffer.textureWidth;
        v = (float) (framebuffer.textureHeight - height) / framebuffer.textureHeight;

        if (windowWidth != 0 && windowHeight != 0 && (framebuffer.textureWidth != windowWidth || framebuffer.textureHeight != windowHeight)) {
            framebuffer.resize(windowWidth, windowHeight);
            entityOutlineFramebuffer.resize(windowWidth, windowHeight);
        }
    }

    @Override
    public void onMetaChanged() {
        aspect = Float.intBitsToFloat(screen.meta.getOrDefault("aspect", Float.floatToIntBits(16f / 9f)));
    }

    @Override
    public int getTextureId() {
        return (first ? framebuffer1 : framebuffer2).getColorAttachment();
    }

    @Override
    public void pause(boolean pause) {
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public void setVolume(int volume) {
    }

    @Override
    public boolean canSetProgress() {
        return false;
    }

    @Override
    public void setProgress(long progress) {
    }

    @Override
    public long getProgress() {
        return 0;
    }

    @Override
    public long getTotalProgress() {
        return 0;
    }

    @Override
    public void setTargetTime(long targetTime) {
    }

    @Override
    public int getWidth() {
        return targetWidth;
    }

    @Override
    public int getHeight() {
        return targetHeight;
    }

    @Override
    public boolean flippedY() {
        return true;
    }

    @Override
    public boolean isPostUpdate() {
        return true;
    }

    @Override
    public void draw(Matrix4f mat, int id, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, float u1, float v1, float u2, float v2) {
        float eu = 1 - u;
        float ev = 1 - v;
        IVideoPlayer.super.draw(mat, id, p1, p2, p3, p4, MathHelper.lerp(u1, u, eu), MathHelper.lerp(v1, v, ev), MathHelper.lerp(u2, u, eu), MathHelper.lerp(v2, v, ev));
    }
}
