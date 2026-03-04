package com.RCUTANF.herobrinehud.client

import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.RCUTANF.herobrinehud.data.TeamInfo
import org.slf4j.LoggerFactory

/**
 * HUD 选择状态管理器
 *
 * 管理当前导播界面选中的展示队伍，以及要隐藏的玩家。
 * 支持任意数量的队伍同时展示在屏幕上（通过槽位机制）。
 * 提供给 HUD 渲染层使用的过滤数据接口。
 */
object HudSelectionState {

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/Selection")

    // ════════════════════════════════════════════════════════════
    //  多队伍槽位管理（核心 API）
    // ════════════════════════════════════════════════════════════

    /**
     * 添加一个队伍展示槽位
     * @param teamName 队伍名称
     * @param side     展示侧（LEFT / RIGHT）
     * @return 分配的槽位索引
     */
    fun addDisplaySlot(teamName: String, side: DisplaySide = DisplaySide.LEFT): Int {
        HudConfig.update {
            // 避免重复添加同一支队伍
            displaySlots.removeAll { it.teamName == teamName }
            val newIndex = displaySlots.size
            displaySlots.add(DisplaySlot(index = newIndex, teamName = teamName, side = side))
        }
        val resultIndex = HudConfig.data.displaySlots.size - 1
        LOGGER.info("添加展示槽位: {} (side={}, index={})", teamName, side, resultIndex)
        return resultIndex
    }

    /**
     * 移除一个队伍展示槽位（按队伍名称）
     */
    fun removeDisplaySlot(teamName: String) {
        HudConfig.update {
            displaySlots.removeAll { it.teamName == teamName }
            reindexSlots()
        }
        LOGGER.info("移除展示槽位: {}", teamName)
    }

    /**
     * 移除一个队伍展示槽位（按索引）
     */
    fun removeDisplaySlotAt(index: Int) {
        HudConfig.update {
            if (index in displaySlots.indices) {
                val removed = displaySlots.removeAt(index)
                reindexSlots()
                LOGGER.info("移除展示槽位[{}]: {}", index, removed.teamName)
            }
        }
    }

    /**
     * 修改某个槽位的展示侧
     */
    fun setSlotSide(index: Int, side: DisplaySide) {
        HudConfig.update {
            if (index in displaySlots.indices) {
                displaySlots[index] = displaySlots[index].copy(side = side)
            }
        }
    }

    /**
     * 修改某个槽位绑定的队伍
     */
    fun setSlotTeam(index: Int, teamName: String) {
        HudConfig.update {
            if (index in displaySlots.indices) {
                displaySlots[index] = displaySlots[index].copy(teamName = teamName)
            }
        }
    }

    /**
     * 获取当前所有展示槽位（不可变拷贝）
     */
    fun getDisplaySlots(): List<DisplaySlot> = HudConfig.data.displaySlots.toList()

    /**
     * 获取指定侧的所有展示槽位
     */
    fun getSlotsBySide(side: DisplaySide): List<DisplaySlot> {
        return HudConfig.data.displaySlots.filter { it.side == side }
    }

    /**
     * 获取展示槽位总数
     */
    fun getDisplaySlotCount(): Int = HudConfig.data.displaySlots.size

    // ════════════════════════════════════════════════════════════
    //  队伍数据获取（多队伍）
    // ════════════════════════════════════════════════════════════

    /**
     * 获取指定侧所有队伍的完整数据
     */
    fun getTeamsBySide(side: DisplaySide): List<TeamInfo> {
        return getSlotsBySide(side).mapNotNull { slot ->
            ClientTeamData.getTeam(slot.teamName)
        }
    }

    /**
     * 获取指定侧所有队伍中需要显示的玩家列表
     */
    fun getPlayersBySide(side: DisplaySide): List<PlayerInfo> {
        return getTeamsBySide(side).flatMap { team ->
            team.players.filter { it.uuid !in HudConfig.data.hiddenPlayers }
        }
    }

    /**
     * 获取指定槽位对应的队伍数据
     */
    fun getTeamAtSlot(index: Int): TeamInfo? {
        val slot = HudConfig.data.displaySlots.getOrNull(index) ?: return null
        return ClientTeamData.getTeam(slot.teamName)
    }

    /**
     * 获取指定槽位中需要显示的玩家列表
     */
    fun getPlayersAtSlot(index: Int): List<PlayerInfo> {
        return getTeamAtSlot(index)?.players?.filter {
            it.uuid !in HudConfig.data.hiddenPlayers
        } ?: emptyList()
    }

    /**
     * 获取所有展示队伍的完整数据（按槽位顺序）
     */
    fun getAllDisplayTeams(): List<TeamInfo> {
        return HudConfig.data.displaySlots.mapNotNull { slot ->
            ClientTeamData.getTeam(slot.teamName)
        }
    }

    // ════════════════════════════════════════════════════════════
    //  向后兼容（左/右队伍快捷方法）
    // ════════════════════════════════════════════════════════════

    /**
     * 设置左侧展示队伍（兼容旧代码）
     */
    fun setLeftTeam(teamName: String?) {
        HudConfig.update {
            displaySlots.removeAll { it.side == DisplaySide.LEFT }
            if (teamName != null) {
                displaySlots.add(0, DisplaySlot(index = 0, teamName = teamName, side = DisplaySide.LEFT))
                reindexSlots()
            }
        }
        LOGGER.info("左侧队伍设置为: {}", teamName ?: "无")
    }

    /**
     * 设置右侧展示队伍（兼容旧代码）
     */
    fun setRightTeam(teamName: String?) {
        HudConfig.update {
            displaySlots.removeAll { it.side == DisplaySide.RIGHT }
            if (teamName != null) {
                displaySlots.add(DisplaySlot(index = displaySlots.size, teamName = teamName, side = DisplaySide.RIGHT))
                reindexSlots()
            }
        }
        LOGGER.info("右侧队伍设置为: {}", teamName ?: "无")
    }

    /**
     * 交换左右队伍
     */
    fun swapTeams() {
        HudConfig.update {
            for (i in displaySlots.indices) {
                val slot = displaySlots[i]
                val newSide = if (slot.side == DisplaySide.LEFT) DisplaySide.RIGHT else DisplaySide.LEFT
                displaySlots[i] = slot.copy(side = newSide)
            }
        }
        LOGGER.info("左右队伍已交换")
    }

    /**
     * 获取左侧队伍名称
     */
    fun getLeftTeamName(): String? = HudConfig.data.leftTeam

    /**
     * 获取右侧队伍名称
     */
    fun getRightTeamName(): String? = HudConfig.data.rightTeam

    /**
     * 获取左侧队伍的完整数据（如有）
     */
    fun getLeftTeam(): TeamInfo? {
        val name = HudConfig.data.leftTeam ?: return null
        return ClientTeamData.getTeam(name)
    }

    /**
     * 获取右侧队伍的完整数据（如有）
     */
    fun getRightTeam(): TeamInfo? {
        val name = HudConfig.data.rightTeam ?: return null
        return ClientTeamData.getTeam(name)
    }

    /**
     * 获取左侧队伍中需要显示的玩家列表
     */
    fun getLeftPlayers(): List<PlayerInfo> {
        return getLeftTeam()?.players?.filter {
            it.uuid !in HudConfig.data.hiddenPlayers
        } ?: emptyList()
    }

    /**
     * 获取右侧队伍中需要显示的玩家列表
     */
    fun getRightPlayers(): List<PlayerInfo> {
        return getRightTeam()?.players?.filter {
            it.uuid !in HudConfig.data.hiddenPlayers
        } ?: emptyList()
    }

    // ──────────── 玩家可见性 ────────────

    /**
     * 切换某玩家的可见性
     */
    fun togglePlayerVisibility(uuid: String) {
        HudConfig.update {
            if (uuid in hiddenPlayers) {
                hiddenPlayers.remove(uuid)
            } else {
                hiddenPlayers.add(uuid)
            }
        }
    }

    /**
     * 检查某玩家是否被隐藏
     */
    fun isPlayerHidden(uuid: String): Boolean {
        return uuid in HudConfig.data.hiddenPlayers
    }

    // ──────────── HUD 可见性 ────────────

    /**
     * 切换 HUD 显示/隐藏
     */
    fun toggleHudVisibility() {
        HudConfig.update { hudVisible = !hudVisible }
    }

    /**
     * 获取 HUD 是否可见
     */
    fun isHudVisible(): Boolean = HudConfig.data.hudVisible

    // ──────────── 工具方法 ────────────

    /**
     * 获取可供选择的所有队伍列表
     */
    fun getAvailableTeams(): List<TeamInfo> {
        return ClientTeamData.getAllTeams().values.toList()
    }

    /**
     * 判断是否有有效配置（至少选择了一个队伍）
     */
    fun hasValidSelection(): Boolean {
        return HudConfig.data.displaySlots.isNotEmpty()
    }

    /**
     * 清空所有选择
     */
    fun clearSelection() {
        HudConfig.update {
            displaySlots.clear()
            hiddenPlayers.clear()
        }
        LOGGER.info("已清空所有 HUD 选择")
    }

    // ──────────── 内部工具 ────────────

    private fun ConfigData.reindexSlots() {
        displaySlots.forEachIndexed { i, slot ->
            displaySlots[i] = slot.copy(index = i)
        }
    }
}

