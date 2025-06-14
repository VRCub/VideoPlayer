package com.github.squi2rel.vp.network;

import io.netty.buffer.ByteBuf;
import org.joml.Vector3f;

import java.nio.charset.StandardCharsets;

public class ByteBufUtils {
    public static String readString(ByteBuf buf, int maxLength) {
        int len = buf.readUnsignedShort();
        if (len > maxLength) throw new IllegalStateException(String.format("length(%d) exceeds max length(%d)", len, maxLength));
        byte[] data = new byte[len];
        buf.readBytes(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void writeString(ByteBuf buf, String str) {
        byte[] data = str.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(data.length);
        buf.writeBytes(data);
    }

    public static Vector3f readVec3(ByteBuf buf) {
        return new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void writeVec3(ByteBuf buf, Vector3f v) {
        buf.writeFloat(v.x);
        buf.writeFloat(v.y);
        buf.writeFloat(v.z);
    }
}
