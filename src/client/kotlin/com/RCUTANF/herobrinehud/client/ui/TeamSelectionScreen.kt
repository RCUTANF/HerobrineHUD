package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.DisplaySide
import com.RCUTANF.herobrinehud.client.HudConfig
import com.RCUTANF.herobrinehud.data.TeamInfo
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * 玩家上屏分配界面（重构版）
 *
 * 布局：
 *  ┌──────────────────────────────────────────────────────┐
 *  │  顶部设置栏：显示开关 + 快捷操作                         │
 *  ├───────────────┬───────────────┬───────────────────────┤
 *  │  左侧玩家列表  │  右侧玩家列表  │    未上屏玩家列表        │
 *  │  （可滚动）   │  （可滚动）   │    （可滚动）            │
 *  └───────────────┴───────────────┴───────────────────────┘
 *                            [完成]
 */
class TeamSelectionScreen(parent: Screen?) : Screen(Component.literal("HerobrineHUD 分配")) {

    private val parentScreen = parent

    // ── 布局常量 ──────────────────────────────────────────
    private val TOP_BAR_H   = 58   // 顶部设置栏高度
    private val BTN_H       = 16   // 小按钮高度
    private val COL_GAP     = 4    // 列间距
    private val TITLE_H     = 14   // 列标题高度
    private val BOTTOM_H    = 28   // 底部完成按钮区高度

    // ── 三个面板（init 中创建） ────────────────────────────
    private var leftPanel:   PlayerListPanel? = null
    private var rightPanel:  PlayerListPanel? = null
    private var nonePanel:   PlayerListPanel? = null
    private var selectedTeamName: String? = null
    private var isTeamDropdownOpen: Boolean = false

    // 下拉框区域（用于点击外部时自动收起）
    private var dropdownSelectorX: Int = 0
    private var dropdownSelectorY: Int = 0
    private var dropdownSelectorW: Int = 0
    private var dropdownSelectorH: Int = 0
    private var dropdownMenuX: Int = 0
    private var dropdownMenuY: Int = 0
    private var dropdownMenuW: Int = 0
    private var dropdownMenuH: Int = 0


    // ─────────────────────────────────────────────────────
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
        leftPanel  = null
        rightPanel = null
        nonePanel  = null

        val cfg = HudConfig.data

        // ══════════ 顶部设置栏 ══════════
        val row1Y = 10
        val row2Y = row1Y + BTN_H + 4

        // 辅助：切换标签
        fun toggleLabel(label: String, on: Boolean) = if (on) "[$label:开]" else "[$label:关]"

        // ── 第一行：显示开关 ──
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

        // 第一行右侧：装备 / 效果
        bx = (width - totalW1) / 2 + totalW1 + 10

        addRenderableWidget(Button.builder(Component.literal(toggleLabel("装备", cfg.showEquipment))) { _ ->
            HudConfig.update { showEquipment = !showEquipment }; rebuildWidgets()
        }.bounds(bx, row1Y, btnW1, BTN_H).build())
        bx += btnW1 + 6

        addRenderableWidget(Button.builder(Component.literal(toggleLabel("效果", cfg.showEffects))) { _ ->
            HudConfig.update { showEffects = !showEffects }; rebuildWidgets()
        }.bounds(bx, row1Y, btnW1, BTN_H).build())

        // ── 第二行：快捷操作 ──
        // HUD 开关
        val visLabel = if (HudSelectionState.isHudVisible()) "HUD: 开" else "HUD: 关"
        val qBtnW = 58
        val qGap  = 6
        val teamSelectorW = 130
        val batchBtnW = 62
        val teams = HudSelectionState.getAvailableTeams().sortedBy { it.displayName.ifBlank { it.name } }
        selectedTeamName = normalizeSelectedTeam(teams)
        val selectedTeam = teams.firstOrNull { it.name == selectedTeamName }

        // 固定快捷按钮数量：交换、清空、HUD开关
        val fixedBtns = 3
        val fixedBtnsW   = fixedBtns * qBtnW + (fixedBtns - 1) * qGap
        val separatorW   = if (teams.isNotEmpty()) 10 else 0
        val batchControlsW = teamSelectorW + qGap + batchBtnW * 2 + qGap
        val row2TotalW   = fixedBtnsW + separatorW + batchControlsW
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

        // 队伍选择 + 批量分配左右（避免队伍多时按钮挤爆一行）
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

        // ══════════ 三列面板 ══════════
        val panelTop    = TOP_BAR_H + TITLE_H
        val panelBottom = height - BOTTOM_H
        val panelH      = (panelBottom - panelTop).coerceAtLeast(20)
        val totalColW   = width - 16   // 左右各留 8px 边距
        val colW        = (totalColW - COL_GAP * 2) / 3
        val col1X       = 8
        val col2X       = col1X + colW + COL_GAP
        val col3X       = col2X + colW + COL_GAP

        // 构建三个面板
        val lPanel = PlayerListPanel(minecraft, colW, panelH, panelTop, DisplaySide.LEFT)  { uuid, side -> move(uuid, side) }
        val rPanel = PlayerListPanel(minecraft, colW, panelH, panelTop, DisplaySide.RIGHT) { uuid, side -> move(uuid, side) }
        val nPanel = PlayerListPanel(minecraft, colW, panelH, panelTop, DisplaySide.NONE)  { uuid, side -> move(uuid, side) }

        // 设置面板的 X 坐标
        lPanel.setPosition(col1X, panelTop)
        rPanel.setPosition(col2X, panelTop)
        nPanel.setPosition(col3X, panelTop)
        leftPanel = lPanel
        rightPanel = rPanel
        nonePanel = nPanel
        // 添加到布局
        addRenderableWidget(lPanel)
        addRenderableWidget(rPanel)
        addRenderableWidget(nPanel)
        // 初始化数据
        refreshPanels()

        // ══════════ 完成按钮 ══════════
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

    /** 刷新三个面板的数据 */
    private fun refreshPanels() {
        leftPanel?.setPlayers(HudSelectionState.getPlayersBySide(DisplaySide.LEFT))
        rightPanel?.setPlayers(HudSelectionState.getPlayersBySide(DisplaySide.RIGHT))
        nonePanel?.setPlayers(HudSelectionState.getUnassignedPlayers())
    }

    /** 移动玩家到指定侧并刷新界面 */
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

    // ──────────────────────────────────────────────────────────
    //  渲染
    // ──────────────────────────────────────────────────────────

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick)

        // 标题
        guiGraphics.centeredText(font, title, width / 2, 1, 0xFFFFFF)

        // 顶部分隔线
        guiGraphics.fill(8, TOP_BAR_H - 2, width - 8, TOP_BAR_H - 1, 0x66FFFFFF)

        // 三列标题
        val totalColW = width - 16
        val colW = (totalColW - COL_GAP * 2) / 3
        val col1X = 8
        val col2X = col1X + colW + COL_GAP
        val col3X = col2X + colW + COL_GAP
        val titleY = TOP_BAR_H + 2

        val lCount = HudSelectionState.getPlayersBySide(DisplaySide.LEFT).size
        val rCount = HudSelectionState.getPlayersBySide(DisplaySide.RIGHT).size
        val nCount = HudSelectionState.getUnassignedPlayers().size

        guiGraphics.centeredText(font, "◀ 左侧 ($lCount)",  col1X + colW / 2, titleY, 0x55AAFF)
        guiGraphics.centeredText(font, "右侧 ($rCount) ▶", col2X + colW / 2, titleY, 0xFF9955)
        guiGraphics.centeredText(font, "未上屏 ($nCount)",  col3X + colW / 2, titleY, 0xAAAAAA)

        // 列间分隔线
        val panelTop    = TOP_BAR_H + TITLE_H
        val panelBottom = height - BOTTOM_H
        guiGraphics.fill(col2X - 2, panelTop, col2X - 1, panelBottom, 0x44FFFFFF)
        guiGraphics.fill(col3X - 2, panelTop, col3X - 1, panelBottom, 0x44FFFFFF)
    }



    // ──────────────────────────────────────────────────────────
    override fun onClose() { minecraft.setScreen(parentScreen) }
    override fun isPauseScreen(): Boolean = false
}
