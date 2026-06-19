package com.RCUTANF.herobrinehud.client.hud

/**
 * A lightweight registration record for a selectable HUD.
 *
 * Rendering is intentionally not part of this API. Each HUD backend owns its
 * own render lifecycle; the center only tracks availability and selection.
 */
interface HudProvider {
    val id: String
    val displayName: String
    val description: String
        get() = ""

    fun isAvailable(): Boolean = true

    fun onEnable() {
    }

    fun onDisable() {
    }
}
