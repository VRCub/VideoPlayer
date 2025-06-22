package com.github.squi2rel.vp.mixin;

import com.github.squi2rel.vp.VideoPlayerMain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.caprica.vlcj.factory.discovery.provider.DirectoryProviderDiscoveryStrategy;
import uk.co.caprica.vlcj.factory.discovery.provider.DiscoveryDirectoryProvider;

import java.util.List;

@Mixin(DirectoryProviderDiscoveryStrategy.class)
public class VlcJMixin {
    @Inject(method = "getSupportedProviders", at = @At("HEAD"), cancellable = true, remap = false)
    protected void getSupportedProviders(CallbackInfoReturnable<List<DiscoveryDirectoryProvider>> cir) {
        cir.setReturnValue(List.of(new DiscoveryDirectoryProvider() {
            @Override
            public int priority() {
                return Integer.MAX_VALUE;
            }

            @Override
            public String[] directories() {
                return new String[]{VideoPlayerMain.libDir.toAbsolutePath().toString()};
            }

            @Override
            public boolean supported() {
                return true;
            }
        }));
    }
}
