package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.CameraRenderer;
import com.github.squi2rel.vp.ClientVideoScreen;
import com.github.squi2rel.vp.provider.VideoInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.UUID;

public class EntityCameraPlayer extends AbstractCameraPlayer {
    public Entity player;
    public UUID uuid;
    public int fov = 70;

    public EntityCameraPlayer(ClientVideoScreen screen) {
        super(screen);
    }

    @Override
    public void play(VideoInfo info) {
        uuid = UUID.fromString(info.rawPath());
        player = getPlayer(uuid);
        for (String param : info.params()) {
            String[] split = StringUtils.split(param, "=");
            if (Objects.equals(split[0], "fov")) fov = Integer.parseInt(split[1]);
        }
    }

    @Override
    public void stop() {
        player = null;
        uuid = null;
    }

    @Override
    public void updateTexture() {
        if (uuid == null) return;
        if (player == null) {
            player = getPlayer(uuid);
            if (player == null) return;
        }
        Window window = MinecraftClient.getInstance().getWindow();
        int width = window.getWidth();
        int height = window.getHeight();
        if (width > 1 && height > 1 && (framebuffer.textureWidth != width || framebuffer.textureHeight != height)) {
            framebuffer.resize(width, height);
        }
        CameraRenderer.renderWorld(player, framebuffer, fov);
    }

    public static Entity getPlayer(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        return client.world.getPlayerByUuid(uuid);
    }
}
