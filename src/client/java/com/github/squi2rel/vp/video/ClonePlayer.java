package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientVideoScreen;
import com.github.squi2rel.vp.provider.VideoInfo;
import net.minecraft.client.render.VertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public record ClonePlayer(ClientVideoScreen screen, ClientVideoScreen source) implements IVideoPlayer {
    @Override
    public @Nullable ClientVideoScreen screen() {
        return source;
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
    public void play(VideoInfo info) {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public int getTextureId() {
        return source.player.getTextureId();
    }

    @Override
    public void stop() {
        if (source.player != null) source.player.stop();
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
        return source.player == null ? 0 : source.player.getProgress();
    }

    @Override
    public long getTotalProgress() {
        return source.player == null ? 0 : source.player.getTotalProgress();
    }

    @Override
    public void setTargetTime(long targetTime) {
    }

    @Override
    public void init() {
    }

    @Override
    public int getWidth() {
        return source.player.getWidth();
    }

    @Override
    public int getHeight() {
        return source.player.getHeight();
    }

    @Override
    public void updateTexture() {
    }

    @Override
    public void draw(Matrix4f mat, VertexConsumer consumer, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, float u1, float v1, float u2, float v2) {
        boolean fx = source.player.flippedX();
        boolean fy = source.player.flippedY();
        IVideoPlayer.super.draw(mat, consumer, p1, p2, p3, p4, fx ? u2 : u1, fy ? v2 : v1, fx ? u1 : u2, fy ? v1 : v2);
    }
}
