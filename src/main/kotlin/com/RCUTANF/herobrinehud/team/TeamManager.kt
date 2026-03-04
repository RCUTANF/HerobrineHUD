package com.RCUTANF.herobrinehud.team

import com.RCUTANF.herobrinehud.data.PlayerEffect
import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.RCUTANF.herobrinehud.data.TeamInfo
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.scores.PlayerTeam
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 队伍管理器
 * 维护一个本地数据库，实时同步原版 Scoreboard 中 Team 的状态
 * 所有玩家数据通过事件触发式更新，无需 tick 轮询
 */
object TeamManager {

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/TeamManager")

    /** 以队伍名称为 key 的队伍数据缓存 */
    private val teams = ConcurrentHashMap<String, TeamInfo>()

    /** 服务器引用 */
    private var server: MinecraftServer? = null

    // ──────────────── 生命周期 ────────────────

    /**
     * 服务器启动时调用，进行初始化全量同步
     */
    fun onServerStarted(server: MinecraftServer) {
        this.server = server
        LOGGER.info("服务器已启动，开始全量同步队伍数据...")
        fullSync(server)
        registerTeamEventListeners()
        registerPlayerDataEventListeners()
        LOGGER.info("队伍数据同步完成，共 {} 支队伍", teams.size)
    }

    /**
     * 服务器关闭时清理
     */
    fun onServerStopping(server: MinecraftServer) {
        LOGGER.info("服务器关闭，清理队伍数据缓存...")
        teams.clear()
        this.server = null
    }

    // ──────────────── 全量同步 ────────────────

    /**
     * 从服务器 Scoreboard 中读取所有队伍并同步到本地数据库
     */
    private fun fullSync(server: MinecraftServer) {
        teams.clear()
        val scoreboard = server.scoreboard

        for (team in scoreboard.playerTeams) {
            val teamInfo = convertTeam(team, server)
            teams[team.name] = teamInfo
            LOGGER.debug("同步队伍: {} (成员: {})", team.name, teamInfo.players.size)
        }
    }

    // ──────────────── 队伍事件监听 ────────────────

    /**
     * 注册队伍变更事件监听器（队伍增删改、成员加入/离开）
     */
    private fun registerTeamEventListeners() {
        TeamSyncCallback.TEAM_ADDED.register(TeamSyncCallback.TeamAdded { team ->
            val srv = server ?: return@TeamAdded
            val teamInfo = convertTeam(team, srv)
            teams[team.name] = teamInfo
            LOGGER.info("队伍已创建: {} (颜色: {})", team.name, teamInfo.color)
        })

        TeamSyncCallback.TEAM_REMOVED.register(TeamSyncCallback.TeamRemoved { team ->
            val removed = teams.remove(team.name)
            if (removed != null) {
                LOGGER.info("队伍已移除: {}", team.name)
            }
        })

        TeamSyncCallback.TEAM_MODIFIED.register(TeamSyncCallback.TeamModified { team ->
            val srv = server ?: return@TeamModified
            val existing = teams[team.name]
            if (existing != null) {
                val updated = convertTeam(team, srv)
                teams[team.name] = updated
                LOGGER.info("队伍已修改: {} (新颜色: {})", team.name, updated.color)
            } else {
                val teamInfo = convertTeam(team, srv)
                teams[team.name] = teamInfo
                LOGGER.warn("修改了不存在的队伍，已重新创建: {}", team.name)
            }
        })

        TeamSyncCallback.PLAYER_JOINED.register(TeamSyncCallback.PlayerJoinedTeam { team, playerName ->
            val srv = server ?: return@PlayerJoinedTeam
            val teamInfo = teams[team.name]
            if (teamInfo != null) {
                if (teamInfo.players.none { it.name == playerName }) {
                    val playerInfo = resolvePlayerInfo(playerName, srv)
                    teamInfo.players.add(playerInfo)
                    LOGGER.info("玩家 {} 加入队伍 {}", playerName, team.name)
                }
            } else {
                LOGGER.warn("玩家 {} 加入了不存在的队伍 {}", playerName, team.name)
            }
        })

        TeamSyncCallback.PLAYER_LEFT.register(TeamSyncCallback.PlayerLeftTeam { team, playerName ->
            val teamInfo = teams[team.name]
            if (teamInfo != null) {
                teamInfo.players.removeIf { it.name == playerName }
                LOGGER.info("玩家 {} 离开队伍 {}", playerName, team.name)
            }
        })
    }

    // ──────────────── 玩家数据事件监听（触发式更新） ────────────────

    /**
     * 注册玩家数据变更事件监听器
     * 替代原先的 tick 轮询刷新，实现按需精准更新
     */
    private fun registerPlayerDataEventListeners() {

        // 血量变化
        PlayerDataCallback.HEALTH_CHANGED.register(PlayerDataCallback.HealthChanged { player ->
            updatePlayerField(player) { info ->
                info.health = player.health.toDouble()
                info.maxHealth = player.maxHealth.toDouble()
            }
        })

        // 游戏模式变化
        PlayerDataCallback.GAMEMODE_CHANGED.register(PlayerDataCallback.GamemodeChanged { player, _, newMode ->
            updatePlayerField(player) { info ->
                info.gamemode = newMode.getName()
            }
            LOGGER.debug("玩家 {} 游戏模式变更为 {}", player.gameProfile.name, newMode.getName())
        })

        // 维度变化
        PlayerDataCallback.DIMENSION_CHANGED.register(PlayerDataCallback.DimensionChanged { player ->
            updatePlayerField(player) { info ->
                info.dimension = player.level().dimension().toString()
            }
            LOGGER.debug("玩家 {} 维度变更为 {}", player.gameProfile.name, player.level().dimension())
        })

        // 玩家死亡
        PlayerDataCallback.PLAYER_DIED.register(PlayerDataCallback.PlayerDied { player ->
            updatePlayerField(player) { info ->
                info.isAlive = false
                info.health = 0.0
            }
            LOGGER.debug("玩家 {} 已死亡", player.gameProfile.name)
        })

        // 玩家重生
        PlayerDataCallback.PLAYER_RESPAWNED.register(PlayerDataCallback.PlayerRespawned { player ->
            // 重生时做全量刷新，因为重生会创建新的 ServerPlayer 实例
            refreshSinglePlayer(player)
            LOGGER.debug("玩家 {} 已重生", player.gameProfile.name)
        })

        // 药水效果变化
        PlayerDataCallback.EFFECT_CHANGED.register(PlayerDataCallback.EffectChanged { player ->
            updatePlayerField(player) { info ->
                info.effects.clear()
                for (effectInstance in player.activeEffects) {
                    info.effects.add(
                        PlayerEffect(
                            name = effectInstance.effect.value().descriptionId,
                            identifier = effectInstance.effect.unwrapKey()
                                .map { it.identifier().toString() }
                                .orElse("unknown"),
                            amplifier = effectInstance.amplifier,
                            duration = effectInstance.duration / 20 // tick → 秒
                        )
                    )
                }
            }
        })

        // 装备变化（同时更新护甲值）
        PlayerDataCallback.EQUIPMENT_CHANGED.register(PlayerDataCallback.EquipmentChanged { player ->
            updatePlayerField(player) { info ->
                info.armor = player.armorValue
                // TODO: 如果需要详细装备信息，可在此处更新 info.equipment
            }
        })

        // 玩家加入服务器 — 全量刷新该玩家在队伍中的数据
        PlayerDataCallback.PLAYER_JOINED_SERVER.register(PlayerDataCallback.PlayerJoinedServer { player ->
            refreshSinglePlayer(player)
            LOGGER.info("玩家 {} 加入服务器，已刷新队伍数据", player.gameProfile.name)
        })

        // 玩家离开服务器 — 标记为离线状态
        PlayerDataCallback.PLAYER_LEFT_SERVER.register(PlayerDataCallback.PlayerLeftServer { player ->
            val playerName = player.gameProfile.name
            updatePlayerFieldByName(playerName) { info ->
                info.gamemode = "unknown"
                info.health = 0.0
                info.isAlive = false
                info.armor = 0
                info.dimension = null
                info.effects.clear()
            }
            LOGGER.info("玩家 {} 离开服务器，已标记为离线", playerName)
        })
    }

    // ──────────────── 细粒度更新工具方法 ────────────────

    /**
     * 根据 ServerPlayer 定位其在队伍中的 PlayerInfo 并执行更新操作
     * @param player 在线玩家
     * @param updater 对 PlayerInfo 执行的更新操作
     */
    private inline fun updatePlayerField(player: ServerPlayer, updater: (PlayerInfo) -> Unit) {
        val playerName = player.gameProfile.name
        updatePlayerFieldByName(playerName, updater)
    }

    /**
     * 根据玩家名定位其在队伍中的 PlayerInfo 并执行更新操作
     * @param playerName 玩家名
     * @param updater 对 PlayerInfo 执行的更新操作
     */
    private inline fun updatePlayerFieldByName(playerName: String, updater: (PlayerInfo) -> Unit) {
        for (teamInfo in teams.values) {
            val playerInfo = teamInfo.players.find { it.name == playerName }
            if (playerInfo != null) {
                updater(playerInfo)
                return
            }
        }
    }

    /**
     * 全量刷新单个玩家的数据（用于加入服务器、重生等场景）
     */
    private fun refreshSinglePlayer(player: ServerPlayer) {
        val playerName = player.gameProfile.name
        for (teamInfo in teams.values) {
            val idx = teamInfo.players.indexOfFirst { it.name == playerName }
            if (idx >= 0) {
                teamInfo.players[idx] = createPlayerInfoFromOnline(player)
                return
            }
        }
    }

    // ──────────────── 数据转换 ────────────────

    /**
     * 将原版 PlayerTeam 转换为自定义 TeamInfo
     */
    private fun convertTeam(team: PlayerTeam, server: MinecraftServer): TeamInfo {
        val color = formatColorToHex(team)
        val displayName = team.playerPrefix.string + team.displayName.string + team.playerSuffix.string

        val teamInfo = TeamInfo(
            name = team.name,
            displayName = displayName.ifBlank { team.name },
            color = color
        )

        // 同步队伍中的玩家
        for (memberName in team.players) {
            val playerInfo = resolvePlayerInfo(memberName, server)
            teamInfo.players.add(playerInfo)
        }

        return teamInfo
    }

    /**
     * 解析玩家信息
     * 优先从在线玩家获取详细信息，离线玩家则使用基本信息
     */
    private fun resolvePlayerInfo(playerName: String, server: MinecraftServer): PlayerInfo {
        val onlinePlayer: ServerPlayer? = server.playerList.getPlayerByName(playerName)

        return if (onlinePlayer != null) {
            createPlayerInfoFromOnline(onlinePlayer)
        } else {
            createPlayerInfoOffline(playerName)
        }
    }

    /**
     * 从在线玩家创建详细的 PlayerInfo
     */
    private fun createPlayerInfoFromOnline(player: ServerPlayer): PlayerInfo {
        val effects = player.activeEffects.map { effectInstance ->
            PlayerEffect(
                name = effectInstance.effect.value().descriptionId,
                identifier = effectInstance.effect.unwrapKey()
                    .map { it.identifier().toString() }
                    .orElse("unknown"),
                amplifier = effectInstance.amplifier,
                duration = effectInstance.duration / 20
            )
        }.toMutableList()

        return PlayerInfo(
            uuid = player.uuid.toString(),
            name = player.gameProfile.name,
            displayName = player.displayName?.string ?: player.gameProfile.name,
            gamemode = player.gameMode.gameModeForPlayer.getName(),
            health = player.health.toDouble(),
            maxHealth = player.maxHealth.toDouble(),
            armor = player.armorValue,
            isAlive = player.isAlive,
            dimension = player.level().dimension().toString(),
            effects = effects
        )
    }

    /**
     * 为离线玩家创建基本的 PlayerInfo
     */
    private fun createPlayerInfoOffline(playerName: String): PlayerInfo {
        return PlayerInfo(
            uuid = "",
            name = playerName,
            displayName = playerName,
            gamemode = "unknown",
            health = 0.0,
            maxHealth = 20.0,
            isAlive = false
        )
    }

    /**
     * 从队伍格式化颜色中提取 HEX 颜色值
     */
    private fun formatColorToHex(team: PlayerTeam): String {
        val chatColor = team.color
        val colorValue = chatColor.color
        return if (colorValue != null) {
            String.format("#%06X", colorValue)
        } else {
            "#FFFFFF"
        }
    }

    // ──────────────── 查询接口 ────────────────

    /**
     * 获取所有队伍信息（不可变视图）
     */
    fun getAllTeams(): Map<String, TeamInfo> = teams.toMap()

    /**
     * 根据队伍名称获取队伍信息
     */
    fun getTeam(name: String): TeamInfo? = teams[name]

    /**
     * 获取指定玩家所在的队伍
     */
    fun getPlayerTeam(playerName: String): TeamInfo? {
        return teams.values.find { teamInfo ->
            teamInfo.players.any { it.name == playerName }
        }
    }

    /**
     * 获取队伍总数
     */
    fun getTeamCount(): Int = teams.size
}

