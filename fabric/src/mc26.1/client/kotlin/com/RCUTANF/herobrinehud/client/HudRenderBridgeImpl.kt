package com.RCUTANF.herobrinehud.client

import com.RCUTANF.herobrinehud.Herobrinehud
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.resources.Identifier

internal class HudRenderBridgeImpl : HudRenderBridge {
    override fun register(renderer: HudRenderable) {
        val hudId = Identifier.fromNamespaceAndPath(Herobrinehud.MOD_ID, "player_cards")
        val element = HudElement { drawContext, tickCounter ->
            renderer.renderHud(drawContext, tickCounter)
        }
        HudElementRegistry.attachElementBefore(VanillaHudElements.HOTBAR, hudId, element)
    }
}
