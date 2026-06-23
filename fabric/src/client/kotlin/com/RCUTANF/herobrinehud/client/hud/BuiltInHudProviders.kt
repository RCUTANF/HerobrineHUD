package com.RCUTANF.herobrinehud.client.hud

import com.RCUTANF.herobrinehud.client.HudRenderCompat
import com.RCUTANF.herobrinehud.client.ui.HudRenderer

object BuiltInHudProviders {
    fun registerAll() {
        HudCenter.register(ClassicHudProvider)
        HudCenter.register(NoneHudProvider)
        HudCenter.ensureValidSelection()
        HudCenter.current()?.onEnable()
    }
}

object ClassicHudProvider : HudProvider {
    override val id: String = HudCenter.DEFAULT_HUD_ID
    override val displayName: String = "Classic"
    override val description: String = "Default HerobrineHUD player card overlay."

    private var renderHookInstalled = false

    override fun onEnable() {
        if (renderHookInstalled) return
        HudRenderCompat.register(HudRenderer)
        renderHookInstalled = true
    }
}

object NoneHudProvider : HudProvider {
    override val id: String = "herobrinehud:none"
    override val displayName: String = "None"
    override val description: String = "Disables HerobrineHUD-managed HUD selection."
}
