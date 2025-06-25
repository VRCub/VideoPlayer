package com.github.squi2rel.vp.mixin.client;

import com.github.squi2rel.vp.CameraRenderer;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "getFramebufferWidth", at = @At("HEAD"), cancellable = true)
    public void getFramebufferWidth(CallbackInfoReturnable<Integer> cir) {
        if (CameraRenderer.rendering) cir.setReturnValue(CameraRenderer.width);
    }

    @Inject(method = "getFramebufferHeight", at = @At("HEAD"), cancellable = true)
    public void getFramebufferHeight(CallbackInfoReturnable<Integer> cir) {
        if (CameraRenderer.rendering) cir.setReturnValue(CameraRenderer.height);
    }
}
