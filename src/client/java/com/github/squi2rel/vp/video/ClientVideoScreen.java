package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.VideoPlayerClient;
import com.github.squi2rel.vp.provider.VideoInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector3f;

import java.util.Objects;

public class ClientVideoScreen extends VideoScreen {
    public IVideoPlayer player = null;
    private VideoInfo toPlay = null;
    private long toSeek = -1;
    private long startTime = System.currentTimeMillis();
    public boolean interactable = true;

    public ClientVideoScreen(VideoArea area, String name, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, String source) {
        super(area, name, v1, v2, v3, v4, source);
    }

    public void updatePlaylist(VideoInfo[] target) {
        infos.clear();
        for (VideoInfo info : target) {
            infos.offer(info);
        }
    }

    @Override
    public void readMeta(ByteBuf buf) {
        super.readMeta(buf);
        metaChanged();
    }

    public void metaChanged() {
        interactable = meta.getOrDefault("interactable", 1) != 0;
        if (player instanceof MetaListener m) m.onMetaChanged();
    }

    public ClientVideoScreen getScreen() {
        return player == null ? this : player.screen();
    }

    public void cleanup() {
        if (player != null) player.cleanup();
    }

    public void draw(MatrixStack matrices, VertexConsumerProvider.Immediate immediate) {
        if (player != null) player.draw(matrices, immediate, this);
    }

    public void swapTexture() {
        if (player != null) player.swapTexture();
    }

    public void updateTexture() {
        if (player != null) player.updateTexture();
    }

    public ClientVideoScreen getTrackingScreen() {
        return player == null ? this : player.getTrackingScreen();
    }

    public void load() {
        VideoPlayerClient.screens.add(this);
        if (source.isEmpty()) {
            if (toPlay != null) play(toPlay);
            return;
        }
        ClientVideoScreen parent = (ClientVideoScreen) area.screens.stream().filter(v -> Objects.equals(v.name, source)).findAny().orElseThrow();
        ((ClientVideoArea) area).afterLoad(() -> player = new ClonePlayer(this, parent));
    }

    public void play(VideoInfo info) {
        if (source.isEmpty()) {
            IVideoPlayer old = player;
            player = VideoPlayers.from(info, this, player);
            if (player == null) return;
            if (player != old) {
                if (old != null) old.cleanup();
                player.init();
            }
            if (player instanceof MetaListener m) m.onMetaChanged();
            if (toSeek > 0) {
                startTime = System.currentTimeMillis() - toSeek;
                player.setTargetTime(toSeek);
                toSeek = -1;
            } else {
                player.setTargetTime(-1);
                startTime = System.currentTimeMillis();
            }
            player.play(info);
        }
    }

    public void setToPlay(VideoInfo info) {
        toPlay = info;
    }

    public void setToSeek(long seek) {
        toSeek = seek;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setProgress(long progress) {
        player.setProgress(progress);
        startTime = System.currentTimeMillis() - progress;
    }

    public void unload() {
        VideoPlayerClient.screens.remove(this);
        if (player != null) player.cleanup();
    }

    public boolean isPostUpdate() {
        return player != null && player.isPostUpdate();
    }

    public static ClientVideoScreen from(VideoScreen screen) {
        return new ClientVideoScreen(screen.area, screen.name, screen.p1, screen.p2, screen.p3, screen.p4, screen.source);
    }
}
