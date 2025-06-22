package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.provider.VideoInfo;

public class VideoListeners {
    public static IVideoListener from(VideoInfo info) {
        if (StreamListener.accept(info)) {
            return new StreamListener(info);
        }
        if (PlayerListener.accept(info)) {
            return new PlayerListener();
        }
        return null;
    }
}
