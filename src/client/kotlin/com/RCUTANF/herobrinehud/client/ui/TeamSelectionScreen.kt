package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.DisplaySide
import com.RCUTANF.herobrinehud.client.HudConfig
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
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
        val teams = HudSelectionState.getAvailableTeams()
        // 固定快捷按钮数量：交换、清空、HUD开关
        val fixedBtns = 3
        val teamBtnPairsW = teams.size * (qBtnW * 2 + qGap) + if (teams.isNotEmpty()) (teams.size - 1) * qGap else 0
        val fixedBtnsW   = fixedBtns * qBtnW + (fixedBtns - 1) * qGap
        val separatorW   = if (teams.isNotEmpty()) 10 else 0
        val row2TotalW   = fixedBtnsW + separatorW + teamBtnPairsW
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

        // 每个队伍的批量按钮：队名→全员左 / 全员右
        for (team in teams) {
            val label = team.displayName.take(6)
            addRenderableWidget(Button.builder(Component.literal("◀$label")) { _ ->
                HudSelectionState.batchSetTeamSide(team.name, DisplaySide.LEFT); rebuildWidgets()
            }.bounds(bx, row2Y, qBtnW, BTN_H).build())
            bx += qBtnW + 2

            addRenderableWidget(Button.builder(Component.literal("$label▶")) { _ ->
                HudSelectionState.batchSetTeamSide(team.name, DisplaySide.RIGHT); rebuildWidgets()
            }.bounds(bx, row2Y, qBtnW, BTN_H).build())
            bx += qBtnW + qGap
        }

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
        val mc = minecraft

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
