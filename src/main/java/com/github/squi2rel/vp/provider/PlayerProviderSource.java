package com.github.squi2rel.vp.provider;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PlayerProviderSource implements IProviderSource {
    private final ServerPlayerEntity player;

    public PlayerProviderSource(ServerPlayerEntity entity) {
        player = entity;
    }

    @Override
    public String name() {
        return player.getGameProfile().getName();
    }

    @Override
    public void reply(String text) {
        player.sendMessage(Text.of(text));
    }
}
