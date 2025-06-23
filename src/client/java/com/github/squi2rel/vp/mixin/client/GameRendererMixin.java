package com.github.squi2rel.vp.mixin.client;

import com.github.squi2rel.vp.CameraRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    public void renderHand(Camera camera, float tickDelta, Matrix4f matrix4f, CallbackInfo ci) {
        if (CameraRenderer.rendering) ci.cancel();
    }
}
