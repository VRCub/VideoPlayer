package com.github.squi2rel.vp.video;

import net.minecraft.client.MinecraftClient;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.opengl.GL21.*;

public class PBOManager {
    private final int[] id = new int[2];
    private boolean allocated = false, next = false;
    private ByteBuffer buffer;
    private final ReentrantLock lock = new ReentrantLock();

    private static void initPBO(int id, int width, int height) {
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, id);
        glBufferData(GL_PIXEL_UNPACK_BUFFER, width * height * 4L, GL_STREAM_DRAW);
    }

    public void init(int width, int height) {
        if (!allocated) {
            glGenBuffers(id);
            allocated = true;
        }
        int prevPBO = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
        initPBO(id[0], width, height);
        initPBO(id[1], width, height);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, prevPBO);
        buffer = ByteBuffer.allocateDirect(width * height * 4);
    }

    public void release() {
        lock.lock();
        if (allocated) {
            int[] i = new int[]{id[0], id[1]};
            MinecraftClient.getInstance().execute(() -> glDeleteBuffers(i));
            allocated = false;
            buffer = null;
        }
        lock.unlock();
    }

    public void bind() {
        if (!allocated) throw new IllegalStateException("PBO not allocated");
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, next ? id[1] : id[0]);
        next = !next;
    }

    public ByteBuffer map() {
        lock.lock();
        if (!allocated) throw new IllegalStateException("PBO not allocated");
        return glMapBuffer(GL_PIXEL_UNPACK_BUFFER, GL_WRITE_ONLY, buffer.capacity(), buffer.position(0));
    }

    public void unmap() {
        glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
        lock.unlock();
    }

    public boolean allocated() {
        return allocated;
    }
}
