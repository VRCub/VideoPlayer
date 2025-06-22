package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.ClientVideoScreen;
import com.github.squi2rel.vp.provider.VideoInfo;

public class VideoPlayers {
    public static IVideoPlayer from(VideoInfo info, ClientVideoScreen screen, IVideoPlayer old) {
        if (StreamListener.accept(info)) {
            if (old instanceof VideoPlayer) return old;
            return new VideoPlayer(screen, screen.p1, screen.p2, screen.p3, screen.p4);
        }
        if (PlayerListener.accept(info)) {
            if (old instanceof EntityCameraPlayer) return old;
            return new EntityCameraPlayer(screen);
        }
        return null;
    }
}
