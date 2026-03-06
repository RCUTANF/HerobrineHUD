package com.RCUTANF.herobrinehud.client

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
 */
object HudRenderer : HudRenderCallback {

    private val L = CardLayout

    override fun onHudRender(drawContext: GuiGraphics, tickCounter: DeltaTracker) {
        if (!HudSelectionState.isHudVisible()) return
        if (!HudSelectionState.hasValidSelection()) return
        if (!ClientTeamData.isSynced) return

        val client = Minecraft.getInstance()
        val screenWidth = client.window.guiScaledWidth
        val opacity = (HudConfig.data.hudOpacity * 255).toInt().coerceIn(0, 255)

        // ──────────── 渲染左侧玩家 ────────────
        val leftPlayers = HudSelectionState.getPlayersBySide(DisplaySide.LEFT)
        var leftY = L.MARGIN
        for ((index, player) in leftPlayers.withIndex()) {
            val (teamName, teamColor) = findTeamInfo(player)
            // 左侧编号：1, 2, 3, 4, 5
            val hotkeyNumber = if (index < 5) index + 1 else -1
            PlayerCardRenderer.renderCard(drawContext, player, L.MARGIN, leftY, teamName, teamColor, opacity, hotkeyNumber)
            leftY += L.CARD_HEIGHT + L.CARD_GAP
        }

        // ──────────── 渲染右侧玩家 ────────────
        val rightPlayers = HudSelectionState.getPlayersBySide(DisplaySide.RIGHT)
        var rightY = L.MARGIN
        // 右侧编号序列：6, 7, 8, 9, 0
        val rightHotkeyNumbers = listOf(6, 7, 8, 9, 0)
        for ((index, player) in rightPlayers.withIndex()) {
            val (teamName, teamColor) = findTeamInfo(player)
            val hotkeyNumber = if (index < rightHotkeyNumbers.size) rightHotkeyNumbers[index] else -1
            PlayerCardRenderer.renderCard(
                drawContext, player,
                screenWidth - L.CARD_WIDTH - L.MARGIN, rightY,
                teamName, teamColor, opacity, hotkeyNumber
            )
            rightY += L.CARD_HEIGHT + L.CARD_GAP
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
