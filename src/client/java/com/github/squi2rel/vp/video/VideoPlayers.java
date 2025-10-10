package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.provider.VideoInfo;

public class VideoPlayers {
    public static IVideoPlayer from(VideoInfo info, ClientVideoScreen screen, IVideoPlayer old) {
        if (StreamListener.accept(info)) {
            if (screen.meta.getOrDefault("360", 0) != 0) {
                if (old != null && old.getClass() == Degree360Player.class) return old;
                return new Degree360Player(screen, screen.p1, screen.p2, screen.p3, screen.p4);
            }
            if (old != null && old.getClass() == VideoPlayer.class) return old;
            return new VideoPlayer(screen, screen.p1, screen.p2, screen.p3, screen.p4);
        }
        if (PlayerListener.accept(info)) {
            if (old != null && old.getClass() == EntityCameraPlayer.class) return old;
            return new EntityCameraPlayer(screen);
        }
        return null;
    }
}
