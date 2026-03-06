package com.RCUTANF.herobrinehud.client

import com.RCUTANF.herobrinehud.network.SpectatePlayerPayload
import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory
import java.util.UUID

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
     * 小键盘数字键 1-9, 0 对应的快捷键绑定
     * 编号顺序与 HUD 卡片编号一致：
     *   1~5 → 左侧第 1~5 个玩家
     *   6~9 → 右侧第 1~4 个玩家
     *   0   → 右侧第 5 个玩家
     */
    private val NUMPAD_KEYS: Map<Int, KeyMapping> = run {
        // 数字 1~9 对应 KP_1~KP_9，0 对应 KP_0
        val kpKeyCodes = mapOf(
            1 to InputConstants.KEY_NUMPAD1,
            2 to InputConstants.KEY_NUMPAD2,
            3 to InputConstants.KEY_NUMPAD3,
            4 to InputConstants.KEY_NUMPAD4,
            5 to InputConstants.KEY_NUMPAD5,
            6 to InputConstants.KEY_NUMPAD6,
            7 to InputConstants.KEY_NUMPAD7,
            8 to InputConstants.KEY_NUMPAD8,
            9 to InputConstants.KEY_NUMPAD9,
            0 to InputConstants.KEY_NUMPAD0,
        )
        kpKeyCodes.mapValues { (number, keyCode) ->
            KeyBindingHelper.registerKeyBinding(
                KeyMapping(
                    "key.herobrinehud.player_$number",
                    InputConstants.Type.KEYSYM,
                    keyCode,
                    CATEGORY
                )
            )
        }
    }

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

            // 轮询小键盘数字键
            for ((number, binding) in NUMPAD_KEYS) {
                while (binding.consumeClick()) {
                    val player = HudSelectionState.getPlayerByHotkeyNumber(number)
                    if (player != null) {
                        ClientPlayNetworking.send(SpectatePlayerPayload(UUID.fromString(player.uuid)))
                    }
                }
            }
        }
    }
}
