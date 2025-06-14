package com.github.squi2rel.vp;

import com.github.squi2rel.vp.provider.VideoInfo;
import com.github.squi2rel.vp.video.*;
import org.joml.Vector3f;

import java.util.Objects;

public class ClientVideoScreen extends VideoScreen {
    public IVideoPlayer player = null;
    private VideoInfo toPlay = null;
    private long toSeek = -1;
    private long startTime = System.currentTimeMillis();

    public ClientVideoScreen(VideoArea area, String name, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, String source) {
        super(area, name, v1, v2, v3, v4, source);
    }

    public void updatePlaylist(VideoInfo[] target) {
        infos.clear();
        for (VideoInfo info : target) {
            infos.offer(info);
        }
    }

    public void load() {
        if (source.isEmpty()) {
            VideoPlayer p = new VideoPlayer(this, p1, p2, p3, p4);
            p.init();
            player = p;
            VideoPlayerClient.players.add(p);
            if (toPlay != null) {
                startTime = System.currentTimeMillis() - toSeek;
                p.setTargetTime(toSeek);
                p.play(toPlay);
                toPlay = null;
            } else {
                startTime = System.currentTimeMillis();
            }
        } else {
            ClientVideoScreen parent = (ClientVideoScreen) area.screens.stream().filter(v -> Objects.equals(v.name, source)).findAny().orElseThrow();
            ((ClientVideoArea) area).afterLoad(() -> {
                player = new ClonePlayer(p1, p2, p3, p4, (VideoPlayer) parent.player);
                VideoPlayerClient.players.add(player);
            });
        }
    }

    public void play(VideoInfo info) {
        if (player == null) return;
        reset();
        player.setTargetTime(-1);
        player.play(info);
    }

    public void setToPlay(VideoInfo info) {
        toPlay = info;
    }

    public void setToSeek(long seek) {
        toSeek = seek;
    }

    public long getStartTime() {
        return startTime;
    }

    private void reset() {
        toSeek = -1;
        startTime = System.currentTimeMillis();
    }

    public void unload() {
        VideoPlayerClient.players.remove(player);
        if (player instanceof VideoPlayer vp) vp.cleanup();
    }

    public static ClientVideoScreen from(VideoScreen screen) {
        return new ClientVideoScreen(screen.area, screen.name, screen.p1, screen.p2, screen.p3, screen.p4, screen.source);
    }
}
