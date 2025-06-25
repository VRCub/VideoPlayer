package com.github.squi2rel.vp.mixin.client;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {
    @Accessor
    Framebuffer getEntityOutlineFramebuffer();

    @Accessor
    void setEntityOutlineFramebuffer(Framebuffer framebuffer);
}
