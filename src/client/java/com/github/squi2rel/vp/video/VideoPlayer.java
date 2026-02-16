package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.provider.VideoInfo;
import com.github.squi2rel.vp.vivecraft.Vivecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

import static com.github.squi2rel.vp.VideoPlayerClient.config;

public class VideoPlayer implements IVideoPlayer, MetaListener {
    public final Vector3f p1, p2, p3, p4;
    protected VlcDecoder decoder;
    protected VideoQuad quad;
    protected boolean initialized = false;
    protected boolean changed = false;
    protected long targetTime = -1;
    protected boolean is3d = false;
    public int videoWidth, videoHeight;

    protected final ClientVideoScreen screen;

    public VideoPlayer(ClientVideoScreen screen, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4) {
        this.screen = screen;
        this.p1 = new Vector3f(p1);
        this.p2 = new Vector3f(p2);
        this.p3 = new Vector3f(p3);
        this.p4 = new Vector3f(p4);
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
    public void updateTexture() {
        if (changed || !initialized) return;
        ByteBuffer buf = decoder.decodeNextFrame();
        if (buf == null || buf.capacity() == 0) return;
        quad.updateTexture(buf);
    }

    @Override
    public synchronized void init() {
        if (initialized) throw new IllegalStateException("already initialized");

        decoder = new VlcDecoder();
        decoder.onSizeChanged((w, h) -> {
            changed = true;
            MinecraftClient.getInstance().execute(() -> {
                videoWidth = w;
                videoHeight = h;
                quad.resize(w, h);
                changed = false;
            });
        });
        decoder.onFinish(() -> MinecraftClient.getInstance().execute(() -> quad.resize(1, 1)));

        quad = new VideoQuad(decoder.getWidth(), decoder.getHeight());

        initialized = true;
    }

    @Override
    public int getWidth() {
        return is3d ? videoWidth / 2 : videoWidth;
    }

    @Override
    public int getHeight() {
        return videoHeight;
    }

    @Override
    public void play(VideoInfo info) {
        if (targetTime > 0) {
            String[] params = info.params();
            String[] newParams = new String[params.length + 1];
            System.arraycopy(params, 0, newParams, 0, params.length);
            newParams[newParams.length - 1] = ":start-time=" + targetTime / 1000f;
            info = new VideoInfo(info.playerName(), info.name(), info.path(), info.rawPath(), info.expire(), info.seekable(), newParams);
        }
        decoder.onPlay(() -> decoder.submit(() -> decoder.setVolume(config.volume)));
        decoder.init(info);
    }

    @Override
    public int getTextureId() {
        if (initialized) {
            return quad.getTextureId();
        }
        return -1;
    }

    @Override
    public void stop() {
        decoder.stop();
        quad.stop();
    }

    @Override
    public boolean canPause() {
        return decoder.canPause();
    }

    @Override
    public void pause(boolean pause) {
        decoder.pause(pause);
    }

    @Override
    public boolean isPaused() {
        return decoder.isPaused();
    }

    @Override
    public void setVolume(int volume) {
        decoder.setVolume(volume);
    }

    @Override
    public boolean canSetProgress() {
        return decoder.canSetProgress();
    }

    @Override
    public void setProgress(long progress) {
        decoder.setProgress(progress);
    }

    @Override
    public long getProgress() {
        return decoder.getProgress();
    }

    @Override
    public long getTotalProgress() {
        return decoder.getTotalProgress();
    }

    @Override
    public void setTargetTime(long targetTime) {
        this.targetTime = targetTime;
    }

    public void setRate(float rate) {
        decoder.setRate(rate);
    }

    public float getRate() {
        return decoder.getRate();
    }

    @Override
    public synchronized void cleanup() {
        initialized = false;
        if (decoder != null) decoder.cleanup();
        if (quad != null) quad.cleanup();
    }

    @Override
    public void onMetaChanged() {
        is3d = screen.meta.getOrDefault("3d", 0) != 0;
    }

    @Override
    public void draw(Matrix4f mat, VertexConsumer consumer, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, float u1, float v1, float u2, float v2) {
        if (is3d) {
            draw3D(mat, consumer, p1, p2, p3, p4, u1, v1, u2, v2);
            return;
        }
        IVideoPlayer.super.draw(mat, consumer, p1, p2, p3, p4, u1, v1, u2, v2);
    }

    public void draw3D(Matrix4f mat, VertexConsumer consumer, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, float u1, float v1, float u2, float v2) {
        if (Vivecraft.loaded && Vivecraft.isRightEye()) {
            IVideoPlayer.super.draw(mat, consumer, p1, p2, p3, p4, (u1 + u2) / 2, v1, u2, v2);
        } else {
            IVideoPlayer.super.draw(mat, consumer, p1, p2, p3, p4, u1, v1, (u1 + u2) / 2, v2);
        }
    }
}