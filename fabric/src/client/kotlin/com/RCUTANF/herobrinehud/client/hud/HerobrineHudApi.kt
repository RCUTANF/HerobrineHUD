package com.RCUTANF.herobrinehud.client.hud

import com.RCUTANF.herobrinehud.client.api.HerobrineHudDataApi

object HerobrineHudApi {
    fun registerHud(provider: HudProvider) {
        HudCenter.register(provider)
    }

    fun hudProviders(): List<HudProvider> = HudCenter.all()

    fun currentHud(): HudProvider? = HudCenter.current()

    fun data(): HerobrineHudDataApi = HerobrineHudDataApi
}
