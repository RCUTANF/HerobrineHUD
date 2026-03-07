package com.RCUTANF.herobrinehud.client

import com.RCUTANF.herobrinehud.client.ui.HudSelectionState
import com.RCUTANF.herobrinehud.data.Equipment
import com.RCUTANF.herobrinehud.data.TeamInfo
import com.RCUTANF.herobrinehud.network.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 客户端队伍数据缓存
 *
 * 接收服务端推送的全量/增量数据并维护本地镜像，
 * 供 HUD 渲染层直接读取。
 */
object ClientTeamData {

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/ClientTeamData")

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** 客户端本地队伍数据缓存 */
    private val teams = ConcurrentHashMap<String, TeamInfo>()

    /** 是否已收到过全量数据 */
    var isSynced: Boolean = false
        private set

    // ──────────────── 客户端本地状态（不同步到服务器） ────────────────

    /** 玩家 UUID -> 快捷键编号 (0-9) 的映射 */
    private val playerHotkeyMap = ConcurrentHashMap<String, Int>()

    /** 当前正在旁观的玩家 UUID */
    @Volatile
    private var spectatingPlayerUuid: String? = null

    // ──────────────── 查询接口（供 HUD 渲染使用） ────────────────

    fun getAllTeams(): Map<String, TeamInfo> = teams.toMap()

    fun getTeam(name: String): TeamInfo? = teams[name]

    fun getTeamCount(): Int = teams.size

    // ──────────────── 快捷键管理 ────────────────

    /**
     * 设置玩家的快捷键编号
     * @param playerUuid 玩家 UUID
     * @param hotkeyNumber 快捷键编号 (0-9)，-1 表示清除
     */
    fun setPlayerHotkey(playerUuid: String, hotkeyNumber: Int) {
        if (hotkeyNumber < 0) {
            playerHotkeyMap.remove(playerUuid)
        } else {
            playerHotkeyMap[playerUuid] = hotkeyNumber
        }
    }

    /**
     * 获取玩家的快捷键编号
     * @param playerUuid 玩家 UUID
     * @return 快捷键编号 (0-9)，未设置则返回 -1
     */
    fun getPlayerHotkey(playerUuid: String): Int {
        return playerHotkeyMap[playerUuid] ?: -1
    }

    /**
     * 批量设置快捷键映射（用于 HUD 渲染前更新）
     * @param hotkeyMap UUID -> 快捷键编号的映射
     */
    fun updateHotkeyMappings(hotkeyMap: Map<String, Int>) {
        playerHotkeyMap.clear()
        playerHotkeyMap.putAll(hotkeyMap)
    }

    /**
     * 清空所有快捷键映射
     */
    fun clearHotkeyMappings() {
        playerHotkeyMap.clear()
    }

    // ──────────────── 旁观状态管理 ────────────────

    /**
     * 设置当前正在旁观的玩家
     * @param playerUuid 玩家 UUID，null 表示未旁观任何玩家
     */
    fun setSpectatingPlayer(playerUuid: String?) {
        spectatingPlayerUuid = playerUuid
    }

    /**
     * 获取当前正在旁观的玩家 UUID
     * @return 玩家 UUID，未旁观则返回 null
     */
    fun getSpectatingPlayer(): String? {
        return spectatingPlayerUuid
    }

    /**
     * 检查是否正在旁观指定玩家
     * @param playerUuid 玩家 UUID
     * @return true 如果正在旁观该玩家
     */
    fun isSpectating(playerUuid: String): Boolean {
        return spectatingPlayerUuid == playerUuid
    }

    // ──────────────── 数据处理 ────────────────

    /**
     * 处理全量同步数据
     */
    fun handleFullSync(jsonStr: String) {
        try {
            val data = json.decodeFromString<FullSyncData>(jsonStr)
            teams.clear()
            teams.putAll(data.teams)
            isSynced = true
            LOGGER.info("收到全量同步，共 {} 支队伍", teams.size)
        } catch (e: Exception) {
            LOGGER.error("解析全量同步数据失败: {}", e.message)
        }
        //初始化playerHotKeyMap
        HudSelectionState.updateHotkeyMappings()
    }

    /**
     * 处理增量更新
     */
    fun handleIncrementalUpdate(updateType: String, jsonStr: String) {
        try {
            when (updateType) {
                UpdateType.TEAM_ADDED -> {
                    val data = json.decodeFromString<TeamUpdateData>(jsonStr)
                    teams[data.team.name] = data.team
                    LOGGER.debug("队伍新增: {}", data.team.name)
                }

                UpdateType.TEAM_REMOVED -> {
                    val data = json.decodeFromString<TeamRemovedData>(jsonStr)
                    teams.remove(data.teamName)
                    LOGGER.debug("队伍移除: {}", data.teamName)
                }

                UpdateType.TEAM_MODIFIED -> {
                    val data = json.decodeFromString<TeamUpdateData>(jsonStr)
                    teams[data.team.name] = data.team
                    LOGGER.debug("队伍修改: {}", data.team.name)
                }

                UpdateType.PLAYER_JOINED_TEAM -> {
                    val data = json.decodeFromString<PlayerJoinedTeamData>(jsonStr)
                    val teamInfo = teams[data.teamName]
                    if (teamInfo != null) {
                        if (teamInfo.players.none { it.name == data.player.name }) {
                            teamInfo.players.add(data.player)
                        }
                        LOGGER.debug("玩家 {} 加入队伍 {}", data.player.name, data.teamName)
                    }
                }

                UpdateType.PLAYER_LEFT_TEAM -> {
                    val data = json.decodeFromString<PlayerLeftTeamData>(jsonStr)
                    val teamInfo = teams[data.teamName]
                    teamInfo?.players?.removeIf { it.name == data.playerName }
                    LOGGER.debug("玩家 {} 离开队伍 {}", data.playerName, data.teamName)
                }

                UpdateType.PLAYER_DATA_UPDATED, UpdateType.PLAYER_JOINED_SERVER -> {
                    val data = json.decodeFromString<PlayerDataUpdatedData>(jsonStr)
                    val teamInfo = teams[data.teamName]
                    if (teamInfo != null) {
                        val idx = teamInfo.players.indexOfFirst { it.name == data.player.name }
                        if (idx >= 0) {
                            teamInfo.players[idx] = data.player
                        } else {
                            teamInfo.players.add(data.player)
                        }
                        LOGGER.debug("玩家 {} 数据已更新", data.player.name)
                    }
                }

                UpdateType.PLAYER_LEFT_SERVER -> {
                    val data = json.decodeFromString<PlayerLeftServerData>(jsonStr)
                    for (teamInfo in teams.values) {
                        val player = teamInfo.players.find { it.name == data.playerName }
                        if (player != null) {
                            player.gamemode = "unknown"
                            player.health = 0.0
                            player.isAlive = false
                            player.armor = 0
                            player.dimension = null
                            player.effects.clear()
                            player.equipment = Equipment()
                            LOGGER.debug("玩家 {} 已标记离线", data.playerName)
                            break
                        }
                    }
                }

                else -> {
                    LOGGER.warn("收到未知增量更新类型: {}", updateType)
                }
            }
        } catch (e: Exception) {
            LOGGER.error("解析增量更新数据失败 (type={}): {}", updateType, e.message)
        }
    }

    /**
     * 清空本地缓存（断开连接时调用）
     */
    fun clear() {
        teams.clear()
        isSynced = false
        playerHotkeyMap.clear()
        spectatingPlayerUuid = null
        LOGGER.info("客户端队伍数据缓存已清空")
    }
}

