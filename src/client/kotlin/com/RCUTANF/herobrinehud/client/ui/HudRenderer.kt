package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.ClientTeamData
import com.RCUTANF.herobrinehud.client.DisplaySide
import com.RCUTANF.herobrinehud.client.HudConfig
import com.RCUTANF.herobrinehud.data.PlayerInfo
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/**
 * HUD 渲染器
 *
 * 在游戏画面上绘制玩家信息卡片。
 * 玩家按 uuid 独立分配到 LEFT / RIGHT 侧，与队伍无关。
 * 布局：左下角从左往右排列，右下角从左往右排列但最右边贴屏幕右下角
 */
object HudRenderer : HudRenderCallback {

    private val L = CardLayout

    override fun onHudRender(drawContext: GuiGraphics, tickCounter: DeltaTracker) {
        if (!HudSelectionState.isHudVisible()) return
        if (!HudSelectionState.hasValidSelection()) return
        if (!ClientTeamData.isSynced) return

        val client = Minecraft.getInstance()
        val screenWidth = client.window.guiScaledWidth
        val screenHeight = client.window.guiScaledHeight
        val opacity = (HudConfig.data.hudOpacity * 255).toInt().coerceIn(0, 255)

        // ──────────── 渲染左下角玩家（从左往右） ────────────
        val leftPlayers = HudSelectionState.getPlayersBySide(DisplaySide.LEFT)
        var leftX = L.MARGIN
        val leftY = screenHeight - L.CARD_HEIGHT - L.MARGIN
        for (player in leftPlayers) {
            val (teamName, teamColor) = findTeamInfo(player)
            PlayerCardRenderer.renderCard(drawContext, player, leftX, leftY, teamName, teamColor, opacity)
            leftX += L.CARD_WIDTH + L.CARD_GAP
        }

        // ──────────── 渲染右下角玩家（从左往右，但最右边贴右下角） ────────────
        val rightPlayers = HudSelectionState.getPlayersBySide(DisplaySide.RIGHT)
        val rightY = screenHeight - L.CARD_HEIGHT - L.MARGIN
        // 计算右侧卡片组的总宽度
        val rightTotalWidth = rightPlayers.size * L.CARD_WIDTH + (rightPlayers.size - 1) * L.CARD_GAP
        // 从右边开始计算起始X位置
        var rightX = screenWidth - rightTotalWidth - L.MARGIN
        for (player in rightPlayers) {
            val (teamName, teamColor) = findTeamInfo(player)
            PlayerCardRenderer.renderCard(
                drawContext, player,
                rightX, rightY,
                teamName, teamColor, opacity
            )
            rightX += L.CARD_WIDTH + L.CARD_GAP
        }
    }

    /** 通过 ClientTeamData 反查玩家所属队伍的名称和颜色 */
    private fun findTeamInfo(player: PlayerInfo): Pair<String, String> {
        for (team in ClientTeamData.getAllTeams().values) {
            val match = team.players.any {
                (it.uuid.isNotEmpty() && it.uuid == player.uuid) || it.name == player.name
            }
            if (match) {
                return team.displayName to team.color
            }
        }
        return "" to "#FFFFFF"
    }
}
