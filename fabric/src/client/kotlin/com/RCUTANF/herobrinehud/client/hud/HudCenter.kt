package com.RCUTANF.herobrinehud.client.hud

import com.RCUTANF.herobrinehud.client.HudConfig
import org.slf4j.LoggerFactory

object HudCenter {
    const val DEFAULT_HUD_ID = "herobrinehud:classic"

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/HudCenter")
    private val providers = linkedMapOf<String, HudProvider>()

    fun register(provider: HudProvider) {
        val id = provider.id.trim()
        require(id.isNotEmpty()) { "HUD provider id cannot be blank" }
        require(id.contains(":")) { "HUD provider id must be namespaced: $id" }
        require(id !in providers) { "Duplicate HUD provider id: $id" }
        providers[id] = provider
        LOGGER.info("Registered HUD provider: {} ({})", id, provider.displayName)
    }

    fun all(): List<HudProvider> = providers.values.toList()

    fun available(): List<HudProvider> = providers.values.filter { it.isAvailable() }

    fun get(id: String): HudProvider? = providers[id]

    fun current(): HudProvider? {
        val configured = get(HudConfig.data.hudProviderId)?.takeIf { it.isAvailable() }
        if (configured != null) return configured
        return get(DEFAULT_HUD_ID)?.takeIf { it.isAvailable() } ?: available().firstOrNull()
    }

    fun isCurrent(id: String): Boolean = current()?.id == id

    fun select(id: String): Boolean {
        val next = get(id)?.takeIf { it.isAvailable() } ?: return false
        val previous = current()
        if (previous?.id == next.id && HudConfig.data.hudProviderId == next.id) return true

        previous?.onDisable()
        HudConfig.update { hudProviderId = next.id }
        next.onEnable()
        LOGGER.info("Selected HUD provider: {} ({})", next.id, next.displayName)
        return true
    }

    fun selectNext(): HudProvider? {
        val list = available()
        if (list.isEmpty()) return null
        val currentId = current()?.id
        val currentIndex = list.indexOfFirst { it.id == currentId }
        val next = list[(currentIndex + 1).floorMod(list.size)]
        select(next.id)
        return next
    }

    fun ensureValidSelection() {
        val current = current()
        if (current == null) {
            LOGGER.warn("No available HUD providers are registered")
            return
        }
        if (HudConfig.data.hudProviderId != current.id) {
            HudConfig.update { hudProviderId = current.id }
        }
    }

    private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
}
