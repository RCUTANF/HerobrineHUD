package com.RCUTANF.herobrinehud.client

import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.RCUTANF.herobrinehud.data.TeamInfo
import org.slf4j.LoggerFactory

/**
 * HUD 选择状态管理器（玩家级分配）
 *
 * 每个玩家可独立分配到屏幕左侧（LEFT）、右侧（RIGHT）或不上屏（NONE）。
 * 分配信息持久化到 HudConfig.data.playerPlacements。
 */
object HudSelectionState {

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/Selection")

    // ════════════════════════════════════════════════════════════
    //  玩家分配核心 API
    // ════════════════════════════════════════════════════════════

    /**
     * 设置指定玩家的上屏侧
     * @param uuid  玩家 UUID
     * @param side  LEFT / RIGHT / NONE
     */
    fun setPlayerSide(uuid: String, side: DisplaySide) {
        HudConfig.update {
            if (side == DisplaySide.NONE) {
                playerPlacements.remove(uuid)
            } else {
                playerPlacements[uuid] = PlayerPlacement(uuid = uuid, side = side)
            }
        }
        LOGGER.debug("玩家 {} 分配至 {}", uuid, side)
    }

    /**
     * 获取指定玩家的当前侧位
     */
    fun getPlayerSide(uuid: String): DisplaySide {
        return HudConfig.data.playerPlacements[uuid]?.side ?: DisplaySide.NONE
    }

    /**
     * 获取指定侧上屏的所有 PlayerInfo（按服务器同步数据）
     */
    fun getPlayersBySide(side: DisplaySide): List<PlayerInfo> {
        val uuids = HudConfig.data.playerPlacements
            .values
            .filter { it.side == side }
            .map { it.uuid }
            .toSet()
        return ClientTeamData.getAllTeams().values
            .flatMap { it.players }
            .filter { it.uuid in uuids }
    }

    /**
     * 获取所有已知玩家（来自所有同步队伍），无论是否上屏
     */
    fun getAllKnownPlayers(): List<PlayerInfo> {
        return ClientTeamData.getAllTeams().values.flatMap { it.players }
    }

    /**
     * 获取当前未分配上屏（NONE）的玩家列表
     */
    fun getUnassignedPlayers(): List<PlayerInfo> {
        val assignedUuids = HudConfig.data.playerPlacements
            .values.filter { it.side != DisplaySide.NONE }
            .map { it.uuid }.toSet()
        return getAllKnownPlayers().filter { it.uuid !in assignedUuids }
    }

    // ════════════════════════════════════════════════════════════
    //  批量快捷操作
    // ════════════════════════════════════════════════════════════

    /**
     * 将整个队伍的所有玩家批量分配到指定侧
     * @param teamName 队伍名称
     * @param side     目标侧（LEFT / RIGHT / NONE）
     */
    fun batchSetTeamSide(teamName: String, side: DisplaySide) {
        val team = ClientTeamData.getTeam(teamName) ?: return
        HudConfig.update {
            for (player in team.players) {
                if (side == DisplaySide.NONE) {
                    playerPlacements.remove(player.uuid)
                } else {
                    playerPlacements[player.uuid] = PlayerPlacement(uuid = player.uuid, side = side)
                }
            }
        }
        LOGGER.info("队伍 {} 批量分配至 {}", teamName, side)
    }

    /**
     * 交换左右两侧所有玩家
     */
    fun swapSides() {
        HudConfig.update {
            val entries = playerPlacements.values.toList()
            for (p in entries) {
                val newSide = when (p.side) {
                    DisplaySide.LEFT  -> DisplaySide.RIGHT
                    DisplaySide.RIGHT -> DisplaySide.LEFT
                    DisplaySide.NONE  -> DisplaySide.NONE
                }
                playerPlacements[p.uuid] = p.copy(side = newSide)
            }
        }
        LOGGER.info("左右侧玩家已交换")
    }

    /**
     * 清空所有分配
     */
    fun clearSelection() {
        HudConfig.update {
            playerPlacements.clear()
        }
        LOGGER.info("已清空所有 HUD 分配")
    }

    // ════════════════════════════════════════════════════════════
    //  HUD 可见性
    // ════════════════════════════════════════════════════════════

    fun toggleHudVisibility() {
        HudConfig.update { hudVisible = !hudVisible }
    }

    fun isHudVisible(): Boolean = HudConfig.data.hudVisible

    // ════════════════════════════════════════════════════════════
    //  辅助查询
    // ════════════════════════════════════════════════════════════

    /**
     * 判断是否有有效的上屏配置（至少一名玩家上屏）
     */
    fun hasValidSelection(): Boolean {
        return HudConfig.data.playerPlacements.values.any {
            it.side == DisplaySide.LEFT || it.side == DisplaySide.RIGHT
        }
    }

    /**
     * 获取可供操作的所有队伍列表
     */
    fun getAvailableTeams(): List<TeamInfo> {
        return ClientTeamData.getAllTeams().values.toList()
    }

    // ════════════════════════════════════════════════════════════
    //  向后兼容：保留旧方法签名，防止其他地方编译失败
    // ════════════════════════════════════════════════════════════

    @Deprecated("改用 setPlayerSide", ReplaceWith("batchSetTeamSide(teamName, DisplaySide.LEFT)"))
    fun setLeftTeam(teamName: String?) {
        if (teamName != null) batchSetTeamSide(teamName, DisplaySide.LEFT)
    }

    @Deprecated("改用 setPlayerSide", ReplaceWith("batchSetTeamSide(teamName, DisplaySide.RIGHT)"))
    fun setRightTeam(teamName: String?) {
        if (teamName != null) batchSetTeamSide(teamName, DisplaySide.RIGHT)
    }

    @Deprecated("改用 swapSides")
    fun swapTeams() = swapSides()

    @Deprecated("改用 getPlayersBySide")
    fun getTeamsBySide(side: DisplaySide): List<TeamInfo> = emptyList()
}
