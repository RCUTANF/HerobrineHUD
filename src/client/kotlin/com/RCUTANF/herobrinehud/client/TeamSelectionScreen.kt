package com.RCUTANF.herobrinehud.client

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * 队伍选择界面
 *
 * 允许用户添加/移除任意数量的队伍到屏幕左侧或右侧展示。
 */
class TeamSelectionScreen(parent: Screen?) : Screen(Component.literal("HerobrineHUD 队伍选择")) {

    private val parentScreen = parent

    override fun init() {
        super.init()
        rebuildWidgets()
    }

    override fun rebuildWidgets() {
        clearWidgets()

        val centerX = width / 2
        var y: Int

        // ══════════ 标题区 ══════════
        // 标题通过 render 方法绘制

        // ══════════ 当前展示槽位 ══════════
        y = 50
        val slots = HudSelectionState.getDisplaySlots()

        if (slots.isEmpty()) {
            // 无槽位时不渲染按钮，标签通过 render 绘制
            y += 14
        } else {
            for (slot in slots) {
                val team = ClientTeamData.getTeam(slot.teamName)
                val label = "${slot.teamName} [${slot.side.name}]"

                // 切换侧边按钮
                addRenderableWidget(Button.builder(Component.literal(slot.side.name)) { _ ->
                    val newSide = if (slot.side == DisplaySide.LEFT) DisplaySide.RIGHT else DisplaySide.LEFT
                    HudSelectionState.setSlotSide(slot.index, newSide)
                    rebuildWidgets()
                }.bounds(centerX - 120, y, 40, 20).build())

                // 队伍名标签按钮（不可点击，仅展示）
                addRenderableWidget(Button.builder(Component.literal(label)) { _ -> }
                    .bounds(centerX - 75, y, 130, 20).build())

                // 移除按钮
                addRenderableWidget(Button.builder(Component.literal("✕")) { _ ->
                    HudSelectionState.removeDisplaySlotAt(slot.index)
                    rebuildWidgets()
                }.bounds(centerX + 60, y, 20, 20).build())

                y += 24

                // 玩家可见性切换按钮
                if (team != null) {
                    for (player in team.players) {
                        val hidden = HudSelectionState.isPlayerHidden(player.uuid)
                        val icon = if (hidden) "✗" else "✓"
                        val playerLabel = "$icon ${player.name}"
                        addRenderableWidget(Button.builder(Component.literal(playerLabel)) { _ ->
                            HudSelectionState.togglePlayerVisibility(player.uuid)
                            rebuildWidgets()
                        }.bounds(centerX - 75, y, 155, 16).build())
                        y += 18
                    }
                }
            }
        }

        // ══════════ 功能按钮 ══════════
        y += 6

        // 交换左右
        addRenderableWidget(Button.builder(Component.literal("交换左右")) { _ ->
            HudSelectionState.swapTeams()
            rebuildWidgets()
        }.bounds(centerX - 100, y, 60, 20).build())

        // 清空���有
        addRenderableWidget(Button.builder(Component.literal("清空全部")) { _ ->
            HudSelectionState.clearSelection()
            rebuildWidgets()
        }.bounds(centerX - 30, y, 60, 20).build())

        // 切换 HUD 可见性
        val visLabel = if (HudSelectionState.isHudVisible()) "HUD: 开" else "HUD: 关"
        addRenderableWidget(Button.builder(Component.literal(visLabel)) { _ ->
            HudSelectionState.toggleHudVisibility()
            rebuildWidgets()
        }.bounds(centerX + 40, y, 60, 20).build())

        y += 30

        // ══════════ 可用队伍列表 ══════════
        val availableTeams = HudSelectionState.getAvailableTeams()
        val currentSlotNames = slots.map { it.teamName }.toSet()

        if (availableTeams.isEmpty()) {
            // 无队伍提示通过 render 绘制
        } else {
            for (team in availableTeams) {
                val alreadyAdded = team.name in currentSlotNames

                // 队伍名称
                addRenderableWidget(Button.builder(Component.literal(team.displayName)) { _ -> }
                    .bounds(centerX - 120, y, 100, 20).build())

                // 添加到左侧
                val leftBtn = Button.builder(Component.literal("◀ 左")) { _ ->
                    HudSelectionState.addDisplaySlot(team.name, DisplaySide.LEFT)
                    rebuildWidgets()
                }.bounds(centerX - 15, y, 45, 20).build()
                leftBtn.active = !alreadyAdded
                addRenderableWidget(leftBtn)

                // 添加到右侧
                val rightBtn = Button.builder(Component.literal("右 ▶")) { _ ->
                    HudSelectionState.addDisplaySlot(team.name, DisplaySide.RIGHT)
                    rebuildWidgets()
                }.bounds(centerX + 35, y, 45, 20).build()
                rightBtn.active = !alreadyAdded
                addRenderableWidget(rightBtn)

                y += 24
            }
        }

        // ══════════ 关闭按钮 ══════════
        addRenderableWidget(Button.builder(Component.literal("完成")) { _ ->
            onClose()
        }.bounds(centerX - 50, height - 30, 100, 20).build())
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        val centerX = width / 2

        // 标��
        guiGraphics.drawCenteredString(font, title, centerX, 10, 0xFFFFFF)

        // 当前展示区标签
        guiGraphics.drawCenteredString(font, "── 当前展示 ──", centerX, 38, 0xAAAAAA)

        val slots = HudSelectionState.getDisplaySlots()
        if (slots.isEmpty()) {
            guiGraphics.drawCenteredString(font, "尚未选择任何队伍", centerX, 55, 0x888888)
        }

        // 可用队伍区标签
        var slotsEndY = 50 + slots.size * 24 + 6 + 30 + 4
        // 计算玩家行占用的高度
        for (slot in slots) {
            val team = ClientTeamData.getTeam(slot.teamName)
            if (team != null) {
                slotsEndY += team.players.size * 18
            }
        }
        guiGraphics.drawCenteredString(font, "── 可用队伍 ──", centerX, slotsEndY, 0xAAAAAA)

        val availableTeams = HudSelectionState.getAvailableTeams()
        if (availableTeams.isEmpty()) {
            guiGraphics.drawCenteredString(font, "未从服务器同步到队伍数据", centerX, slotsEndY + 14, 0x888888)
        }
    }

    override fun onClose() {
        minecraft?.setScreen(parentScreen)
    }

    override fun isPauseScreen(): Boolean = false
}

