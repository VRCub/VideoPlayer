package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.provider.VideoInfo;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class StreamListener {
    public static MediaPlayerFactory factory;
    private final Object syncLock = new Object();
    private MediaPlayer player;
    private Consumer<Boolean> playing = seekable -> {};
    private Runnable stopped = () -> {};
    private Runnable errored = () -> {};
    private Runnable timeout = () -> {};
    private final VideoInfo info;

    public StreamListener(VideoInfo info) {
        this.info = info;
        player = factory.mediaPlayers().newMediaPlayer();
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(10000);
                if (player.status().isPlaying()) return;
                MediaPlayer p = player;
                synchronized (syncLock) {
                    if (player == null) return;
                    player = null;
                    timeout.run();
                    stopped.run();
                }
                p.controls().stopAsync();
                p.release();
            } catch (InterruptedException ignored) {
            }
        });
        player.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                playing.accept(player.status().isSeekable());
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                synchronized (syncLock) {
                    if (player == null) return;
                    stopped.run();
                    mediaPlayer.release();
                    player = null;
                }
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                synchronized (syncLock) {
                    if (player == null) return;
                    errored.run();
                    stopped.run();
                    mediaPlayer.controls().stopAsync();
                    mediaPlayer.release();
                    player = null;
                }
            }
        });
    }

    public long getProgress() {
        return player.status().time();
    }

    public boolean isPlaying() {
        return player.status().isPlaying();
    }

    public void playing(Consumer<Boolean> playing) {
        this.playing = playing;
    }

    public void stopped(Runnable stopped) {
        this.stopped = stopped;
    }

    public void errored(Runnable errored) {
        this.errored = errored;
    }

    public void timeout(Runnable timeout) {
        this.timeout = timeout;
    }

    public void listen() {
        player.media().play(info.path(), info.vlcParams());
    }

    public void cancel() {
        synchronized (syncLock) {
            MediaPlayer p = player;
            player = null;
            p.controls().stopAsync();
            p.release();
        }
    }

    public static void load() {
        factory = new MediaPlayerFactory("--no-video", "--aout=none", "--no-xlib", "--intf=dummy", "--quiet");
    }
}
