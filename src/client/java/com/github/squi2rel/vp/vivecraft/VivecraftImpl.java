package com.github.squi2rel.vp.vivecraft;

import org.joml.Matrix4f;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRState;
import org.vivecraft.client_vr.render.RenderPass;

class VivecraftImpl {
    private static final ClientDataHolderVR DATA_HOLDER = ClientDataHolderVR.getInstance();

    static boolean isRightEye() {
        return DATA_HOLDER.currentPass == RenderPass.RIGHT;
    }

    static boolean isVRActive() {
        return VRState.VR_RUNNING;
    }

    static Matrix4f getRotation() {
        return DATA_HOLDER.vrPlayer.getVRDataWorld().getEye(DATA_HOLDER.currentPass).getMatrix();
    }
}
