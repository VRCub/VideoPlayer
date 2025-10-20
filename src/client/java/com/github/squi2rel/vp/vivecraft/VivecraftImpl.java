package com.github.squi2rel.vp.vivecraft;

import com.github.squi2rel.vp.VideoPlayerClient;
import org.joml.Matrix4f;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRState;

class VivecraftImpl {
    private static final ClientDataHolderVR DATA_HOLDER = ClientDataHolderVR.getInstance();
    private static Object renderPassObj;

    static boolean isRightEye() {
        if (renderPassObj == null) {
            renderPassObj = VideoPlayerClient.getEnumValue("RIGHT",
                    "org.vivecraft.api.client.data.RenderPass",
                    "org.vivecraft.client_vr.render.RenderPass");
        }
        return DATA_HOLDER.currentPass == renderPassObj;
    }

    static boolean isVRActive() {
        return VRState.VR_RUNNING;
    }

    static Matrix4f getRotation() {
        return DATA_HOLDER.vrPlayer.getVRDataWorld().getEye(DATA_HOLDER.currentPass).getMatrix();
    }
}
