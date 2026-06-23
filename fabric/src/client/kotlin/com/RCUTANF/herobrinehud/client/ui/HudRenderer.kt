package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.HudRenderable
import com.RCUTANF.herobrinehud.client.api.HerobrineHudDataApi
import com.RCUTANF.herobrinehud.client.hud.HudCenter
import com.RCUTANF.herobrinehud.data.PlayerInfo
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * HUD 渲染器
 *
 * 在游戏画面上绘制玩家信息卡片。
 * 玩家按 uuid 独立分配到 LEFT / RIGHT 侧，与队伍无关。
 * 布局：左下角从左往右排列，右下角从左往右排列但最右边贴屏幕右下角
 */
object HudRenderer : HudRenderable {

    private val L = CardLayout

    override fun renderHud(drawContext: GuiGraphicsExtractor, tickCounter: DeltaTracker) {
        val data = HerobrineHudDataApi

        if (!HudCenter.isCurrent(HudCenter.DEFAULT_HUD_ID)) return
        if (!data.settings.hudVisible) return
        if (!data.isSynced) return

        val leftPlayers = data.leftPlayers()
        val rightPlayers = data.rightPlayers()
        if (leftPlayers.isEmpty() && rightPlayers.isEmpty()) return

        val client = Minecraft.getInstance()
        val screenWidth = client.window.guiScaledWidth
        val screenHeight = client.window.guiScaledHeight
        val opacity = (data.settings.hudOpacity * 255).toInt().coerceIn(0, 255)

        // ──────────── 渲染左下角玩家（从左往右） ────────────
        var leftX = L.MARGIN
        val leftY = screenHeight - L.CARD_HEIGHT - L.MARGIN
        for (player in leftPlayers) {
            val (teamName, teamColor) = findTeamInfo(player)
            val hotkeyNumber = data.hotkey(player.uuid)
            PlayerCardRenderer.renderCard(drawContext, player, leftX, leftY, teamName, teamColor, opacity, hotkeyNumber)
            leftX += L.CARD_WIDTH + L.CARD_GAP
        }

        // ──────────── 渲染右下角玩家（从左往右，但最右边贴右下角） ────────────
        val rightY = screenHeight - L.CARD_HEIGHT - L.MARGIN
        // 计算右侧卡片组的总宽度
        val rightTotalWidth = rightPlayers.size * L.CARD_WIDTH + (rightPlayers.size - 1) * L.CARD_GAP
        // 从右边开始计算起始X位置
        var rightX = screenWidth - rightTotalWidth - L.MARGIN
        for (player in rightPlayers) {
            val (teamName, teamColor) = findTeamInfo(player)
            val hotkeyNumber = data.hotkey(player.uuid)
            PlayerCardRenderer.renderCard(
                drawContext, player,
                rightX, rightY,
                teamName, teamColor, opacity, hotkeyNumber
            )
            rightX += L.CARD_WIDTH + L.CARD_GAP
        }
    }

    /** 通过 Data API 反查玩家所属队伍的名称和颜色 */
    private fun findTeamInfo(player: PlayerInfo): Pair<String, String> {
        val team = HerobrineHudDataApi.teamOf(player)
        return team?.let { it.displayName to it.color } ?: ("" to "#FFFFFF")
    }
}
