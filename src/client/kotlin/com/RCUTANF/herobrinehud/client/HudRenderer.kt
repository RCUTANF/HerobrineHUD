package com.RCUTANF.herobrinehud.client

import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.RCUTANF.herobrinehud.data.TeamInfo
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics

/**
 * HUD 渲染器
 *
 * 在游戏画面上绘制卡片式队伍与玩家信息叠加层。
 * 通过 HudRenderCallback 注册，每帧调用。
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

        // ──────────── 渲染左侧队伍 ────────────
        val leftTeams = HudSelectionState.getTeamsBySide(DisplaySide.LEFT)
        var leftY = L.MARGIN
        for (team in leftTeams) {
            val visiblePlayers = team.players.filter { !HudSelectionState.isPlayerHidden(it.uuid) }
            leftY = renderTeamPanel(
                drawContext, team, visiblePlayers,
                x = L.MARGIN, y = leftY, opacity = opacity
            )
            leftY += L.TEAM_GAP
        }

        // ──────────── 渲染右侧队伍 ────────────
        val rightTeams = HudSelectionState.getTeamsBySide(DisplaySide.RIGHT)
        var rightY = L.MARGIN
        for (team in rightTeams) {
            val visiblePlayers = team.players.filter { !HudSelectionState.isPlayerHidden(it.uuid) }
            rightY = renderTeamPanel(
                drawContext, team, visiblePlayers,
                x = screenWidth - L.CARD_WIDTH - L.MARGIN, y = rightY, opacity = opacity
            )
            rightY += L.TEAM_GAP
        }
    }

    /**
     * 渲染单个队伍面板（标题 + 玩家卡片列表），返回面板底部 Y 坐标
     */
    private fun renderTeamPanel(
        ctx: GuiGraphics,
        team: TeamInfo,
        players: List<PlayerInfo>,
        x: Int, y: Int,
        opacity: Int
    ): Int {
        val font = Minecraft.getInstance().font
        val totalHeight = L.TEAM_HEADER_HEIGHT +
                players.size * (L.CARD_HEIGHT + L.CARD_GAP) +
                L.PANEL_PADDING * 2

        // 面板背景
        ctx.fill(x, y, x + L.CARD_WIDTH, y + totalHeight, L.bgColor(opacity))

        // 队伍标题
        val teamColor = parseColor(team.color, opacity)
        ctx.drawString(font, team.displayName, x + L.PANEL_PADDING, y + L.PANEL_PADDING, teamColor, true)

        // 玩家卡片列表
        var cardY = y + L.PANEL_PADDING + L.TEAM_HEADER_HEIGHT
        for (player in players) {
            PlayerCardRenderer.renderCard(ctx, player, x, cardY, opacity)
            cardY += L.CARD_HEIGHT + L.CARD_GAP
        }

        return y + totalHeight
    }

    /**
     * 解析 HEX 颜色字符串为 ARGB int
     */
    private fun parseColor(hex: String, alpha: Int): Int {
        return try {
            val rgb = hex.removePrefix("#").toInt(16)
            (alpha shl 24) or rgb
        } catch (_: Exception) {
            (alpha shl 24) or 0xFFFFFF
        }
    }
}
