package com.github.squi2rel.vp.provider;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

public record LocalPlayerProviderSource(String remoteName) implements IProviderSource {
    @Override
    public String name() {
        return remoteName;
    }

    @Override
    public void reply(String text) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        player.sendMessage(Text.of(text), false);
    }
}
