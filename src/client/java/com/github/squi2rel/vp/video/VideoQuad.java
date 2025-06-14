package com.github.squi2rel.vp.video;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL21.*;

public class VideoQuad {

    private int textureId;
    private int width;
    private int height;
    private boolean textureInitialized = false;
    private final PBOManager pbo = new PBOManager();

    public VideoQuad(int width, int height) {
        this.width = width;
        this.height = height;
        initializeTexture();
        pbo.init(width, height);
    }

    public synchronized void resize(int width, int height) {
        this.width = width;
        this.height = height;
        regenTexture();
        pbo.init(width, height);
    }

    private void initializeTexture() {
        textureId = glGenTextures();
        regenTexture();
    }

    public synchronized void stop() {
        pbo.release();
    }

    private void regenTexture() {
        RenderSystem.bindTexture(textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        RenderSystem.bindTexture(0);
        textureInitialized = true;
    }

    public synchronized void updateTexture(ByteBuffer frameData) {
        if (!pbo.allocated()) return;
        RenderSystem.bindTexture(textureId);
        RenderSystem.pixelStore(GL_UNPACK_ALIGNMENT, 4);
        RenderSystem.pixelStore(GL_UNPACK_ROW_LENGTH, width);
        RenderSystem.pixelStore(GL_UNPACK_SKIP_ROWS, 0);
        RenderSystem.pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
        int prevPBO = glGetInteger(GL_PIXEL_UNPACK_BUFFER_BINDING);
        pbo.bind();
        ByteBuffer buf = pbo.map();
        if (buf.remaining() == frameData.remaining()) buf.put(frameData);
        pbo.unmap();
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, prevPBO);
        RenderSystem.bindTexture(0);
    }

    public void cleanup() {
        if (textureInitialized) {
            MinecraftClient.getInstance().execute(() -> {
                glDeleteTextures(textureId);
                pbo.release();
            });
            textureInitialized = false;
        }
    }

    public int getTextureId() {
        return textureId;
    }
}