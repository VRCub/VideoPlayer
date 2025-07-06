package com.github.squi2rel.vp.vivecraft;

import net.fabricmc.loader.api.FabricLoader;
import org.joml.Matrix4f;

public class Vivecraft {
    public static final boolean loaded = FabricLoader.getInstance().isModLoaded("vivecraft");

    public static boolean isRightEye() {
        return VivecraftImpl.isRightEye();
    }

    public static boolean isVRActive() {
        return VivecraftImpl.isVRActive();
    }

    public static Matrix4f getRotation() {
        return VivecraftImpl.getRotation();
    }
}
