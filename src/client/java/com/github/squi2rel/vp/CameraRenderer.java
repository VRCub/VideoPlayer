package com.github.squi2rel.vp;

import com.github.squi2rel.vp.mixin.client.GameRendererAccessor;
import com.github.squi2rel.vp.mixin.client.MinecraftClientAccessor;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Pool;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class CameraRenderer {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static final Pool pool = new Pool(3);
    private static final Camera camera = new Camera();
    public static boolean rendering = false;
    public static boolean renderSelf = false;

    public static void renderWorld(Entity entity, Framebuffer framebuffer, int fov) {
        if (client.world == null) return;
        Camera c = client.gameRenderer.getCamera();
        camera.update(client.world, entity, false, false, client.getRenderTickCounter().getTickDelta(true));
        camera.updateEyeHeight();
        Matrix4f proj = client.gameRenderer.getBasicProjectionMatrix(fov);
        MatrixStack matrices = new MatrixStack();
        proj.mul(matrices.peek().getPositionMatrix());
        Matrix4f base = client.gameRenderer.getBasicProjectionMatrix(fov);
        RenderSystem.setProjectionMatrix(base, ProjectionType.PERSPECTIVE);
        Quaternionf rot = camera.getRotation().conjugate(new Quaternionf());
        Matrix4f mat = new Matrix4f().rotation(rot);
        client.worldRenderer.setupFrustum(camera.getPos(), mat, base);
        MinecraftClientAccessor access = (MinecraftClientAccessor) client;
        Framebuffer old = access.getFramebuffer();
        access.setFramebuffer(framebuffer);
        framebuffer.beginWrite(true);
        rendering = true;
        renderSelf = entity != client.player;
        ((GameRendererAccessor) client.gameRenderer).setCamera(camera);
        client.worldRenderer.render(pool, client.getRenderTickCounter(), false, camera, client.gameRenderer, mat, proj);
        ((GameRendererAccessor) client.gameRenderer).setCamera(c);
        renderSelf = false;
        rendering = false;
        RenderSystem.colorMask(false, false, false, true);
        framebuffer.setClearColor(0, 0, 0, 1);
        framebuffer.clear();
        RenderSystem.colorMask(true, true, true, true);
        framebuffer.endWrite();
        access.setFramebuffer(old);
        GlStateManager._viewport(0, 0, old.viewportWidth, old.viewportHeight);
    }
}
