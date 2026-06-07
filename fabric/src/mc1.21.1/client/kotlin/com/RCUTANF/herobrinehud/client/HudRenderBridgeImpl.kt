package com.RCUTANF.herobrinehud.client

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback

internal class HudRenderBridgeImpl : HudRenderBridge {
    override fun register(renderer: HudRenderable) {
        HudRenderCallback.EVENT.register { drawContext, tickCounter ->
            renderer.renderHud(drawContext, tickCounter)
        }
    }
}
