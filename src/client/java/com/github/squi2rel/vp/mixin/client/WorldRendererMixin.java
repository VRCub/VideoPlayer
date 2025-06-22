package com.github.squi2rel.vp.mixin.client;

import com.github.squi2rel.vp.CameraRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "getEntitiesToRender", at = @At("RETURN"))
    public void addSelf(Camera camera, Frustum frustum, List<Entity> output, CallbackInfoReturnable<Boolean> cir) {
        if (CameraRenderer.renderSelf) {
            output.add(MinecraftClient.getInstance().player);
        }
    }
}
