package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.DataHolder;
import com.github.squi2rel.vp.network.ByteBufUtils;
import com.github.squi2rel.vp.network.ServerPacketHandler;
import com.github.squi2rel.vp.provider.NamedProviderSource;
import com.github.squi2rel.vp.provider.VideoInfo;
import com.github.squi2rel.vp.provider.VideoProviders;
import io.netty.buffer.ByteBuf;
import net.minecraft.server.PlayerManager;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.squi2rel.vp.DataHolder.server;
import static com.github.squi2rel.vp.VideoPlayerMain.LOGGER;

public class VideoScreen {
    public static final int MAX_NAME_LENGTH = 32;

    public transient VideoArea area;
    public String name;
    public Vector3f p1, p2, p3, p4;
    public float u1 = 0, v1 = 0, u2 = 1, v2 = 1;
    public String source;
    public float skipPercent = 0.5f;
    public ArrayDeque<VideoInfo> infos = new ArrayDeque<>();
    public boolean muted = false;
    public boolean interactable = true;
    private transient IVideoListener now;
    private transient CompletableFuture<IVideoListener> nextTask;
    private transient HashSet<UUID> skipped;
    private transient ReentrantLock lock;

    public VideoScreen(VideoArea area, String name, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4, String source) {
        this.area = area;
        this.name = name;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
        this.p4 = p4;
        this.source = source;
    }

    public void readMeta(ByteBuf buf) {
        muted = buf.readBoolean();
        interactable = buf.readBoolean();
    }

    public void writeMeta(ByteBuf buf) {
        buf.writeBoolean(muted);
        buf.writeBoolean(interactable);
    }

    public void syncInfo() {
        PlayerManager pm = server.getPlayerManager();
        lock();
        byte[] data = ServerPacketHandler.updatePlaylist(List.of(this));
        unlock();
        area.forEachPlayer(u -> ServerPacketHandler.sendTo(pm.getPlayer(u), data));
    }

    public void initServer() {
        skipped = new HashSet<>();
        lock = new ReentrantLock();
        playNext();
    }

    public synchronized void addInfo(VideoInfo info) {
        LOGGER.info("added info: {} {} {}", info.playerName(), info.name(), info.path());
        lock();
        infos.offer(info);
        unlock();
        playNext();
        syncInfo();
    }

    public long getProgress() {
        VideoInfo info = infos.peek();
        if (info == null || !info.seekable()) return -1;
        return now.getProgress();
    }

    public synchronized void voteSkip(UUID uuid) {
        skipped.add(uuid);
        if (shouldSkip()) skip();
    }

    public synchronized void setSkipPercent(float skipPercent) {
        this.skipPercent = skipPercent;
        if (shouldSkip()) skip();
    }

    private boolean shouldSkip() {
        return skipped.size() > area.players() * skipPercent;
    }

    public synchronized void skip() {
        lock();
        if (nextTask != null) {
            nextTask.cancel(true);
            nextTask = null;
        }
        if (now != null) {
            now.cancel();
            now = null;
        }
        infos.poll();
        unlock();
        playNext();
        syncInfo();
    }

    public synchronized void removePlayer(UUID uuid) {
        skipped.remove(uuid);
        if (shouldSkip()) skip();
    }

    public synchronized void remove() {
        lock();
        if (now != null) now.cancel();
        if (nextTask != null) nextTask.cancel(true);
        now = null;
        nextTask = null;
        infos.clear();
        unlock();
        syncInfo();
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public synchronized void playNext() {
        if (nextTask != null && !nextTask.isDone() || now != null && now.isPlaying()) return;
        now = null;
        skipped.clear();
        nextTask = CompletableFuture.supplyAsync(() -> {
            lock();
            VideoInfo info = infos.peek();
            unlock();
            if (info == null) {
                if (area.hasPlayer()) {
                    PlayerManager pm = server.getPlayerManager();
                    byte[] data = ServerPacketHandler.skip(this);
                    DataHolder.lock();
                    area.forEachPlayer(u -> ServerPacketHandler.sendTo(pm.getPlayer(u), data));
                    DataHolder.unlock();
                    syncInfo();
                }
                return null;
            }
            LOGGER.info("playing info: {} {} {}", info.playerName(), info.name(), info.path());
            if (info.expire() > 0 && System.currentTimeMillis() > info.expire()) {
                try {
                    LOGGER.info("expired, {} {}", info.expire(), info.name());
                    info = Objects.requireNonNull(VideoProviders.from(info.rawPath(), new NamedProviderSource(info.playerName()))).get();
                } catch (Exception ignored) {
                }
            }
            if (info == null || info.expire() > 0 && System.currentTimeMillis() > info.expire()) {
                return null;
            }
            if (area.hasPlayer()) {
                PlayerManager pm = server.getPlayerManager();
                byte[] data = ServerPacketHandler.request(this, info);
                DataHolder.lock();
                area.forEachPlayer(u -> ServerPacketHandler.sendTo(pm.getPlayer(u), data));
                DataHolder.unlock();
            }
            syncInfo();
            try {
                info = Objects.requireNonNull(VideoProviders.from(info.rawPath(), new NamedProviderSource(info.playerName()))).get();
            } catch (Exception ignored) {
            }
            return now = VideoListeners.from(info);
        });
        nextTask.thenAccept(s -> {
            if (s == null) return;
            synchronized (this) {
                nextTask = null;
                s.stopped(() -> {
                    lock();
                    infos.poll();
                    unlock();
                    playNext();
                });
                s.listen();
            }
        });
    }

    public VideoInfo currentPlaying() {
        return infos.peek();
    }

    public static VideoScreen read(ByteBuf buf, VideoArea area) {
        return new VideoScreen(
                area,
                ByteBufUtils.readString(buf, MAX_NAME_LENGTH),
                ByteBufUtils.readVec3(buf),
                ByteBufUtils.readVec3(buf),
                ByteBufUtils.readVec3(buf),
                ByteBufUtils.readVec3(buf),
                ByteBufUtils.readString(buf, MAX_NAME_LENGTH)
        );
    }

    public static void write(ByteBuf buf, VideoScreen screen) {
        ByteBufUtils.writeString(buf, screen.name);
        ByteBufUtils.writeVec3(buf, screen.p1);
        ByteBufUtils.writeVec3(buf, screen.p2);
        ByteBufUtils.writeVec3(buf, screen.p3);
        ByteBufUtils.writeVec3(buf, screen.p4);
        ByteBufUtils.writeString(buf, screen.source);
    }
}
