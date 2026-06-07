package com.RCUTANF.herobrinehud.client

import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor

internal interface HudRenderable {
    fun renderHud(drawContext: GuiGraphicsExtractor, tickCounter: DeltaTracker)
}

internal interface HudRenderBridge {
    fun register(renderer: HudRenderable)
}

internal object HudRenderCompat : HudRenderBridge by HudRenderBridgeImpl()
