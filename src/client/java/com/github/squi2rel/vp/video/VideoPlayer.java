package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientVideoScreen;
import com.github.squi2rel.vp.provider.VideoInfo;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

import static com.github.squi2rel.vp.VideoPlayerClient.config;

public class VideoPlayer implements IVideoPlayer {
    public final Vector3f p1, p2, p3, p4;
    private VlcDecoder decoder;
    private VideoQuad quad;
    private boolean initialized = false;
    private boolean changed = false;
    private long targetTime = -1;
    public int videoWidth, videoHeight;

    private final ClientVideoScreen screen;

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
        return videoWidth;
    }

    @Override
    public int getHeight() {
        return videoHeight;
    }

    @Override
    public void play(VideoInfo info) {
        if (targetTime > 0) {
            decoder.onPlay(() -> {
                long time = targetTime;
                MinecraftClient.getInstance().execute(() -> {
                    setVolume(config.volume);
                    decoder.setProgress(time);
                });
                decoder.onPlay(() -> {});
            });
        }
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

    @Override
    public synchronized void cleanup() {
        initialized = false;
        if (decoder != null) decoder.cleanup();
        if (quad != null) quad.cleanup();
    }
}