package com.github.squi2rel.vp.network;

import com.github.squi2rel.vp.video.VideoScreen;

import java.util.function.BiConsumer;

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
    SLICE = 14,
    OPEN_MENU = 15,
    SET_META = 16;

    public enum Action {
        SET_MUTE((v, i) -> v.muted = i != 0),
        SET_INTERACTABLE((v, i) -> v.interactable = i != 0);

        public static final Action[] VALUES = values();

        private final BiConsumer<VideoScreen, Integer> action;

        Action(BiConsumer<VideoScreen, Integer> action) {
            this.action = action;
        }

        public void apply(VideoScreen screen, Integer i) {
            action.accept(screen, i);
        }
    }
}
