package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.CameraRenderer;
import com.github.squi2rel.vp.ClientVideoScreen;
import com.github.squi2rel.vp.mixin.client.ClientWorldAccessor;
import com.github.squi2rel.vp.provider.VideoInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.UUID;

public class EntityCameraPlayer extends AbstractCameraPlayer implements MetaListener {
    public Entity entity;
    public UUID uuid;
    public int fov = 70;
    public int width = 256, height = 256;

    public EntityCameraPlayer(ClientVideoScreen screen) {
        super(screen);
    }

    @Override
    public void play(VideoInfo info) {
        uuid = UUID.fromString(info.rawPath());
        entity = getEntity(uuid);
    }

    @Override
    public void stop() {
        entity = null;
        uuid = null;
    }

    @Override
    public void updateTexture() {
        if (uuid == null) return;
        if (entity == null) {
            entity = getEntity(uuid);
            if (entity == null || entity.isRemoved()) {
                entity = null;
                return;
            }
        }
        if (width > 1 && height > 1 && (framebuffer.textureWidth != width || framebuffer.textureHeight != height)) {
            framebuffer.resize(width, height);
        }
        CameraRenderer.renderWorld(entity, framebuffer, fov);
    }

    @Override
    public void onMetaChanged() {
        fov = screen.meta.getOrDefault("fov", 70);
        int size = screen.meta.getOrDefault("size", 256 << 12 | 256);
        width = size >> 12 & 4095;
        height = size & 4095;
    }

    public static Entity getEntity(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        return ((ClientWorldAccessor) client.world).getEntityManager().getLookup().get(uuid);
    }
}
