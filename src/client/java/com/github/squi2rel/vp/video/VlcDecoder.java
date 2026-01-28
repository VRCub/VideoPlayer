package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.VideoPlayerMain;
import com.github.squi2rel.vp.provider.VideoInfo;
import org.lwjgl.BufferUtils;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;

import java.nio.ByteBuffer;
import java.util.function.BiConsumer;

public class VlcDecoder {
    private static MediaPlayerFactory factory;
    private final EmbeddedMediaPlayer mediaPlayer;

    private int width = 1, height = 1;
    private final TextureRenderFormatCallback callback = new TextureRenderFormatCallback();
    private ByteBuffer buffer, glBuffer;

    private BiConsumer<Integer, Integer> sizeListener = (a, b) -> {};
    private Runnable playListener = () -> {}, finishListener = () -> {};

    public VlcDecoder() {
        mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.videoSurface().set(new CallbackVideoSurface(new BufferFormatCallbackAdapter() {
            @Override
            public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
                if (buffer == null || glBuffer == null || width != sourceWidth || height != sourceHeight) {
                    synchronized (callback) {
                        sizeListener.accept(sourceWidth, sourceHeight);
                        width = sourceWidth;
                        height = sourceHeight;
                        int bytes = sourceWidth * sourceHeight * 4;
                        glBuffer = BufferUtils.createByteBuffer(bytes);
                        buffer = BufferUtils.createByteBuffer(bytes);
                        sizeListener.accept(width, height);
                    }
                }
                return new RGBAFormat(sourceWidth, sourceHeight);
            }
        }, callback, true, (a, b) -> {}));
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                playListener.run();
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                finishListener.run();
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                mediaPlayer.submit(() -> mediaPlayer.controls().stop());
            }
        });
    }

    public static void load() {
        VideoPlayerMain.LOGGER.info("loading library");
        factory = new MediaPlayerFactory();
        VideoPlayerMain.LOGGER.info("loaded library");
    }

    public void onSizeChanged(BiConsumer<Integer, Integer> sizeListener) {
        this.sizeListener = sizeListener;
    }

    public void onFinish(Runnable finishListener) {
        this.finishListener = finishListener;
    }

    public void onPlay(Runnable playListener) {
        this.playListener = playListener;
    }

    public void submit(Runnable r) {
        mediaPlayer.submit(r);
    }

    public void init(VideoInfo info) {
        mediaPlayer.media().play(info.path().replace("rtspt://", "rtsp://"), info.params());
    }

    public ByteBuffer decodeNextFrame() {
        return callback.copy();
    }

    public void cleanup() {
        stop();
        StreamListener.releaseExecutor.submit(mediaPlayer::release);
    }

    public void stop() {
        mediaPlayer.submit(() -> mediaPlayer.controls().stop());
        glBuffer = null;
        buffer = null;
    }

    public boolean canPause() {
        return mediaPlayer.status().canPause();
    }

    public void pause(boolean pause) {
        mediaPlayer.controls().setPause(pause);
    }

    public boolean isPaused() {
        return mediaPlayer.status().isPlaying();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setVolume(int volume) {
        mediaPlayer.audio().setVolume(volume);
    }

    public boolean canSetProgress() {
        return mediaPlayer.status().isSeekable();
    }

    public void setProgress(long progress) {
        mediaPlayer.controls().setTime(progress);
    }

    public long getProgress() {
        return mediaPlayer.status().time();
    }

    public long getTotalProgress() {
        return mediaPlayer.status().length();
    }

    private class TextureRenderFormatCallback implements RenderCallback {
        private static final ByteBuffer empty = BufferUtils.createByteBuffer(0);

        public synchronized ByteBuffer copy() {
            if (buffer == null || glBuffer == null || buffer.capacity() != glBuffer.remaining()) return empty;
            buffer.position(0).put(glBuffer).flip();
            return buffer;
        }

        @Override
        public void lock(MediaPlayer m) {
        }

        @Override
        public synchronized void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat, int displayWidth, int displayHeight) {
            if (glBuffer == null) return;
            glBuffer.position(0).put(nativeBuffers[0].position(0).limit(glBuffer.capacity())).flip();
        }

        @Override
        public void unlock(MediaPlayer m) {
        }
    }

    public static class RGBAFormat extends BufferFormat {
        public RGBAFormat(int width, int height) {
            super("RGBA", width, height, new int[] {width * 4}, new int[] {height});
        }
    }
}
