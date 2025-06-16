package com.github.squi2rel.vp;

import com.github.squi2rel.vp.network.ByteBufUtils;
import com.github.squi2rel.vp.network.VideoPayload;
import com.github.squi2rel.vp.provider.VideoInfo;
import com.github.squi2rel.vp.video.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Vector3f;

import java.util.Objects;

import static com.github.squi2rel.vp.VideoPlayerClient.areas;
import static com.github.squi2rel.vp.network.ByteBufUtils.writeString;
import static com.github.squi2rel.vp.video.VideoScreen.MAX_NAME_LENGTH;
import static com.github.squi2rel.vp.network.PacketID.*;
import static com.github.squi2rel.vp.VideoPlayerMain.LOGGER;

public class ClientPacketHandler {
    public static void handle(ByteBuf buf) {
        short type = buf.readUnsignedByte();
        switch (type) {
            case CONFIG -> {
                String version = ByteBufUtils.readString(buf, 16);
                if (!VideoPlayerClient.checkVersion(version)) {
                    Objects.requireNonNull(MinecraftClient.getInstance().player).sendMessage(Text.of("服务器VideoPlayer版本和本地版本不匹配! 本地版本为" + VideoPlayerMain.version + ", 服务器版本为" + version), false);
                    break;
                }
                VideoPlayerClient.remoteControlName = ByteBufUtils.readString(buf, 256);
                VideoPlayerClient.remoteControlId = buf.readFloat();
                VideoPlayerClient.remoteControlRange = buf.readFloat();
                VideoPlayerClient.noControlRange = buf.readFloat();
                VideoPlayerClient.connected = true;
                config(VideoPlayerMain.version);
            }
            case REQUEST -> areas.get(readName(buf)).getScreen(readName(buf)).play(VideoInfo.read(buf));
            case SYNC -> {
                String areaName = readName(buf);
                String name = ByteBufUtils.readString(buf, 32);
                areas.get(areaName).getScreen(name).player.setProgress(buf.readLong());
            }
            case CREATE_AREA -> areas.put(readName(buf), ClientVideoArea.read(buf));
            case REMOVE_AREA -> areas.remove(readName(buf)).remove();
            case CREATE_SCREEN -> {
                ClientVideoArea area = areas.get(readName(buf));
                short size = buf.readUnsignedByte();
                for (int i = 0; i < size; i++) {
                    ClientVideoScreen screen = ClientVideoScreen.from(VideoScreen.read(buf, area));
                    area.addScreen(screen);
                }
            }
            case REMOVE_SCREEN -> areas.get(readName(buf)).remove(readName(buf));
            case LOAD_AREA -> {
                ClientVideoArea area = areas.get(readName(buf));
                while (buf.readableBytes() != 0) {
                    ClientVideoScreen screen = area.getScreen(readName(buf));
                    screen.setToPlay(VideoInfo.read(buf));
                    screen.setToSeek(buf.readLong());
                }
                area.load();
            }
            case UNLOAD_AREA -> areas.get(readName(buf)).unload();
            case UPDATE_PLAYLIST -> {
                ClientVideoArea area = areas.get(readName(buf));
                if (area == null) break;
                short size = buf.readUnsignedByte();
                for (int i = 0; i < size; i++) {
                    ClientVideoScreen screen = area.getScreen(readName(buf));
                    short len = buf.readUnsignedByte();
                    VideoInfo[] infos = new VideoInfo[len];
                    for (int j = 0; j < len; j++) {
                        infos[j] = new VideoInfo(ByteBufUtils.readString(buf, 256), ByteBufUtils.readString(buf, 256), null, null, -1, false, null);
                    }
                    screen.updatePlaylist(infos);
                }
            }
            case SKIP -> {
                ClientVideoScreen screen = areas.get(readName(buf)).getScreen(readName(buf));
                if (screen == null) break;
                VideoPlayer player = (VideoPlayer) screen.player;
                if (player == null) break;
                MinecraftClient.getInstance().execute(player::stop);
            }
            case EXECUTE -> {
                MinecraftClient client = MinecraftClient.getInstance();
                CommandDispatcher<FabricClientCommandSource> dispatcher = ClientCommandManager.getActiveDispatcher();
                if (dispatcher != null && client.player != null) {
                    try {
                        dispatcher.execute("vlc " + ByteBufUtils.readString(buf, 1024), (FabricClientCommandSource) client.player.networkHandler.getCommandSource());
                    } catch (CommandSyntaxException e) {
                        client.player.sendMessage(Text.literal("执行指令失败: " + e).formatted(Formatting.RED), false);
                    }
                }
            }
            default -> LOGGER.warn("Unknown packet type: {}", type);
        }
        if (buf.readableBytes() > 0) {
            LOGGER.warn("Bytes remaining: {}, type {}", buf.readableBytes(), type);
        }
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

    private static void send(byte[] bytes) {
        ClientPlayNetworking.send(new VideoPayload(bytes));
    }

    public static void config(String version) {
        ByteBuf buf = create(CONFIG);
        writeString(buf, version);
        send(toByteArray(buf));
    }

    public static void request(VideoScreen screen, String path) {
        ByteBuf buf = create(REQUEST);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        writeString(buf, path);
        send(toByteArray(buf));
    }

    public static void sync(VideoScreen screen) {
        ByteBuf buf = create(SYNC);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        send(toByteArray(buf));
    }

    public static void createArea(Vector3f p1, Vector3f p2, String name) {
        ByteBuf buf = create(CREATE_AREA);
        ByteBufUtils.writeVec3(buf, p1);
        ByteBufUtils.writeVec3(buf, p2);
        writeString(buf, name);
        send(toByteArray(buf));
    }

    public static void removeArea(String area) {
        ByteBuf buf = create(REMOVE_AREA);
        writeString(buf, area);
        send(toByteArray(buf));
    }

    public static void createScreen(VideoScreen screen) {
        ByteBuf buf = create(CREATE_SCREEN);
        writeString(buf, screen.area.name);
        VideoScreen.write(buf, screen);
        send(toByteArray(buf));
    }

    public static void removeScreen(VideoScreen screen) {
        ByteBuf buf = create(REMOVE_SCREEN);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        send(toByteArray(buf));
    }

    public static void skip(VideoScreen screen, boolean force) {
        ByteBuf buf = create(SKIP);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        buf.writeBoolean(force);
        send(toByteArray(buf));
    }

    public static void skipPercent(VideoScreen screen, float percent) {
        ByteBuf buf = create(SKIP_PERCENT);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        buf.writeFloat(percent);
        send(toByteArray(buf));
    }

    public static void idlePlay(VideoScreen screen, String url) {
        ByteBuf buf = create(IDLE_PLAY);
        writeString(buf, screen.area.name);
        writeString(buf, screen.name);
        writeString(buf, url);
        send(toByteArray(buf));
    }
}
