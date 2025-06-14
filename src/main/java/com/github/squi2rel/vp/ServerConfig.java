package com.github.squi2rel.vp;

import com.github.squi2rel.vp.video.VideoArea;

import java.util.ArrayList;

public class ServerConfig {
    public ArrayList<VideoArea> areas = new ArrayList<>();
    public String remoteControlName = "minecraft:iron_ingot";
    public float remoteControlId = -1;
    public float remoteControlRange = 64;
    public float noControlRange = 16;
}
