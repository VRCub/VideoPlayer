package com.github.squi2rel.vp.provider;

import com.github.squi2rel.vp.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

public record VideoInfo(String playerName, String name, String path, String rawPath, long expire, boolean seekable, String[] vlcParams) {
    public static void write(ByteBuf buf, VideoInfo i) {
        ByteBufUtils.writeString(buf, i.playerName);
        ByteBufUtils.writeString(buf, i.name);
        ByteBufUtils.writeString(buf, i.path);
        ByteBufUtils.writeString(buf, i.rawPath);
        buf.writeLong(i.expire);
        buf.writeBoolean(i.seekable);
        buf.writeByte(i.vlcParams.length);
        for (String vlcParam : i.vlcParams) {
            ByteBufUtils.writeString(buf, vlcParam);
        }
    }

    public static VideoInfo read(ByteBuf buf) {
        String playerName = ByteBufUtils.readString(buf, 256);
        String name = ByteBufUtils.readString(buf, 256);
        String path = ByteBufUtils.readString(buf, 1024);
        String rawPath = ByteBufUtils.readString(buf, 1024);
        long expire = buf.readLong();
        boolean seekable = buf.readBoolean();
        byte length = buf.readByte();
        String[] vlcParams = new String[length];
        for (int i = 0; i < length; i++) {
            vlcParams[i] = ByteBufUtils.readString(buf, 256);
        }
        return new VideoInfo(playerName, name, path, rawPath, expire, seekable, vlcParams);
    }
}
