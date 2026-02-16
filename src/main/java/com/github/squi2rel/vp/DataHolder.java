package com.github.squi2rel.vp;

import com.github.squi2rel.vp.network.ServerPacketHandler;
import com.github.squi2rel.vp.video.VideoArea;
import com.github.squi2rel.vp.video.VideoScreen;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class DataHolder {
    public static ServerConfig config = new ServerConfig();
    public static ArrayList<UUID> allPlayers = new ArrayList<>();
    public static HashMap<UUID, String> playerDim = new HashMap<>();

    public static final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("videoplayer.json");
    public static HashMap<String, HashMap<String, VideoArea>> areas = new HashMap<>();

    private static final Gson gson = new Gson();
    private static final ReentrantLock lock = new ReentrantLock();

    public static MinecraftServer server;

    public static void update() {
        PlayerManager pm = server.getPlayerManager();
        lock();
        for (UUID uuid : allPlayers) {
            ServerPlayerEntity player = pm.getPlayer(uuid);
            if (player == null) continue;
            String dim = player.getServerWorld().getRegistryKey().getValue().toString();
            HashMap<String, VideoArea> all = areas.get(dim);
            if (all == null || all.isEmpty()) continue;
            for (VideoArea area : all.values()) {
                if (area.inBounds(player.getPos())) {
                    if (area.addPlayer(player.getUuid())) {
                        ServerPacketHandler.sendTo(player, ServerPacketHandler.createArea(area));
                        if (area.screens.isEmpty()) {
                            ServerPacketHandler.sendTo(player, ServerPacketHandler.loadArea(area));
                            continue;
                        }
                        ServerPacketHandler.sendTo(player, ServerPacketHandler.createScreen(area.screens));
                        ServerPacketHandler.sendTo(player, ServerPacketHandler.loadArea(area));
                        ServerPacketHandler.sendTo(player, ServerPacketHandler.updatePlaylist(area.screens));
                        player.sendMessage(Text.literal("进入观影区 " + area.name).formatted(Formatting.DARK_AQUA), true);
                    }
                } else {
                    if (area.removePlayer(player.getUuid())) {
                        ServerPacketHandler.sendTo(player, ServerPacketHandler.unloadArea(area));
                        ServerPacketHandler.sendTo(player, ServerPacketHandler.removeArea(area));
                        player.sendMessage(Text.literal("离开观影区 " + area.name).formatted(Formatting.DARK_AQUA), true);
                    }
                }
            }
        }
        for (Map.Entry<UUID, String> entry : playerDim.entrySet()) {
            ServerPlayerEntity player = pm.getPlayer(entry.getKey());
            if (player == null) continue;
            String dim = player.getServerWorld().getRegistryKey().getValue().toString();
            if (!dim.equals(entry.getValue())) {
                HashMap<String, VideoArea> map = areas.get(entry.getValue());
                if (map == null) continue;
                for (VideoArea area : map.values()) {
                    if (area.removePlayer(player.getUuid())) {
                        ServerPacketHandler.sendTo(player, ServerPacketHandler.unloadArea(area));
                        ServerPacketHandler.sendTo(player, ServerPacketHandler.removeArea(area));
                    }
                }
            }
        }
        for (ServerPlayerEntity player : PlayerLookup.all(server)) {
            playerDim.put(player.getUuid(), player.getServerWorld().getRegistryKey().getValue().toString());
        }
        unlock();
    }

    public static void lock() {
        lock.lock();
    }

    public static void unload(MinecraftServer s) {
        PlayerManager pm = s.getPlayerManager();
        lock();
        for (HashMap<String, VideoArea> map : areas.values()) {
            for (VideoArea area : map.values()) {
                if (!area.hasPlayer()) continue;
                byte[] data = ServerPacketHandler.removeArea(area);
                area.forEachPlayer(u -> ServerPacketHandler.sendTo(pm.getPlayer(u), data));
            }
        }
        unlock();
    }

    public static void playerJoin(ServerPlayerEntity player) {
        ServerPacketHandler.sendTo(player, ServerPacketHandler.config(VideoPlayerMain.version, config));
    }

    public static void playerLeave(UUID uuid) {
        lock();
        allPlayers.remove(uuid);
        playerDim.remove(uuid);
        unlock();
        CompletableFuture.runAsync(() -> {
            lock.lock();
            for (HashMap<String, VideoArea> value : areas.values()) {
                for (VideoArea area : value.values()) {
                    area.removePlayer(uuid);
                }
            }
            lock.unlock();
        });
    }

    public static void unlock() {
        lock.unlock();
    }

    public static void stop(MinecraftServer server) {
        save();
        unload(server);
    }

    public static void save() {
        lock();
        ArrayList<VideoArea> all = new ArrayList<>();
        for (HashMap<String, VideoArea> child : areas.values()) {
            all.addAll(child.values());
        }
        config.areas = all;
        writeString(configPath, gson.toJson(config));
        unlock();
    }

    public static void load(MinecraftServer server) {
        DataHolder.server = server;
        lock();
        try {
            config = gson.fromJson(readString(configPath), ServerConfig.class);
        } catch (Exception e) {
            config = new ServerConfig();
            save();
        }
        for (VideoArea area : config.areas) {
            for (VideoScreen screen : area.screens) {
                if (screen.meta == null) screen.meta = new HashMap<>();
            }
            area.initServer();
            area.afterLoad();
            areas.computeIfAbsent(area.dim, k -> new HashMap<>()).put(area.name, area);
        }
        config.areas = null;
        unlock();
    }

    public static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeString(Path path, String str) {
        try {
            Files.writeString(path, str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
