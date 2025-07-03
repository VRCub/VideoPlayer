package com.github.squi2rel.vp;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.Profilers;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import static com.github.squi2rel.vp.VideoPlayerClient.*;

@SuppressWarnings({"resource", "DataFlowIssue"})
public class VideoRenderer {
    public static int textureId;
    public static final RenderLayer VIDEO_QUADS = RenderLayer.of(
            "video_quads",
            VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormat.DrawMode.QUADS,
            4096,
            true,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(new RenderPhase.ShaderProgram(ShaderProgramKeys.POSITION_TEX_COLOR))
                    .texture(new RenderPhase.TextureBase(() -> RenderSystem.setShaderTexture(0, textureId), () -> {}))
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(true)
    );
    public static final RenderLayer VIDEO_TRIANGLES = RenderLayer.of(
            "video_triangles",
            VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormat.DrawMode.TRIANGLE_STRIP,
            4096,
            true,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(new RenderPhase.ShaderProgram(ShaderProgramKeys.POSITION_TEX_COLOR))
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .texture(new RenderPhase.TextureBase(() -> RenderSystem.setShaderTexture(0, textureId), () -> {}))
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(true)
    );

    private static final Quaternionf tmp = new Quaternionf();
    private static Quaternionf rotation;
    public static float cameraX, cameraY, cameraZ;
    public static boolean skybox;

    public static void render(WorldRenderContext ctx) {
        if (CameraRenderer.rendering) return;
        skybox = false;
        Profiler profiler = Profilers.get();
        profiler.push("video");
        profiler.push("render");
        MatrixStack matrices = ctx.matrixStack();
        matrices.push();
        Vec3d camera = ctx.camera().getPos();
        cameraX = (float) camera.x;
        cameraY = (float) camera.y;
        cameraZ = (float) camera.z;
        rotation = ctx.camera().getRotation();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LESS);
        RenderSystem.disableCull();
        VertexConsumerProvider.Immediate immediate = (VertexConsumerProvider.Immediate) ctx.consumers();
        int old = RenderSystem.getShaderTexture(0);
        for (ClientVideoScreen screen : screens) {
            try {
                screen.draw(matrices, immediate);
            } catch (Exception e) {
                VideoPlayerMain.LOGGER.error("Exception while rendering", e);
            }
        }
        RenderSystem.setShaderTexture(0, old);
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        matrices.pop();
        profiler.pop();
        profiler.pop();
    }

    public static void renderQuads(int textureId, VertexConsumerProvider.Immediate immediate) {
        VideoRenderer.textureId = textureId;
        immediate.draw(VideoRenderer.VIDEO_QUADS);
    }

    public static void rotateMatrix(MatrixStack matrices) {
        matrices.multiply(rotation.invert(tmp));
    }
}
