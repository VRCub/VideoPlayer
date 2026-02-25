package com.github.squi2rel.vp;

public final class Android {

    public static void load() {
        try {
            System.loadLibrary("vlc_jvm_bridge");
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Cannot load Android Library! See https://github.com/squi2rel/VideoPlayer-Library for more info", e);
        }
        init();
    }

    private static native void init();
}
