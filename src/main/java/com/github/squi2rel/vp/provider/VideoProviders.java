package com.github.squi2rel.vp.provider;

import com.github.squi2rel.vp.VideoPlayerMain;
import com.github.squi2rel.vp.provider.bilibili.BiliBiliLiveProvider;
import com.github.squi2rel.vp.provider.bilibili.BiliBiliVideoProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class VideoProviders {
    public static ArrayList<IVideoProvider> providers = new ArrayList<>();

    public static void register() {
        providers.add(new BiliBiliVideoProvider());
        providers.add(new BiliBiliLiveProvider());
        providers.add(new EntityViewProvider());
        providers.add(new NetworkProvider());
    }

    public static @Nullable CompletableFuture<VideoInfo> from(String str, IProviderSource source) {
        VideoPlayerMain.LOGGER.info("Player {} requested {}", source.name(), str);
        for (IVideoProvider provider : providers) {
            CompletableFuture<VideoInfo> info = provider.from(str, source);
            if (info != null) {
                VideoPlayerMain.LOGGER.info("Using {}", provider.getClass().getSimpleName());
                return info;
            }
        }
        VideoPlayerMain.LOGGER.info("No suitable provider");
        return null;
    }
}
