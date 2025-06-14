package com.github.squi2rel.vp.provider;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public interface IVideoProvider {
    String[] NO_PARAMS = new String[0];

    @Nullable CompletableFuture<VideoInfo> from(String str, IProviderSource source);
}
