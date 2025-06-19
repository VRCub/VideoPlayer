package com.github.squi2rel.vp.mixin.client;

import com.github.squi2rel.vp.VideoPlayerClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    //@Inject(at = @At("HEAD"), method = "clearWorld")
    @Inject(at = @At("HEAD"), method = "method_54134", remap = false)
    public void clearWorld(CallbackInfo ci) {
        VideoPlayerClient.disconnectHandler.run();
    }
}
