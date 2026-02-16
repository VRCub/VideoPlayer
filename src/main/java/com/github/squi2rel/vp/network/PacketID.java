package com.github.squi2rel.vp.network;

import com.github.squi2rel.vp.video.VideoScreen;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class PacketID {
    public static final int
    CONFIG = 0,
    REQUEST = 1,
    SYNC = 2,
    CREATE_AREA = 3,
    REMOVE_AREA = 4,
    CREATE_SCREEN = 5,
    REMOVE_SCREEN = 6,
    LOAD_AREA = 7,
    UNLOAD_AREA = 8,
    UPDATE_PLAYLIST = 9,
    SKIP = 10,
    SKIP_PERCENT = 11,
    EXECUTE = 12,
    IDLE_PLAY = 13,
    SET_UV = 14,
    OPEN_MENU = 15,
    SET_META = 16,
    SET_CUSTOM_META = 17,
    SET_SCALE = 18,
    AUTO_SYNC = 19;

    public enum Action {
        MUTE("静音", i -> (i >>> 1) == 0, (v, i) -> v.meta.put("mute", i)),
        INTERACTABLE("可交互", i -> (i >>> 1) == 0, (v, i) -> v.meta.put("interactable", i)),
        ASPECT("宽高比", i -> Float.intBitsToFloat(i) >= 0.0625f && Float.intBitsToFloat(i) <= 16f, (v, i) -> v.meta.put("aspect", i)),
        FOV("视场角", i -> i > 0 && i < 180, (v, i) -> v.meta.put("fov", i)),
        AUTO_SYNC("自动同步", i -> (i >>> 1) == 0, (v, i) -> v.meta.put("autoSync", i));

        public static final Action[] VALUES = values();

        private final Function<Integer, Boolean> verifier;
        private final BiConsumer<VideoScreen, Integer> action;
        public final String name;

        Action(String name, Function<Integer, Boolean> verifier, BiConsumer<VideoScreen, Integer> action) {
            this.name = name;
            this.verifier = verifier;
            this.action = action;
        }

        public boolean verify(int i) {
            return verifier.apply(i);
        }

        public void apply(VideoScreen screen, int i) {
            action.accept(screen, i);
        }
    }
}
