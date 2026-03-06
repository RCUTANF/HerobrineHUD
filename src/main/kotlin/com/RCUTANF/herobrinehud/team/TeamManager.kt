package com.RCUTANF.herobrinehud.team

import com.RCUTANF.herobrinehud.data.AvatarResolver
import com.RCUTANF.herobrinehud.data.Equipment
import com.RCUTANF.herobrinehud.data.PlayerEffect
import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.RCUTANF.herobrinehud.data.TeamInfo
import com.RCUTANF.herobrinehud.network.TeamSyncManager
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
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
            TeamSyncManager.notifyTeamAdded(teamInfo)
        })

        TeamSyncCallback.TEAM_REMOVED.register(TeamSyncCallback.TeamRemoved { team ->
            val removed = teams.remove(team.name)
            if (removed != null) {
                LOGGER.info("队伍已移除: {}", team.name)
                TeamSyncManager.notifyTeamRemoved(team.name)
            }
        })

        TeamSyncCallback.TEAM_MODIFIED.register(TeamSyncCallback.TeamModified { team ->
            val srv = server ?: return@TeamModified
            val existing = teams[team.name]
            if (existing != null) {
                val updated = convertTeam(team, srv)
                teams[team.name] = updated
                LOGGER.info("队伍已修改: {} (新颜色: {})", team.name, updated.color)
                TeamSyncManager.notifyTeamModified(updated)
            } else {
                val teamInfo = convertTeam(team, srv)
                teams[team.name] = teamInfo
                LOGGER.warn("修改了不存在的队伍，已重新创建: {}", team.name)
                TeamSyncManager.notifyTeamAdded(teamInfo)
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
                    TeamSyncManager.notifyPlayerJoinedTeam(team.name, playerInfo)
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
                TeamSyncManager.notifyPlayerLeftTeam(team.name, playerName)
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
            updateAndNotify(player) { info ->
                info.health = player.health.toDouble()
                info.maxHealth = player.maxHealth.toDouble()
            }
        })

        // 游戏模式变化
        PlayerDataCallback.GAMEMODE_CHANGED.register(PlayerDataCallback.GamemodeChanged { player, _, newMode ->
            updateAndNotify(player) { info ->
                info.gamemode = newMode.getName()
            }
            LOGGER.debug("玩家 {} 游戏模式变更为 {}", player.gameProfile.name, newMode.getName())
        })

        // 维度变化
        PlayerDataCallback.DIMENSION_CHANGED.register(PlayerDataCallback.DimensionChanged { player ->
            updateAndNotify(player) { info ->
                info.dimension = player.level().dimension().identifier().toString()
            }
            LOGGER.debug("玩家 {} 维度变更为 {}", player.gameProfile.name, player.level().dimension())
        })

        // 玩家死亡
        PlayerDataCallback.PLAYER_DIED.register(PlayerDataCallback.PlayerDied { player ->
            updateAndNotify(player) { info ->
                info.isAlive = false
                info.health = 0.0
            }
            LOGGER.debug("玩家 {} 已死亡", player.gameProfile.name)
        })

        // 玩家重生
        PlayerDataCallback.PLAYER_RESPAWNED.register(PlayerDataCallback.PlayerRespawned { player ->
            // 重生时做全量刷新，因为重生会创建新的 ServerPlayer 实例
            val result = refreshSinglePlayer(player)
            if (result != null) {
                TeamSyncManager.notifyPlayerDataUpdated(result.first, result.second)
            }
            LOGGER.debug("玩家 {} 已重生", player.gameProfile.name)
        })

        // 药水效果变化
        PlayerDataCallback.EFFECT_CHANGED.register(PlayerDataCallback.EffectChanged { player ->
            updateAndNotify(player) { info ->
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
        PlayerDataCallback.EQUIPMENT_CHANGED.register(PlayerDataCallback.EquipmentChanged { player, slot, stack ->
            updateAndNotify(player) { info ->
                info.armor = player.armorValue
                val itemId = if (stack.isEmpty) null else BuiltInRegistries.ITEM.getKey(stack.item).toString()
                when (slot) {
                    EquipmentSlot.HEAD -> info.equipment.helmet = itemId
                    EquipmentSlot.CHEST -> info.equipment.chestplate = itemId
                    EquipmentSlot.LEGS -> info.equipment.leggings = itemId
                    EquipmentSlot.FEET -> info.equipment.boots = itemId
                    EquipmentSlot.MAINHAND -> {
                        info.equipment.mainHand = itemId
                        info.equipment.mainHandCD = getCooldownString(player, stack)
                    }
                    EquipmentSlot.OFFHAND -> {
                        info.equipment.offHand = itemId
                        info.equipment.offHandCD = getCooldownString(player, stack)
                    }
                    else -> {}
                }
            }
        })

        // 物品冷却变化
        PlayerDataCallback.COOLDOWN_CHANGED.register(PlayerDataCallback.CooldownChanged { player, group, duration ->
            updateAndNotify(player) { info ->
                val cooldowns = player.cooldowns
                val mainStack = player.getItemBySlot(EquipmentSlot.MAINHAND)
                if (!mainStack.isEmpty && cooldowns.getCooldownGroup(mainStack) == group) {
                    info.equipment.mainHandCD = if (duration > 0) "${duration / 20.0}s" else null
                }
                val offStack = player.getItemBySlot(EquipmentSlot.OFFHAND)
                if (!offStack.isEmpty && cooldowns.getCooldownGroup(offStack) == group) {
                    info.equipment.offHandCD = if (duration > 0) "${duration / 20.0}s" else null
                }
            }
        })

        // 玩家数据加载完成 — 全量刷新该玩家在队伍中的数据
        // 使用 PLAYER_DATA_LOADED 而非 PLAYER_JOINED_SERVER，确保装备、效果等数据已从存档反序列化完毕
        PlayerDataCallback.PLAYER_DATA_LOADED.register(PlayerDataCallback.PlayerDataLoaded { player ->
            val result = refreshSinglePlayer(player)
            if (result != null) {
                TeamSyncManager.notifyPlayerJoinedServer(result.first, result.second)
            }
            LOGGER.info("玩家 {} 数据加载完成，已刷新队伍数据", player.gameProfile.name)
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
            TeamSyncManager.notifyPlayerLeftServer(playerName)
            LOGGER.info("玩家 {} 离开服务器，已标记为离线", playerName)
        })
    }

    // ──────────────── 细粒度更新工具方法 ────────────────

    /**
     * 更新玩家字段并通知订阅者
     * @param player 在线玩家
     * @param updater 对 PlayerInfo 执行的更新操作
     */
    private inline fun updateAndNotify(player: ServerPlayer, updater: (PlayerInfo) -> Unit) {
        val playerName = player.gameProfile.name
        for (teamInfo in teams.values) {
            val playerInfo = teamInfo.players.find { it.name == playerName }
            if (playerInfo != null) {
                updater(playerInfo)
                TeamSyncManager.notifyPlayerDataUpdated(teamInfo.name, playerInfo)
                return
            }
        }
    }

    /**
     * 根据玩家名定位其在队伍中的 PlayerInfo 并执行更新操作（不推送通知）
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
     * @return Pair(teamName, updatedPlayerInfo) 如果玩家在某个队伍中；否则 null
     */
    private fun refreshSinglePlayer(player: ServerPlayer): Pair<String, PlayerInfo>? {
        val playerName = player.gameProfile.name
        for (teamInfo in teams.values) {
            val idx = teamInfo.players.indexOfFirst { it.name == playerName }
            if (idx >= 0) {
                val newInfo = createPlayerInfoFromOnline(player)
                teamInfo.players[idx] = newInfo
                return teamInfo.name to newInfo
            }
        }
        return null
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
            avatar = AvatarResolver.resolve(player.gameProfile),
            gamemode = player.gameMode.gameModeForPlayer.getName(),
            health = player.health.toDouble(),
            maxHealth = player.maxHealth.toDouble(),
            armor = player.armorValue,
            isAlive = player.isAlive,
            dimension = player.level().dimension().identifier().toString(),
            equipment = extractEquipment(player),
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
            avatar = AvatarResolver.resolveOffline(playerName),
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

    /**
     * 从在线玩家提取装备信息
     */
    private fun extractEquipment(player: ServerPlayer): Equipment {
        val mainHandStack = player.getItemBySlot(EquipmentSlot.MAINHAND)
        val offHandStack = player.getItemBySlot(EquipmentSlot.OFFHAND)
        return Equipment(
            helmet = getItemId(player, EquipmentSlot.HEAD),
            chestplate = getItemId(player, EquipmentSlot.CHEST),
            leggings = getItemId(player, EquipmentSlot.LEGS),
            boots = getItemId(player, EquipmentSlot.FEET),
            mainHand = getItemId(player, EquipmentSlot.MAINHAND),
            mainHandCD = getCooldownString(player, mainHandStack),
            offHand = getItemId(player, EquipmentSlot.OFFHAND),
            offHandCD = getCooldownString(player, offHandStack)
        )
    }

    /**
     * 获取指定装备槽位的物品标识符，空物品返回 null
     */
    private fun getItemId(player: ServerPlayer, slot: EquipmentSlot): String? {
        val stack = player.getItemBySlot(slot)
        return if (stack.isEmpty) null else BuiltInRegistries.ITEM.getKey(stack.item).toString()
    }

    /**
     * 获取物品的冷却时间字符串，无冷却或空物品返回 null
     */
    private fun getCooldownString(player: ServerPlayer, stack: ItemStack): String? {
        if (stack.isEmpty) return null
        val cooldowns = player.cooldowns
        return if (cooldowns.isOnCooldown(stack)) {
            val remaining = cooldowns.getCooldownPercent(stack, 0f)
            "${String.format("%.1f", remaining * 100)}%"
        } else null
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

