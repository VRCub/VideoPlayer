package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.provider.VideoInfo;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class StreamListener implements IVideoListener {
    private static final ConcurrentHashMap<MediaPlayer, StreamListener> references = new ConcurrentHashMap<>();
    public static MediaPlayerFactory factory;
    private MediaPlayer player;
    private Consumer<Boolean> playing = seekable -> {};
    private Runnable stopped = () -> {};
    private Runnable errored = () -> {};
    private Runnable timeout = () -> {};
    private final VideoInfo info;

    private static final MediaPlayerEventAdapter callback = new MediaPlayerEventAdapter() {
        @Override
        public void playing(MediaPlayer mediaPlayer) {
            System.out.println("playing event triggered");
            StreamListener listener = references.get(mediaPlayer);
            if (listener == null) return;
            listener.playing.accept(listener.player.status().isSeekable());
        }

        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            System.out.println("stopped event triggered");
            finish(mediaPlayer);
        }

        @Override
        public void finished(MediaPlayer mediaPlayer) {
            System.out.println("Finished event triggered");
            finish(mediaPlayer);
        }

        @Override
        public void error(MediaPlayer mediaPlayer) {
            System.out.println("errored event triggered");
            StreamListener listener = references.get(mediaPlayer);
            if (listener == null) return;
            synchronized (listener) {
                if (listener.player == null) return;
                references.remove(mediaPlayer);
                listener.errored.run();
                listener.stopped.run();
                listener.player = null;
                runAsync(() -> {
                    mediaPlayer.controls().stop();
                    mediaPlayer.release();
                });
            }
        }
    };

    private static void finish(MediaPlayer mediaPlayer) {
        StreamListener listener = references.get(mediaPlayer);
        if (listener == null) return;
        synchronized (listener) {
            if (listener.player == null) return;
            references.remove(mediaPlayer);
            listener.player = null;
            listener.stopped.run();
        }
        mediaPlayer.release();
    }

    public StreamListener(VideoInfo info) {
        this.info = info;
        player = factory.mediaPlayers().newMediaPlayer();
        runAsync(() -> {
            try {
                Thread.sleep(10000);
                if (isPlaying()) return;
                MediaPlayer p = player;
                synchronized (this) {
                    if (player == null) return;
                    references.remove(p);
                    player = null;
                    timeout.run();
                    stopped.run();
                }
                p.controls().stop();
                p.release();
            } catch (Exception ignored) {
            }
        });
        player.events().addMediaPlayerEventListener(callback);
    }

    public static boolean accept(VideoInfo info) {
        return !info.path().isEmpty();
    }

    @Override
    public long getProgress() {
        return player.status().time();
    }

    @Override
    public boolean isPlaying() {
        return player != null;
    }

    @Override
    public void playing(Consumer<Boolean> playing) {
        this.playing = playing;
    }

    @Override
    public void stopped(Runnable stopped) {
        this.stopped = stopped;
    }

    @Override
    public void errored(Runnable errored) {
        this.errored = errored;
    }

    @Override
    public void timeout(Runnable timeout) {
        this.timeout = timeout;
    }

    @Override
    public void listen() {
        references.put(player, this);
        player.media().play(info.path(), info.params());
    }

    @Override
    public void cancel() {
        MediaPlayer p = player;
        synchronized (this) {
            if (player == null) return;
            references.remove(player);
            player = null;
        }
        runAsync(() -> {
            p.controls().stop();
            p.release();
        });
    }

    public static void runAsync(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.start();
    }

    public static void load() {
        factory = new MediaPlayerFactory("--no-video", "--aout=none", "--no-xlib", "--intf=dummy", "--quiet");
    }
}
