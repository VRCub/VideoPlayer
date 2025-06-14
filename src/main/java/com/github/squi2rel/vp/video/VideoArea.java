package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.function.Consumer;

public class VideoArea {
    public Vector3f min = new Vector3f();
    public Vector3f max = new Vector3f();
    public String name;
    public String dim;
    private transient HashSet<UUID> players;
    public ArrayList<VideoScreen> screens = new ArrayList<>();

    public VideoArea(Vector3f v1, Vector3f v2, String name, String dim) {
        min.set(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y), Math.min(v1.z, v2.z));
        max.set(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y), Math.max(v1.z, v2.z));
        this.name = name;
        this.dim = dim;
    }

    public void afterLoad(MinecraftServer server) {
        for (VideoScreen screen : screens) {
            screen.area = this;
            screen.initServer(server);
        }
    }

    public synchronized void remove() {
        for (VideoScreen screen : screens) {
            screen.remove();
        }
    }

    public void initServer() {
        players = new HashSet<>();
    }

    public boolean inBounds(Vec3d v) {
        return min.x <= v.x && min.y <= v.y && min.z <= v.z && max.x >= v.x && max.y >= v.y && max.z >= v.z;
    }

    public synchronized boolean addPlayer(UUID uuid) {
        return players.add(uuid);
    }

    public synchronized boolean removePlayer(UUID uuid) {
        boolean removed = players.remove(uuid);
        if (removed) {
            for (VideoScreen screen : screens) {
                screen.removePlayer(uuid);
            }
        }
        return removed;
    }

    public synchronized boolean containsPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public synchronized void forEachPlayer(Consumer<UUID> consumer) {
        for (UUID player : players) {
            consumer.accept(player);
        }
    }

    public synchronized boolean hasPlayer() {
        return !players.isEmpty();
    }

    public synchronized int players() {
        return players.size();
    }

    public void addScreen(VideoScreen screen) {
        screen.area = this;
        screens.add(screen);
    }

    public VideoScreen getScreen(String name) {
        for (VideoScreen screen : screens) {
            if (screen.name.equals(name)) {
                return screen;
            }
        }
        return null;
    }

    public VideoScreen removeScreen(String name) {
        VideoScreen screen = getScreen(name);
        if (screen != null) {
            screens.remove(screen);
            return screen;
        }
        return null;
    }

    public static VideoArea from(Vector3f v, Vector3f v2, String name, String dim) {
        return new VideoArea(v, v2, name, dim);
    }

    public static void write(ByteBuf buf, VideoArea area) {
        ByteBufUtils.writeVec3(buf, area.min);
        ByteBufUtils.writeVec3(buf, area.max);
        ByteBufUtils.writeString(buf, area.name);
        ByteBufUtils.writeString(buf, area.dim);
    }
}
