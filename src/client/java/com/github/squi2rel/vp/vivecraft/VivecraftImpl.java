package com.github.squi2rel.vp.vivecraft;

import org.joml.Matrix4f;
import org.vivecraft.api.client.VRRenderingAPI;
import org.vivecraft.api.client.data.RenderPass;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRState;

class VivecraftImpl {
    private static final ClientDataHolderVR DATA_HOLDER = ClientDataHolderVR.getInstance();
    private static final VRRenderingAPI RAPI = VRRenderingAPI.instance();

    static boolean isRightEye() {
        return RAPI.getCurrentRenderPass() == RenderPass.RIGHT;
    }

    static boolean isVRActive() {
        return VRState.VR_RUNNING;
    }

    static Matrix4f getRotation() {
        return DATA_HOLDER.vrPlayer.getVRDataWorld().getEye(RAPI.getCurrentRenderPass()).getMatrix();
    }
}
