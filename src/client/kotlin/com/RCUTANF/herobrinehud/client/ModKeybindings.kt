package com.RCUTANF.herobrinehud.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

/**
 * 快捷键注册与处理
 */
object ModKeybindings {

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/Keybindings")

    private val CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("herobrinehud", "key_category"))

    /** 切换 HUD 显示/隐藏 */
    private val TOGGLE_HUD = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.herobrinehud.toggle_hud",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            CATEGORY
        )
    )

    /** 打开队伍选择界面 */
    private val OPEN_SELECTION = KeyBindingHelper.registerKeyBinding(
        KeyMapping(
            "key.herobrinehud.open_selection",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            CATEGORY
        )
    )

    /**
     * 注册 Tick 事件监听器以轮询按键状态
     */
    fun register() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            while (TOGGLE_HUD.consumeClick()) {
                HudSelectionState.toggleHudVisibility()
                LOGGER.info("HUD 可见性切换为: {}", HudSelectionState.isHudVisible())
            }

            while (OPEN_SELECTION.consumeClick()) {
                client.setScreen(TeamSelectionScreen(null))
            }
        }
    }
}

