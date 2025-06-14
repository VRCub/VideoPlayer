package com.github.squi2rel.vp.provider;

import com.github.squi2rel.vp.video.StreamListener;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class NetworkProvider implements IVideoProvider {
    @Override
    public @Nullable CompletableFuture<VideoInfo> from(String str, IProviderSource source) {
        char first = str.charAt(0);
        if (first == '/' || first == '\\' || first == '.' || str.charAt(1) == ':') return null;
        return CompletableFuture.supplyAsync(() -> {
            source.reply("正在解析视频流");
            StreamInfo info = getStreamInfo(str);
            if (info == null) {
                source.reply("解析视频流失败");
                return null;
            }
            return new VideoInfo(source.name(), info.name, str, str, -1, info.seekable, NO_PARAMS);
        });
    }

    private static @Nullable StreamInfo getStreamInfo(String mrl) {
        StreamListener listener = new StreamListener(new VideoInfo(null, null, mrl, null, -1, false, NO_PARAMS));
        CompletableFuture<Boolean> lock = new CompletableFuture<>();
        listener.timeout(() -> lock.complete(null));
        listener.errored(() -> lock.complete(null));
        listener.playing(lock::complete);
        listener.listen();
        boolean seekable;
        try {
            Boolean b = lock.get();
            if (b == null) return null;
            seekable = b;
        } catch (Exception e) {
            return null;
        }
        listener.cancel();
        String path = mrl.toLowerCase();
        String name = "Unknown Stream";
        if (path.startsWith("http") && path.contains(".m3u8")) name = "HLS Stream";
        if (path.startsWith("rtsp://")) name = "RTSP Stream";
        if (path.startsWith("http")) name = "HTTP Stream";
        if (path.startsWith("rtp://")) name = "RTP Stream";
        if (path.startsWith("mms://")) name = "MMS Stream";
        return new StreamInfo(name, seekable);
    }

    private record StreamInfo(String name, boolean seekable) {
    }
}
