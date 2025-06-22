package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientVideoScreen;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractCameraPlayer implements IVideoPlayer {
    protected ClientVideoScreen screen;
    protected Framebuffer framebuffer;

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
        framebuffer = new SimpleFramebuffer(2, 2, true);
    }

    @Override
    public void cleanup() {
        framebuffer.delete();
    }

    @Override
    public int getTextureId() {
        return framebuffer.getColorAttachment();
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
        return framebuffer.textureWidth;
    }

    @Override
    public int getHeight() {
        return framebuffer.textureHeight;
    }

    @Override
    public boolean flippedY() {
        return true;
    }
}
