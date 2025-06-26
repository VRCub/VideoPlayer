package com.github.squi2rel.vp;

import com.github.squi2rel.vp.mixin.client.GameRendererAccessor;
import com.github.squi2rel.vp.mixin.client.MinecraftClientAccessor;
import com.github.squi2rel.vp.mixin.client.WorldRendererAccessor;
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
    private static final Camera camera = new Camera();
    public static boolean rendering = false;
    public static boolean renderSelf = false;
    public static int width, height;

    public static void renderWorld(Entity entity, Pool pool, Framebuffer framebuffer, Framebuffer entityOutlineFramebuffer, int fov) {
        if (client.world == null) return;
        width = framebuffer.textureWidth;
        height = framebuffer.textureHeight;
        framebuffer.beginWrite(true);
        rendering = true;
        Camera c = client.gameRenderer.getCamera();
        camera.update(client.world, entity, false, false, client.getRenderTickCounter().getTickDelta(true));
        camera.updateEyeHeight();
        MinecraftClientAccessor gameAccess = (MinecraftClientAccessor) client;
        WorldRendererAccessor renderAccess = (WorldRendererAccessor) client.worldRenderer;
        Framebuffer old = gameAccess.getFramebuffer();
        gameAccess.setFramebuffer(framebuffer);
        Framebuffer oldOutline = renderAccess.getEntityOutlineFramebuffer();
        renderAccess.setEntityOutlineFramebuffer(entityOutlineFramebuffer);
        Matrix4f proj = client.gameRenderer.getBasicProjectionMatrix(fov);
        MatrixStack matrices = new MatrixStack();
        proj.mul(matrices.peek().getPositionMatrix());
        Matrix4f base = client.gameRenderer.getBasicProjectionMatrix(fov);
        RenderSystem.setProjectionMatrix(base, ProjectionType.PERSPECTIVE);
        Quaternionf rot = camera.getRotation().conjugate(new Quaternionf());
        Matrix4f pos = new Matrix4f().rotation(rot);
        client.worldRenderer.setupFrustum(camera.getPos(), pos, base);
        renderSelf = entity != client.player;
        client.gameRenderer.setRenderHand(false);
        ((GameRendererAccessor) client.gameRenderer).setCamera(camera);
        client.worldRenderer.render(pool, client.getRenderTickCounter(), false, camera, client.gameRenderer, pos, proj);
        ((GameRendererAccessor) client.gameRenderer).setCamera(c);
        renderSelf = false;
        rendering = false;
        RenderSystem.colorMask(false, false, false, true);
        framebuffer.setClearColor(0, 0, 0, 1);
        framebuffer.clear();
        RenderSystem.colorMask(true, true, true, true);
        framebuffer.endWrite();
        gameAccess.setFramebuffer(old);
        renderAccess.setEntityOutlineFramebuffer(oldOutline);
        GlStateManager._viewport(0, 0, old.viewportWidth, old.viewportHeight);
    }
}
