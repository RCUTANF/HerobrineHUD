package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.DisplaySide
import com.RCUTANF.herobrinehud.client.HudConfig
import com.RCUTANF.herobrinehud.client.hud.HudCenter
import com.RCUTANF.herobrinehud.data.TeamInfo
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

class TeamSelectionScreen(parent: Screen?) : Screen(Component.literal("HerobrineHUD 分配")) {

    private val parentScreen = parent
    private val TOP_BAR_H = 58
    private val BTN_H = 16
    private val COL_GAP = 4
    private val TITLE_H = 14
    private val BOTTOM_H = 28

    private var leftPanel: PlayerListPanel? = null
    private var rightPanel: PlayerListPanel? = null
    private var nonePanel: PlayerListPanel? = null
    private var selectedTeamName: String? = null
    private var isTeamDropdownOpen: Boolean = false

    private var dropdownSelectorX: Int = 0
    private var dropdownSelectorY: Int = 0
    private var dropdownSelectorW: Int = 0
    private var dropdownSelectorH: Int = 0
    private var dropdownMenuX: Int = 0
    private var dropdownMenuY: Int = 0
    private var dropdownMenuW: Int = 0
    private var dropdownMenuH: Int = 0

    override fun init() {
        super.init()
        buildLayout()
    }

    override fun rebuildWidgets() {
        super.rebuildWidgets()
        buildLayout()
    }

    private fun buildLayout() {
        clearWidgets()
        leftPanel = null
        rightPanel = null
        nonePanel = null

        val cfg = HudConfig.data
        val row1Y = 10
        val row2Y = row1Y + BTN_H + 4

        fun toggleLabel(label: String, on: Boolean) = if (on) "[$label:开]" else "[$label:关]"

        addRenderableWidget(Button.builder(Component.literal(hudProviderLabel())) { _ ->
            HudCenter.selectNext()
            rebuildWidgets()
        }.bounds(8, row1Y, 86, BTN_H).build())

        val btnW1 = 54
        val totalW1 = btnW1 * 4 + 6 * 3
        var bx = (width - totalW1) / 2

        addRenderableWidget(Button.builder(Component.literal(toggleLabel("头像", cfg.showAvatar))) { _ ->
            HudConfig.update { showAvatar = !showAvatar }; rebuildWidgets()
        }.bounds(bx, row1Y, btnW1, BTN_H).build())
        bx += btnW1 + 6

        addRenderableWidget(Button.builder(Component.literal(toggleLabel("HP数字", cfg.showHealthNumber))) { _ ->
            HudConfig.update { showHealthNumber = !showHealthNumber }; rebuildWidgets()
        }.bounds(bx, row1Y, btnW1, BTN_H).build())
        bx += btnW1 + 6

        addRenderableWidget(Button.builder(Component.literal(toggleLabel("护甲", cfg.showArmor))) { _ ->
            HudConfig.update { showArmor = !showArmor }; rebuildWidgets()
        }.bounds(bx, row1Y, btnW1, BTN_H).build())
        bx += btnW1 + 6

        addRenderableWidget(Button.builder(Component.literal(toggleLabel("维度", cfg.showDimension))) { _ ->
            HudConfig.update { showDimension = !showDimension }; rebuildWidgets()
        }.bounds(bx, row1Y, btnW1, BTN_H).build())

        bx = (width - totalW1) / 2 + totalW1 + 10

        addRenderableWidget(Button.builder(Component.literal(toggleLabel("装备", cfg.showEquipment))) { _ ->
            HudConfig.update { showEquipment = !showEquipment }; rebuildWidgets()
        }.bounds(bx, row1Y, btnW1, BTN_H).build())
        bx += btnW1 + 6

        addRenderableWidget(Button.builder(Component.literal(toggleLabel("效果", cfg.showEffects))) { _ ->
            HudConfig.update { showEffects = !showEffects }; rebuildWidgets()
        }.bounds(bx, row1Y, btnW1, BTN_H).build())

        val visLabel = if (HudSelectionState.isHudVisible()) "HUD: 开" else "HUD: 关"
        val qBtnW = 58
        val qGap = 6
        val teamSelectorW = 130
        val batchBtnW = 62
        val teams = HudSelectionState.getAvailableTeams().sortedBy { it.displayName.ifBlank { it.name } }
        selectedTeamName = normalizeSelectedTeam(teams)
        val selectedTeam = teams.firstOrNull { it.name == selectedTeamName }

        val fixedBtns = 3
        val fixedBtnsW = fixedBtns * qBtnW + (fixedBtns - 1) * qGap
        val separatorW = if (teams.isNotEmpty()) 10 else 0
        val batchControlsW = teamSelectorW + qGap + batchBtnW * 2 + qGap
        val row2TotalW = fixedBtnsW + separatorW + batchControlsW
        bx = (width - row2TotalW) / 2

        addRenderableWidget(Button.builder(Component.literal("交换左右")) { _ ->
            HudSelectionState.swapSides(); rebuildWidgets()
        }.bounds(bx, row2Y, qBtnW, BTN_H).build())
        bx += qBtnW + qGap

        addRenderableWidget(Button.builder(Component.literal("清空全部")) { _ ->
            HudSelectionState.clearSelection(); rebuildWidgets()
        }.bounds(bx, row2Y, qBtnW, BTN_H).build())
        bx += qBtnW + qGap

        addRenderableWidget(Button.builder(Component.literal(visLabel)) { _ ->
            HudSelectionState.toggleHudVisibility(); rebuildWidgets()
        }.bounds(bx, row2Y, qBtnW, BTN_H).build())
        bx += qBtnW + separatorW + qGap

        val selector = Button.builder(Component.literal(teamSelectorLabel(selectedTeam, isTeamDropdownOpen))) { _ ->
            if (teams.isNotEmpty()) {
                isTeamDropdownOpen = !isTeamDropdownOpen
                rebuildWidgets()
            }
        }.bounds(bx, row2Y, teamSelectorW, BTN_H).build().apply {
            active = teams.isNotEmpty()
        }
        dropdownSelectorX = bx
        dropdownSelectorY = row2Y
        dropdownSelectorW = teamSelectorW
        dropdownSelectorH = BTN_H
        addRenderableWidget(selector)
        bx += teamSelectorW + qGap

        val moveLeftButton = Button.builder(Component.literal("分到左栏")) { _ ->
            selectedTeamName?.let {
                HudSelectionState.batchSetTeamSide(it, DisplaySide.LEFT)
                rebuildWidgets()
            }
        }.bounds(bx, row2Y, batchBtnW, BTN_H).build().apply {
            active = selectedTeam != null
        }
        addRenderableWidget(moveLeftButton)
        bx += batchBtnW + qGap

        val moveRightButton = Button.builder(Component.literal("分到右栏")) { _ ->
            selectedTeamName?.let {
                HudSelectionState.batchSetTeamSide(it, DisplaySide.RIGHT)
                rebuildWidgets()
            }
        }.bounds(bx, row2Y, batchBtnW, BTN_H).build().apply {
            active = selectedTeam != null
        }
        addRenderableWidget(moveRightButton)

        val panelTop = TOP_BAR_H + TITLE_H
        val panelBottom = height - BOTTOM_H
        val panelH = (panelBottom - panelTop).coerceAtLeast(20)
        val totalColW = width - 16
        val colW = (totalColW - COL_GAP * 2) / 3
        val col1X = 8
        val col2X = col1X + colW + COL_GAP
        val col3X = col2X + colW + COL_GAP

        val lPanel = PlayerListPanel(minecraft, colW, panelH, panelTop, DisplaySide.LEFT) { uuid, side -> move(uuid, side) }
        val rPanel = PlayerListPanel(minecraft, colW, panelH, panelTop, DisplaySide.RIGHT) { uuid, side -> move(uuid, side) }
        val nPanel = PlayerListPanel(minecraft, colW, panelH, panelTop, DisplaySide.NONE) { uuid, side -> move(uuid, side) }

        lPanel.setPosition(col1X, panelTop)
        rPanel.setPosition(col2X, panelTop)
        nPanel.setPosition(col3X, panelTop)
        leftPanel = lPanel
        rightPanel = rPanel
        nonePanel = nPanel
        addRenderableWidget(lPanel)
        addRenderableWidget(rPanel)
        addRenderableWidget(nPanel)
        refreshPanels()

        addRenderableWidget(Button.builder(Component.literal("完成")) { _ ->
            onClose()
        }.bounds(width / 2 - 50, height - BOTTOM_H + 4, 100, 20).build())

        if (isTeamDropdownOpen && teams.isNotEmpty()) {
            addTeamDropdownEntries(teams, row2Y)
        } else {
            dropdownMenuX = dropdownSelectorX
            dropdownMenuY = dropdownSelectorY + dropdownSelectorH + 1
            dropdownMenuW = dropdownSelectorW
            dropdownMenuH = 0
        }
    }

    private fun refreshPanels() {
        leftPanel?.setPlayers(HudSelectionState.getPlayersBySide(DisplaySide.LEFT))
        rightPanel?.setPlayers(HudSelectionState.getPlayersBySide(DisplaySide.RIGHT))
        nonePanel?.setPlayers(HudSelectionState.getUnassignedPlayers())
    }

    private fun move(uuid: String, side: DisplaySide) {
        HudSelectionState.setPlayerSide(uuid, side)
        refreshPanels()
    }

    private fun normalizeSelectedTeam(teams: List<TeamInfo>): String? {
        if (teams.isEmpty()) return null
        if (selectedTeamName == null) return teams.first().name
        return teams.firstOrNull { it.name == selectedTeamName }?.name ?: teams.first().name
    }

    private fun teamSelectorLabel(selectedTeam: TeamInfo?, isOpen: Boolean): String {
        if (selectedTeam == null) return "队伍: 无"
        val label = selectedTeam.displayName.ifBlank { selectedTeam.name }.take(8)
        val arrow = if (isOpen) "▲" else "▼"
        return "队伍: $label $arrow"
    }

    private fun hudProviderLabel(): String {
        val name = HudCenter.current()?.displayName ?: "Unknown"
        return "HUD: ${name.take(10)}"
    }

    private fun addTeamDropdownEntries(teams: List<TeamInfo>, row2Y: Int) {
        val menuX = dropdownSelectorX
        val menuY = row2Y + BTN_H + 2
        val itemH = BTN_H
        val menuW = dropdownSelectorW

        dropdownMenuX = menuX
        dropdownMenuY = menuY
        dropdownMenuW = menuW
        dropdownMenuH = teams.size * itemH

        teams.forEachIndexed { index, team ->
            val y = menuY + index * itemH
            val isSelected = team.name == selectedTeamName
            val labelName = team.displayName.ifBlank { team.name }.take(10)
            val prefix = if (isSelected) "* " else "  "
            addRenderableWidget(Button.builder(Component.literal(prefix + labelName)) { _ ->
                selectedTeamName = team.name
                isTeamDropdownOpen = false
                rebuildWidgets()
            }.bounds(menuX, y, menuW, itemH).build())
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (isTeamDropdownOpen) {
            val x = event.x.toInt()
            val y = event.y.toInt()
            val insideSelector = x >= dropdownSelectorX && x < dropdownSelectorX + dropdownSelectorW &&
                y >= dropdownSelectorY && y < dropdownSelectorY + dropdownSelectorH
            val insideMenu = x >= dropdownMenuX && x < dropdownMenuX + dropdownMenuW &&
                y >= dropdownMenuY && y < dropdownMenuY + dropdownMenuH
            if (!insideSelector && !insideMenu) {
                isTeamDropdownOpen = false
                rebuildWidgets()
            }
        }
        return super.mouseClicked(event, isDoubleClick)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTick)

        guiGraphics.drawCenteredString(font, title, width / 2, 1, 0xFFFFFF)
        guiGraphics.fill(8, TOP_BAR_H - 2, width - 8, TOP_BAR_H - 1, 0x66FFFFFF)

        val totalColW = width - 16
        val colW = (totalColW - COL_GAP * 2) / 3
        val col1X = 8
        val col2X = col1X + colW + COL_GAP
        val col3X = col2X + colW + COL_GAP
        val titleY = TOP_BAR_H + 2

        val lCount = HudSelectionState.getPlayersBySide(DisplaySide.LEFT).size
        val rCount = HudSelectionState.getPlayersBySide(DisplaySide.RIGHT).size
        val nCount = HudSelectionState.getUnassignedPlayers().size

        guiGraphics.drawCenteredString(font, "◀ 左侧 ($lCount)", col1X + colW / 2, titleY, 0x55AAFF)
        guiGraphics.drawCenteredString(font, "右侧 ($rCount) ▶", col2X + colW / 2, titleY, 0xFF9955)
        guiGraphics.drawCenteredString(font, "未上屏 ($nCount)", col3X + colW / 2, titleY, 0xAAAAAA)

        val panelTop = TOP_BAR_H + TITLE_H
        val panelBottom = height - BOTTOM_H
        guiGraphics.fill(col2X - 2, panelTop, col2X - 1, panelBottom, 0x44FFFFFF)
        guiGraphics.fill(col3X - 2, panelTop, col3X - 1, panelBottom, 0x44FFFFFF)
    }

    override fun onClose() { minecraft.setScreen(parentScreen) }
    override fun isPauseScreen(): Boolean = false
}
