package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientPacketHandler;
import com.github.squi2rel.vp.VideoPlayerClient;
import com.github.squi2rel.vp.provider.VideoInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Vector3f;

import java.util.*;

public class ClientVideoScreen extends VideoScreen {
    public IVideoPlayer player = null;
    private VideoInfo toPlay = null;
    private long toSeek = -1;
    private long startTime = System.currentTimeMillis();
    public boolean interactable = true;

    private long lastAutoSync;
    private int syncFrames;

    private double srtt = -1;
    private double rttvar = -1;
    private static final double ALPHA = 0.125;
    private static final double BETA = 0.25;

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

    public void update() {
        if (player != null) player.updateTexture();

        if (player instanceof VideoPlayer vp && !vp.isPaused()) {
            long syncTime = 150L * Math.max(syncFrames, 1);
            if (meta.getOrDefault("autoSync", 0) != 0 && System.currentTimeMillis() - lastAutoSync >= syncTime) {
                lastAutoSync = System.currentTimeMillis();
                ClientPacketHandler.autoSync(this, System.currentTimeMillis());
            }
        }
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
        syncFrames = 0;
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
        syncFrames = 0;
        player.setProgress(progress);
        startTime = System.currentTimeMillis() - progress;
    }

    public void autoSync(int clientDelay, long syncProgress) {
        if (srtt < 0) {
            srtt = clientDelay;
            rttvar = clientDelay / 2.0;
        } else {
            double delta = Math.abs(clientDelay - srtt);
            if (delta > 1000) return;
            rttvar = (1 - BETA) * rttvar + BETA * delta;
            srtt = (1 - ALPHA) * srtt + ALPHA * clientDelay;
        }

        int rtt = (int) Math.round(srtt);
        syncProgress += rtt / 2;

        if (player instanceof VideoPlayer vp && !vp.isPaused()) {
            if (syncProgress <= 0) return;
            long progress = vp.getProgress();
            if (progress <= 0) return;

            long delta = syncProgress - progress;
            if (delta > -25 && delta <= 25) {
                syncFrames++;
            } else {
                syncFrames--;
            }
            syncFrames = Math.clamp(syncFrames, 0, 7);

            if (syncFrames < 5) {
                if (delta > 10000) {
                    if (vp.getRate() != 3f) vp.setRate(3f);
                } else if (delta > 5000) {
                    if (vp.getRate() != 2f) vp.setRate(2f);
                } else if (delta > 3000) {
                    if (vp.getRate() != 1.5f) vp.setRate(1.5f);
                } else if (delta > 1500) {
                    if (vp.getRate() != 1.4f) vp.setRate(1.4f);
                } else if (delta > 500) {
                    if (vp.getRate() != 1.3f) vp.setRate(1.3f);
                } else if (delta > 100) {
                    if (vp.getRate() != 1.2f) vp.setRate(1.2f);
                } else if (delta > 25) {
                    if (vp.getRate() != 1.1f) vp.setRate(1.1f);
                } else if (delta > -25) {
                    if (vp.getRate() != 1f) vp.setRate(1f);
                } else if (delta > -1000) {
                    if (vp.getRate() != 0.9f) vp.setRate(0.9f);
                } else if (delta > -5000) {
                    if (vp.getRate() != 0.8f) vp.setRate(0.8f);
                } else if (delta > -10000) {
                    vp.stop();
                    MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal("提前太多，失去同步").formatted(Formatting.RED), false);
                }
            }

            if (meta.getOrDefault("debug", 0) != 0) {
                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal(
                        "local: %s, server: %s, rtt: %s, delta: %s, sync: %s/7, rate: %.2f".formatted(
                                progress, syncProgress, rtt, delta, syncFrames, vp.getRate()
                        )
                ).formatted(Formatting.GREEN), false);
            }
        }
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
