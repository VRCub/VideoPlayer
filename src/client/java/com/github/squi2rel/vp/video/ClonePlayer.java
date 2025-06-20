package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientVideoScreen;
import com.github.squi2rel.vp.provider.VideoInfo;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ClonePlayer implements IVideoPlayer {
    public final VideoPlayer source;
    private final Vector3f p1, p2, p3, p4;

    private final ClientVideoScreen screen;

    public ClonePlayer(ClientVideoScreen screen, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, VideoPlayer source) {
        this.screen = screen;
        this.p1 = new Vector3f(p1);
        this.p2 = new Vector3f(p2);
        this.p3 = new Vector3f(p3);
        this.p4 = new Vector3f(p4);
        this.source = source;
    }

    @Override
    public VideoScreen getScreen() {
        return source.getScreen();
    }

    @Override
    public VideoScreen getTrackingScreen() {
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
        return source.getTextureId();
    }

    @Override
    public void stop() {
    }

    @Override
    public void pause(boolean pause) {
    }

    @Override
    public boolean isPaused() {
        return source.isPaused();
    }

    @Override
    public void setVolume(int volume) {
        source.setVolume(volume);
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
        return source.getProgress();
    }

    @Override
    public long getTotalProgress() {
        return source.getTotalProgress();
    }

    @Override
    public void setTargetTime(long targetTime) {
    }

    @Override
    public void init() {
    }

    @Override
    public void updateTexture() {
    }

    @Override
    public void draw(Matrix4f mat) {
        float sx = p1.sub(p4, tmp1).length() / (source.videoWidth * Math.abs(screen.u1 - screen.u2));
        float sy = p1.sub(p2, tmp1).length() / (source.videoHeight * Math.abs(screen.v1 - screen.v2));
        boolean vertical;
        float scale;
        if (sx < sy) {
            scale = sx / sy;
            vertical = true;
        } else {
            scale = sy / sx;
            vertical = false;
        }
        if (scale == 1) {
            draw(mat, source.getTextureId(), p1, p2, p3, p4, screen.u1, screen.v1, screen.u2, screen.v2);
            return;
        }
        float inv = (1 - scale) / 2;
        if (vertical) {
            draw(mat, source.getTextureId(), p1.lerp(p2, inv, tmp1), p2.lerp(p1, inv, tmp2), p3.lerp(p4, inv, tmp3), p4.lerp(p3, inv, tmp4), screen.u1, screen.v1, screen.u2, screen.v2);
        } else {
            draw(mat, source.getTextureId(), p1.lerp(p4, inv, tmp1), p2.lerp(p3, inv, tmp2), p3.lerp(p2, inv, tmp3), p4.lerp(p1, inv, tmp4), screen.u1, screen.v1, screen.u2, screen.v2);
        }
    }
}
