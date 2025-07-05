package com.github.squi2rel.vp.vivecraft;

import net.fabricmc.loader.api.FabricLoader;

public class Vivecraft {
    private static final boolean loaded = FabricLoader.getInstance().isModLoaded("vivecraft");

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean isRightEye() {
        return VivecraftImpl.isRightEye();
    }
}
