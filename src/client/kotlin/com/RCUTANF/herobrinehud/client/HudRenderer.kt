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
 * 在游戏画面上绘制队伍与玩家信息叠加层。
 * 通过 HudRenderCallback 注册，每帧调用。
 */
object HudRenderer : HudRenderCallback {

    // 布局常量
    private const val MARGIN = 4
    private const val TEAM_HEADER_HEIGHT = 12
    private const val PLAYER_ROW_HEIGHT = 12
    private const val PANEL_WIDTH = 120
    private const val HEALTH_BAR_WIDTH = 50
    private const val HEALTH_BAR_HEIGHT = 6
    private const val PANEL_PADDING = 3
    private const val TEAM_GAP = 6

    override fun onHudRender(drawContext: GuiGraphics, tickCounter: DeltaTracker) {
        if (!HudSelectionState.isHudVisible()) return
        if (!HudSelectionState.hasValidSelection()) return
        if (!ClientTeamData.isSynced) return

        val client = Minecraft.getInstance()
        val font = client.font
        val screenWidth = client.window.guiScaledWidth
        val screenHeight = client.window.guiScaledHeight
        val opacity = (HudConfig.data.hudOpacity * 255).toInt().coerceIn(0, 255)

        // ──────────── 渲染左侧队伍 ────────────
        val leftTeams = HudSelectionState.getTeamsBySide(DisplaySide.LEFT)
        var leftY = MARGIN
        for (team in leftTeams) {
            val visiblePlayers = team.players.filter { !HudSelectionState.isPlayerHidden(it.uuid) }
            leftY = renderTeamPanel(
                drawContext, font, team, visiblePlayers,
                x = MARGIN, y = leftY, alignRight = false, opacity = opacity
            )
            leftY += TEAM_GAP
        }

        // ──────────── 渲染右侧队伍 ────────────
        val rightTeams = HudSelectionState.getTeamsBySide(DisplaySide.RIGHT)
        var rightY = MARGIN
        for (team in rightTeams) {
            val visiblePlayers = team.players.filter { !HudSelectionState.isPlayerHidden(it.uuid) }
            rightY = renderTeamPanel(
                drawContext, font, team, visiblePlayers,
                x = screenWidth - PANEL_WIDTH - MARGIN, y = rightY, alignRight = true, opacity = opacity
            )
            rightY += TEAM_GAP
        }
    }

    /**
     * 渲染单个队伍面板，返回面板底部 Y 坐标
     */
    private fun renderTeamPanel(
        ctx: GuiGraphics,
        font: net.minecraft.client.gui.Font,
        team: TeamInfo,
        players: List<PlayerInfo>,
        x: Int, y: Int,
        alignRight: Boolean,
        opacity: Int
    ): Int {
        val totalHeight = TEAM_HEADER_HEIGHT + players.size * PLAYER_ROW_HEIGHT + PANEL_PADDING * 2
        val bgColor = (opacity / 2 shl 24) // 半透明黑色背景

        // 背景
        ctx.fill(x, y, x + PANEL_WIDTH, y + totalHeight, bgColor)

        // 队伍标题
        val teamColor = parseColor(team.color, opacity)
        ctx.drawString(font, team.displayName, x + PANEL_PADDING, y + PANEL_PADDING, teamColor, true)

        // 玩家列表
        var rowY = y + PANEL_PADDING + TEAM_HEADER_HEIGHT
        for (player in players) {
            renderPlayerRow(ctx, font, player, x + PANEL_PADDING, rowY, opacity)
            rowY += PLAYER_ROW_HEIGHT
        }

        return y + totalHeight
    }

    /**
     * 渲染单行玩家信息
     */
    private fun renderPlayerRow(
        ctx: GuiGraphics,
        font: net.minecraft.client.gui.Font,
        player: PlayerInfo,
        x: Int, y: Int,
        opacity: Int
    ) {
        val nameColor = if (player.isAlive) (opacity shl 24 or 0xFFFFFF) else (opacity shl 24 or 0x888888)

        // 玩家名称（截断显示）
        val nameWidth = PANEL_WIDTH - PANEL_PADDING * 2 - HEALTH_BAR_WIDTH - 4
        val displayName = if (font.width(player.name) > nameWidth) {
            player.name.take(8) + "…"
        } else {
            player.name
        }
        ctx.drawString(font, displayName, x, y, nameColor, true)

        // 生命值条
        val barX = x + nameWidth + 4
        val healthPercent = if (player.maxHealth > 0) (player.health / player.maxHealth).coerceIn(0.0, 1.0) else 0.0
        val barBg = (opacity / 2 shl 24) or 0x333333
        val barFg = (opacity shl 24) or getHealthColor(healthPercent)

        ctx.fill(barX, y + 1, barX + HEALTH_BAR_WIDTH, y + 1 + HEALTH_BAR_HEIGHT, barBg)
        ctx.fill(barX, y + 1, barX + (HEALTH_BAR_WIDTH * healthPercent).toInt(), y + 1 + HEALTH_BAR_HEIGHT, barFg)

        // 生命值数字
        if (HudConfig.data.showHealthNumber) {
            val healthText = "${player.health.toInt()}/${player.maxHealth.toInt()}"
            val textWidth = font.width(healthText)
            val textX = barX + (HEALTH_BAR_WIDTH - textWidth) / 2
            ctx.drawString(font, healthText, textX, y, (opacity shl 24) or 0xFFFFFF, true)
        }
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

    /**
     * 根据生命值百分比返回颜色 (RGB)
     */
    private fun getHealthColor(percent: Double): Int {
        return when {
            percent > 0.6 -> 0x55FF55   // 绿色
            percent > 0.3 -> 0xFFFF55   // 黄色
            percent > 0.0 -> 0xFF5555   // 红色
            else -> 0x555555            // 灰色（已死亡）
        }
    }
}

