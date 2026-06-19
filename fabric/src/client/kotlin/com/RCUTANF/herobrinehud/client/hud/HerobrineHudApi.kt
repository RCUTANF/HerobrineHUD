package com.RCUTANF.herobrinehud.client.hud

object HerobrineHudApi {
    fun registerHud(provider: HudProvider) {
        HudCenter.register(provider)
    }

    fun hudProviders(): List<HudProvider> = HudCenter.all()

    fun currentHud(): HudProvider? = HudCenter.current()
}
