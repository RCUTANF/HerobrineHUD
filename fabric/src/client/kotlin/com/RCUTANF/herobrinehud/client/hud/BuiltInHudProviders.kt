package com.RCUTANF.herobrinehud.client.hud

object BuiltInHudProviders {
    fun registerAll() {
        HudCenter.register(ClassicHudProvider)
        HudCenter.register(NoneHudProvider)
        HudCenter.ensureValidSelection()
    }
}

object ClassicHudProvider : HudProvider {
    override val id: String = HudCenter.DEFAULT_HUD_ID
    override val displayName: String = "Classic"
    override val description: String = "Default HerobrineHUD player card overlay."
}

object NoneHudProvider : HudProvider {
    override val id: String = "herobrinehud:none"
    override val displayName: String = "None"
    override val description: String = "Disables HerobrineHUD-managed HUD selection."
}
