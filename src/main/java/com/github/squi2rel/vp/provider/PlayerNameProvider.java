package com.github.squi2rel.vp.provider;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import static com.github.squi2rel.vp.DataHolder.server;

public class PlayerNameProvider implements IVideoProvider {
    public static final Pattern REGEX = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");
    @Override
    public @Nullable CompletableFuture<VideoInfo> from(String str, IProviderSource source) {
        if (REGEX.matcher(str).matches()) return CompletableFuture.completedFuture(new VideoInfo(source.name(), "PLAYER VIEW", "", str, -1, false, NO_PARAMS));
        ServerPlayerEntity player = PlayerLookup.all(server).stream().filter(p -> p.getName().getString().equals(str)).findAny().orElse(null);
        if (player == null) return null;
        return CompletableFuture.completedFuture(new VideoInfo(source.name(), "PLAYER VIEW", "", player.getUuidAsString(), -1, false, NO_PARAMS));
    }
}
