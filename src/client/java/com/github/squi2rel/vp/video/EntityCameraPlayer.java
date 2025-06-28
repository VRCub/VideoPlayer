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
        super.updateTexture();
        if (uuid == null) return;
        if (entity == null) {
            entity = getEntity(uuid);
            if (entity == null || entity.isRemoved()) {
                entity = null;
                return;
            }
        }
        CameraRenderer.renderWorld(entity, pool, framebuffer, entityOutlineFramebuffer, aspect, fov);
    }

    @Override
    public void onMetaChanged() {
        super.onMetaChanged();
        fov = screen.meta.getOrDefault("fov", 70);
    }

    public static Entity getEntity(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        return ((ClientWorldAccessor) client.world).getEntityManager().getLookup().get(uuid);
    }
}
