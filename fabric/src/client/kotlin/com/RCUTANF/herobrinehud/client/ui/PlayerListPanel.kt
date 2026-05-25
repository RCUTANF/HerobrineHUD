// PlayerListPanel.kt
package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.ClientTeamData
import com.RCUTANF.herobrinehud.client.DisplaySide
import com.RCUTANF.herobrinehud.data.PlayerInfo
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.ObjectSelectionList
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * PlayerListPanel
 *
 * 可滚动的玩家卡槽列表，用于"左侧/右侧/未上屏"三栏中的每一栏。
 */
class PlayerListPanel(
    mc: Minecraft,
    panelWidth: Int,
    panelHeight: Int,
    panelY: Int,
    val side: DisplaySide,
    private val onMove: (uuid: String, targetSide: DisplaySide) -> Unit
) : ObjectSelectionList<PlayerListPanel.PlayerEntry>(
    mc, panelWidth, panelHeight, panelY, ENTRY_HEIGHT
) {

    companion object {
        const val ENTRY_HEIGHT = 24
        const val BTN_W = 18
    }

    /** 用给定的玩家列表刷新条目 */
    fun setPlayers(players: List<PlayerInfo>) {
        clearEntries()
        for (p in players) addEntry(PlayerEntry(p))
    }

    override fun getRowWidth(): Int = width - 12

    // ──────────────────────────────────────────────────────────
    //  Entry
    // ──────────────────────────────────────────────────────────

    inner class PlayerEntry(val player: PlayerInfo) : Entry<PlayerEntry>() {

        override fun extractContent(
            guiGraphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            isHovering: Boolean,
            partialTick: Float
        ) {
            val font = minecraft.font

            // Entry 的位置和尺寸由父类管理
            val left = this.x
            val top = this.y
            val width = this.width
            val height = this.height

            // 行背景（悬停时加亮）
            if (isHovering) {
                guiGraphics.fill(left, top, left + width, top + height, 0x44FFFFFF)
            }

            // 找到队伍颜色
            val teamColor = findTeamColor(player)
            // 左侧色条
            guiGraphics.fill(left + 2, top + 2, left + 5, top + height - 2, teamColor or (0xFF shl 24))

            // 玩家显示名
            val displayName = player.displayName.ifBlank { player.name }
            guiGraphics.text(font, displayName, left + 9, top + 3, 0xFFFFFFFF.toInt(), false)

            // 队伍标签（小字）
            val teamLabel = findTeamDisplayName(player)
            if (teamLabel.isNotEmpty()) {
                guiGraphics.text(font, teamLabel, left + 9, top + 13, 0xFFAAAAAA.toInt(), false)
            }

            // 绘制三个移动按钮
            val btnAreaRight = left + width - 4
            val by = top + (height - 14) / 2

            // 按钮坐标
            val noneX  = btnAreaRight - BTN_W
            val rightX = btnAreaRight - BTN_W * 2 - 2
            val leftX  = btnAreaRight - BTN_W * 3 - 4

            // 绘制按钮背景和文字
            drawButton(guiGraphics, "◀", leftX,  by, side != DisplaySide.LEFT,  mouseX, mouseY)
            drawButton(guiGraphics, "▶", rightX, by, side != DisplaySide.RIGHT, mouseX, mouseY)
            drawButton(guiGraphics, "✕", noneX,  by, side != DisplaySide.NONE,  mouseX, mouseY)
        }

        private fun drawButton(g: GuiGraphicsExtractor, label: String, x: Int, y: Int, enabled: Boolean, mx: Int, my: Int) {
            val hovered = enabled && mx >= x && mx < x + BTN_W && my >= y && my < y + 14
            val bgColor = when {
                !enabled -> 0x33333333
                hovered  -> 0xAA555555.toInt()
                else     -> 0x66333333
            }
            g.fill(x, y, x + BTN_W, y + 14, bgColor)
            val textColor = if (enabled) 0xFFFFFFFF.toInt() else 0xFF666666.toInt()
            val font = minecraft.font
            val tw = font.width(label)
            g.text(font, label, x + (BTN_W - tw) / 2, y + 3, textColor, false)
        }

        override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
            val mouseX = event.x.toInt()
            val mouseY = event.y.toInt()

            // Entry 的位置和尺寸由父类管理
            val left = this.x
            val top = this.y
            val width = this.width
            val height = this.height

            val btnAreaRight = left + width - 4
            val by = top + (height - 14) / 2

            val noneX  = btnAreaRight - BTN_W
            val rightX = btnAreaRight - BTN_W * 2 - 2
            val leftX  = btnAreaRight - BTN_W * 3 - 4

            // 检查点击了哪个按钮
            if (mouseY >= by && mouseY < by + 14) {
                when {
                    mouseX >= leftX && mouseX < leftX + BTN_W && side != DisplaySide.LEFT -> {
                        onMove(player.uuid, DisplaySide.LEFT)
                        return true
                    }
                    mouseX >= rightX && mouseX < rightX + BTN_W && side != DisplaySide.RIGHT -> {
                        onMove(player.uuid, DisplaySide.RIGHT)
                        return true
                    }
                    mouseX >= noneX && mouseX < noneX + BTN_W && side != DisplaySide.NONE -> {
                        onMove(player.uuid, DisplaySide.NONE)
                        return true
                    }
                }
            }
            return false
        }

        override fun getNarration(): Component =
            Component.literal(player.displayName.ifBlank { player.name })
    }

    // ──────────────────────────────────────────────────────────
    //  团队信息反查
    // ──────────────────────────────────────────────────────────

    private fun findTeamColor(player: PlayerInfo): Int {
        for (team in ClientTeamData.getAllTeams().values) {
            val match = team.players.any {
                (it.uuid.isNotEmpty() && it.uuid == player.uuid) || it.name == player.name
            }
            if (match) {
                return try {
                    val hex = team.color.trimStart('#')
                    hex.toLong(16).toInt()
                } catch (_: Exception) { 0xFFFFFF }
            }
        }
        return 0xFFFFFF
    }

    private fun findTeamDisplayName(player: PlayerInfo): String {
        for (team in ClientTeamData.getAllTeams().values) {
            val match = team.players.any {
                (it.uuid.isNotEmpty() && it.uuid == player.uuid) || it.name == player.name
            }
            if (match) return team.displayName
        }
        return ""
    }

    // ──────────────────────────────────────────────────────────
    //  背景渲染（覆盖默认的材质背景）
    // ──────────────────────────────────────────────────────────

    override fun extractListBackground(guiGraphics: GuiGraphicsExtractor) {
        // 绘制半透明背景，替代默认的材质背景
        guiGraphics.fill(x, y, x + width, bottom, 0x55000000)
    }

    override fun extractListSeparators(guiGraphics: GuiGraphicsExtractor) {
        // 不渲染分隔线
    }
}