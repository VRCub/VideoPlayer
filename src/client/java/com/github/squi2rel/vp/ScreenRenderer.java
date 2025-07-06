package com.github.squi2rel.vp;

import com.github.squi2rel.vp.vivecraft.Vivecraft;
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

import java.util.HashMap;

import static com.github.squi2rel.vp.VideoPlayerClient.*;

@SuppressWarnings({"resource", "DataFlowIssue"})
public class ScreenRenderer {
    private static final HashMap<Integer, RenderLayer> quadsCache = new HashMap<>();

    private static int triangleId;
    private static final RenderLayer VIDEO_TRIANGLES = RenderLayer.of(
            "video_triangles",
            VertexFormats.POSITION_TEXTURE_COLOR,
            VertexFormat.DrawMode.TRIANGLE_STRIP,
            4096,
            true,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(new RenderPhase.ShaderProgram(ShaderProgramKeys.POSITION_TEX_COLOR))
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .texture(new RenderPhase.TextureBase(() -> RenderSystem.setShaderTexture(0, triangleId), () -> {}))
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(true)
    );

    private static final Quaternionf rotation = new Quaternionf();
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
        if (Vivecraft.loaded && Vivecraft.isVRActive()) {
            rotation.setFromNormalized(Vivecraft.getRotation()).invert();
        } else {
            ctx.camera().getRotation().invert(rotation);
        }
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LESS);
        RenderSystem.disableCull();
        VertexConsumerProvider.Immediate immediate = (VertexConsumerProvider.Immediate) ctx.consumers();
        quadsCache.clear();
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

    public static RenderLayer getLayer(int textureId) {
        return quadsCache.computeIfAbsent(textureId, v -> RenderLayer.of(
                "video_quad_" + textureId,
                VertexFormats.POSITION_TEXTURE_COLOR,
                VertexFormat.DrawMode.QUADS,
                32,
                true,
                true,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(new RenderPhase.ShaderProgram(ShaderProgramKeys.POSITION_TEX_COLOR))
                        .texture(new RenderPhase.TextureBase(() -> RenderSystem.setShaderTexture(0, textureId), () -> {}))
                        .cull(RenderPhase.DISABLE_CULLING)
                        .build(true)
        ));
    }

    public static void drawTriangles(int textureId, Runnable r) {
        triangleId = textureId;
        VIDEO_TRIANGLES.startDrawing();
        r.run();
        VIDEO_TRIANGLES.endDrawing();
    }

    public static void rotateMatrix(MatrixStack matrices) {
        matrices.multiply(rotation);
    }
}
