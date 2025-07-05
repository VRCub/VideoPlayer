package com.github.squi2rel.vp.vivecraft;

import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.render.RenderPass;

class VivecraftImpl {
    private static final ClientDataHolderVR DATA_HOLDER = ClientDataHolderVR.getInstance();

    static boolean isRightEye() {
        return DATA_HOLDER.currentPass == RenderPass.RIGHT;
    }
}
