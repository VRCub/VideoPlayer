package com.github.squi2rel.vp.video;

import com.github.squi2rel.vp.provider.VideoInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public interface IVideoPlayer {
    Vector3f v1 = new Vector3f(), v2 = new Vector3f(), v3 = new Vector3f(), v4 = new Vector3f();

    VideoScreen getScreen();

    boolean canPause();

    void init();

    void play(VideoInfo info);

    void cleanup();

    int getTextureId();

    void stop();

    void pause(boolean pause);

    boolean isPaused();

    void setVolume(int volume);

    boolean canSetProgress();

    void setProgress(long progress);

    long getProgress();

    long getTotalProgress();

    void setTargetTime(long targetTime);

    void updateTexture();

    void draw(Matrix4f mat);

    default void draw(Matrix4f mat, int id, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f p4) {
        int old = RenderSystem.getShaderTexture(0);
        RenderSystem.setShaderTexture(0, id);
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferBuilder.vertex(mat, p1.x, p1.y, p1.z).texture(0, 0);
        bufferBuilder.vertex(mat, p2.x, p2.y, p2.z).texture(0, 1);
        bufferBuilder.vertex(mat, p3.x, p3.y, p3.z).texture(1, 1);
        bufferBuilder.vertex(mat, p4.x, p4.y, p4.z).texture(1, 0);
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        RenderSystem.setShaderTexture(0, old);
    }
}
