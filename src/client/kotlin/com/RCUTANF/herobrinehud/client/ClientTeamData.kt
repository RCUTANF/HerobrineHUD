package com.RCUTANF.herobrinehud.client

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

    // ──────────────── 查询接口（供 HUD 渲染使用） ────────────────

    fun getAllTeams(): Map<String, TeamInfo> = teams.toMap()

    fun getTeam(name: String): TeamInfo? = teams[name]

    fun getTeamCount(): Int = teams.size

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
        LOGGER.info("客户端队伍数据缓存已清空")
    }
}

