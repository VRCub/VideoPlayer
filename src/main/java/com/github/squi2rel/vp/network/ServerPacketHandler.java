package com.github.squi2rel.vp.network;

import com.github.squi2rel.vp.ServerConfig;
import com.github.squi2rel.vp.provider.PlayerProviderSource;
import com.github.squi2rel.vp.provider.VideoInfo;
import com.github.squi2rel.vp.provider.VideoProviders;
import com.github.squi2rel.vp.video.VideoArea;
import com.github.squi2rel.vp.DataHolder;
import com.github.squi2rel.vp.video.VideoScreen;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.github.squi2rel.vp.VideoPlayerMain.LOGGER;
import static com.github.squi2rel.vp.network.ByteBufUtils.readString;
import static com.github.squi2rel.vp.network.ByteBufUtils.writeString;
import static com.github.squi2rel.vp.video.VideoScreen.MAX_NAME_LENGTH;
import static com.github.squi2rel.vp.network.PacketID.*;

public class ServerPacketHandler {
    public static void handle(ServerPlayerEntity player, ByteBuf buf) {
        short type = buf.readUnsignedByte();
        LOGGER.info("server type: {}", type);
        switch (type) {
            case CONFIG -> {
                ByteBufUtils.readString(buf, 16);
                DataHolder.lock();
                DataHolder.allPlayers.add(player.getUuid());
                DataHolder.unlock();
            }
            case REQUEST -> {
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                VideoScreen screen = area.getScreen(readName(buf));
                if (screen == null) return;
                String url = ByteBufUtils.readString(buf, 256);
                if (fetchSource(player, url, screen::addInfo)) return;
            }
            case SYNC -> {
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                VideoScreen screen = area.getScreen(readName(buf));
                if (screen == null || screen.currentPlaying() == null) return;
                sendTo(player, sync(screen, screen.getProgress()));
            }
            case CREATE_AREA -> {
                // TODO check permission
                VideoArea area = VideoArea.from(ByteBufUtils.readVec3(buf), ByteBufUtils.readVec3(buf), readName(buf), player.getWorld().getRegistryKey().getValue().toString());
                area.initServer();
                DataHolder.lock();
                DataHolder.areas.computeIfAbsent(area.dim, k -> new HashMap<>()).put(area.name, area);
                DataHolder.unlock();
            }
            case REMOVE_AREA -> {
                // TODO check permission
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                DataHolder.lock();
                DataHolder.areas.get(area.dim).remove(area.name).remove();
                if (area.hasPlayer()) {
                    byte[] data = removeArea(area);
                    PlayerManager pm = Objects.requireNonNull(player.getServer()).getPlayerManager();
                    area.forEachPlayer(p -> sendTo(pm.getPlayer(p), data));
                }
                DataHolder.unlock();
            }
            case CREATE_SCREEN -> {
                // TODO check permission
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                VideoScreen screen = VideoScreen.read(buf, area);
                screen.initServer();
                DataHolder.lock();
                area.addScreen(screen);
                if (area.hasPlayer()) {
                    byte[] data = createScreen(List.of(screen));
                    PlayerManager pm = Objects.requireNonNull(player.getServer()).getPlayerManager();
                    area.forEachPlayer(p -> sendTo(pm.getPlayer(p), data));
                }
                DataHolder.unlock();
            }
            case REMOVE_SCREEN -> {
                // TODO check permission
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                DataHolder.lock();
                VideoScreen removed = area.removeScreen(readName(buf));
                if (removed != null && area.hasPlayer()) {
                    byte[] data = removeScreen(removed);
                    PlayerManager pm = Objects.requireNonNull(player.getServer()).getPlayerManager();
                    area.forEachPlayer(p -> sendTo(pm.getPlayer(p), data));
                }
                DataHolder.unlock();
            }
            case SKIP -> {
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                VideoScreen screen = area.getScreen(readName(buf));
                if (screen == null) return;
                boolean force = buf.readBoolean();
                if (force) {
                    // TODO check permission
                    screen.skip();
                    return;
                }
                screen.voteSkip(player.getUuid());
            }
            case SKIP_PERCENT -> {
                // TODO check permission
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                VideoScreen screen = area.getScreen(readName(buf));
                if (screen == null) return;
                screen.setSkipPercent(buf.readFloat());
            }
            case IDLE_PLAY -> {
                // TODO
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                VideoScreen screen = area.getScreen(readName(buf));
                if (screen == null) return;
                readString(buf, 1024);
            }
            case SLICE -> {
                // TODO check permission
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                VideoScreen screen = area.getScreen(readName(buf));
                if (screen == null) return;
                readUV(buf, screen);
                if (area.hasPlayer()) {
                    byte[] data = slice(screen, screen.u1, screen.v1, screen.u2, screen.v2);
                    PlayerManager pm = Objects.requireNonNull(player.getServer()).getPlayerManager();
                    area.forEachPlayer(p -> sendTo(pm.getPlayer(p), data));
                }
            }
            case OPEN_MENU -> {
                // TODO
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                VideoScreen screen = area.getScreen(readName(buf));
                if (screen == null) return;
            }
            case SET_META -> {
                short id = buf.readUnsignedByte();
                if (id > Action.VALUES.length) {
                    player.networkHandler.disconnect(Text.of("Unknown action type: " + id));
                    return;
                }
                Action action = Action.VALUES[id];
                VideoArea area = getArea(player, readName(buf));
                if (area == null) return;
                VideoScreen screen = area.getScreen(readName(buf));
                if (screen == null) return;
                int value = buf.readInt();
                if (!action.verify(value)) {
                    player.networkHandler.disconnect(Text.of("Invalid value: " + value));
                    return;
                }
                action.apply(screen, value);
                if (area.hasPlayer()) {
                    byte[] data = setMeta(screen, id, value);
                    PlayerManager pm = Objects.requireNonNull(player.getServer()).getPlayerManager();
                    area.forEachPlayer(p -> sendTo(pm.getPlayer(p), data));
                }
            }
            default -> player.networkHandler.disconnect(Text.of("Unknown packet type: " + type));
        }
        if (buf.readableBytes() > 0) {
            player.networkHandler.disconnect(Text.of("Illegal packet! Remaining: " + buf.readableBytes()));
        }
    }

    private static boolean fetchSource(ServerPlayerEntity player, String url, Consumer<VideoInfo> cb) {
        CompletableFuture<VideoInfo> video = VideoProviders.from(url, new PlayerProviderSource(player));
        if (video == null) {
            player.sendMessage(Text.of("无法解析视频源"));
            return true;
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("start fetch");
                return video.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(v -> {
            try {
                if (v == null) {
                    player.sendMessage(Text.of("无法解析视频源"));
                    return;
                }
                cb.accept(v);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return false;
    }

    private static VideoArea getArea(ServerPlayerEntity player, String name) {
        String dim = player.getServerWorld().getRegistryKey().getValue().toString();
        DataHolder.lock();
        VideoArea area = DataHolder.areas.get(dim).get(name);
        DataHolder.unlock();
        // TODO check bypass permission
        return area != null && area.containsPlayer(player.getUuid()) ? area : null;
    }

    private static String readName(ByteBuf buf) {
        return ByteBufUtils.readString(buf, MAX_NAME_LENGTH);
    }

    private static ByteBuf create(int id) {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
        buf.writeByte((byte) id);
        return buf;
    }

    private static byte[] toByteArray(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return bytes;
    }

    public static void readUV(ByteBuf buf, VideoScreen screen) {
        screen.u1 = buf.readFloat();
        screen.v1 = buf.readFloat();
        screen.u2 = buf.readFloat();
        screen.v2 = buf.readFloat();
    }

    public static void writeUV(ByteBuf buf, VideoScreen screen) {
        buf.writeFloat(screen.u1);
        buf.writeFloat(screen.v1);
        buf.writeFloat(screen.u2);
        buf.writeFloat(screen.v2);
    }

    public static void sendTo(ServerPlayerEntity player, byte[] bytes) {
        ServerPlayNetworking.send(player, new VideoPayload(bytes));
    }

    public static byte[] config(String version, ServerConfig config) {
        ByteBuf buf = create(CONFIG);
        writeString(buf, version);
        writeString(buf, config.remoteControlName);
        buf.writeFloat(config.remoteControlId);
        buf.writeFloat(config.remoteControlRange);
        buf.writeFloat(config.noControlRange);
        return toByteArray(buf);
    }

    public static byte[] request(VideoScreen screen, VideoInfo info) {
        ByteBuf buf = create(REQUEST);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        VideoInfo.write(buf, info);
        return toByteArray(buf);
    }

    public static byte[] sync(VideoScreen screen, long time) {
        ByteBuf buf = create(SYNC);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        buf.writeLong(time);
        return toByteArray(buf);
    }

    public static byte[] createArea(VideoArea area) {
        ByteBuf buf = create(CREATE_AREA);
        writeString(buf, area.name);
        VideoArea.write(buf, area);
        return toByteArray(buf);
    }

    public static byte[] removeArea(VideoArea area) {
        ByteBuf buf = create(REMOVE_AREA);
        writeString(buf, area.name);
        return toByteArray(buf);
    }

    public static byte[] createScreen(List<VideoScreen> screens) {
        ByteBuf buf = create(CREATE_SCREEN);
        writeString(buf, screens.getFirst().area.name);
        buf.writeByte(screens.size());
        for (VideoScreen screen : screens) {
            VideoScreen.write(buf, screen);
            writeUV(buf, screen);
            screen.writeMeta(buf);
        }
        return toByteArray(buf);
    }

    public static byte[] removeScreen(VideoScreen screen) {
        ByteBuf buf = create(REMOVE_SCREEN);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        return toByteArray(buf);
    }

    public static byte[] loadArea(VideoArea area) {
        ByteBuf buf = create(LOAD_AREA);
        writeString(buf, area.name);
        for (VideoScreen screen : area.screens) {
            VideoInfo info = screen.currentPlaying();
            if (info == null) continue;
            writeString(buf, screen.name);
            VideoInfo.write(buf, info);
            buf.writeLong(screen.getProgress());
        }
        return toByteArray(buf);
    }

    public static byte[] unloadArea(VideoArea area) {
        ByteBuf buf = create(UNLOAD_AREA);
        writeString(buf, area.name);
        return toByteArray(buf);
    }

    public static byte[] updatePlaylist(List<VideoScreen> screens) {
        ByteBuf buf = create(UPDATE_PLAYLIST);
        writeString(buf, screens.getFirst().area.name);
        buf.writeByte(screens.size());
        for (VideoScreen screen : screens) {
            writeString(buf, screen.name);
            buf.writeByte(screen.infos.size());
            for (VideoInfo info : screen.infos) {
                writeString(buf, info.playerName());
                writeString(buf, info.name());
            }
        }
        return toByteArray(buf);
    }

    public static byte[] skip(VideoScreen screen) {
        ByteBuf buf = create(SKIP);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        return toByteArray(buf);
    }

    public static byte[] execute(String command) {
        ByteBuf buf = create(EXECUTE);
        writeString(buf, command);
        return toByteArray(buf);
    }

    public static byte[] slice(VideoScreen screen, float u1, float v1, float u2, float v2) {
        ByteBuf buf = create(SLICE);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        buf.writeFloat(u1);
        buf.writeFloat(v1);
        buf.writeFloat(u2);
        buf.writeFloat(v2);
        return toByteArray(buf);
    }

    public static byte[] setMeta(VideoScreen screen, int actionId, int value) {
        ByteBuf buf = create(SET_META);
        buf.writeByte(actionId);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        buf.writeInt(value);
        return toByteArray(buf);
    }
}
