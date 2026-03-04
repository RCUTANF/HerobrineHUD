package com.RCUTANF.herobrinehud.network

import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.RCUTANF.herobrinehud.data.TeamInfo
import com.RCUTANF.herobrinehud.team.TeamManager
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 队伍数据网络同步管理器（服务端）
 *
 * 职责：
 * 1. 维护订阅者列表
 * 2. 新订阅者立即推送全量数据
 * 3. 数据变更时向所有订阅者推送增量更新
 */
object TeamSyncManager {

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/TeamSyncManager")

    /** JSON 序列化配置 */
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /** 订阅者集合：以 UUID 为 key 存储 ServerPlayer 引用 */
    private val subscribers = ConcurrentHashMap<String, ServerPlayer>()

    // ──────────────── 订阅管理 ────────────────

    /**
     * 添加订阅者，并立即推送全量数据
     */
    fun subscribe(player: ServerPlayer) {
        val uuid = player.uuid.toString()
        subscribers[uuid] = player
        LOGGER.info("玩家 {} 已订阅 HUD 数据推送", player.gameProfile.name)

        // 立即推送全量数据
        sendFullSync(player)
    }

    /**
     * 移除订阅者
     */
    fun unsubscribe(player: ServerPlayer) {
        val uuid = player.uuid.toString()
        val removed = subscribers.remove(uuid)
        if (removed != null) {
            LOGGER.info("玩家 {} 已取消订阅 HUD 数据推送", player.gameProfile.name)
        }
    }

    /**
     * 玩家断开连接时清理订阅
     */
    fun onPlayerDisconnect(player: ServerPlayer) {
        subscribers.remove(player.uuid.toString())
    }

    /**
     * 服务器关闭时清空所有订阅
     */
    fun onServerStopping() {
        subscribers.clear()
    }

    /**
     * 检查玩家是否已订阅
     */
    fun isSubscribed(player: ServerPlayer): Boolean {
        return subscribers.containsKey(player.uuid.toString())
    }

    // ──────────────── 全量同步 ────────────────

    /**
     * 向指定玩家发送全量队伍数据
     */
    fun sendFullSync(player: ServerPlayer) {
        val allTeams = TeamManager.getAllTeams()
        val data = FullSyncData(teams = allTeams)
        val jsonStr = json.encodeToString(data)
        val payload = FullSyncPayload(jsonStr)

        if (ServerPlayNetworking.canSend(player, HudPayloadIds.FULL_SYNC)) {
            ServerPlayNetworking.send(player, payload)
            LOGGER.debug("已向 {} 发送全量同步（{} 支队伍）", player.gameProfile.name, allTeams.size)
        } else {
            LOGGER.warn("无法向 {} 发送全量同步（通道未就绪）", player.gameProfile.name)
        }
    }

    // ──────────────── 增量推送 ────────────────

    /**
     * 向所有订阅者广播增量更新
     */
    private fun broadcastIncremental(updateType: String, jsonStr: String) {
        if (subscribers.isEmpty()) return

        val payload = IncrementalUpdatePayload(updateType, jsonStr)
        val toRemove = mutableListOf<String>()

        for ((uuid, player) in subscribers) {
            try {
                if (ServerPlayNetworking.canSend(player, HudPayloadIds.INCREMENTAL_UPDATE)) {
                    ServerPlayNetworking.send(player, payload)
                } else {
                    // 无法发送，可能已断线但尚未清理
                    toRemove.add(uuid)
                }
            } catch (e: Exception) {
                LOGGER.warn("向 {} 发送增量更新失败: {}", player.gameProfile.name, e.message)
                toRemove.add(uuid)
            }
        }

        // 清理无效订阅者
        toRemove.forEach { subscribers.remove(it) }
    }

    // ──────────────── 队伍变更推送 ────────────────

    /** 队伍新增 */
    fun notifyTeamAdded(team: TeamInfo) {
        val data = TeamUpdateData(team = team)
        broadcastIncremental(UpdateType.TEAM_ADDED, json.encodeToString(data))
    }

    /** 队伍移除 */
    fun notifyTeamRemoved(teamName: String) {
        val data = TeamRemovedData(teamName = teamName)
        broadcastIncremental(UpdateType.TEAM_REMOVED, json.encodeToString(data))
    }

    /** 队伍修改 */
    fun notifyTeamModified(team: TeamInfo) {
        val data = TeamUpdateData(team = team)
        broadcastIncremental(UpdateType.TEAM_MODIFIED, json.encodeToString(data))
    }

    // ──────────────── 队伍成员变更推送 ────────────────

    /** 玩家加入队伍 */
    fun notifyPlayerJoinedTeam(teamName: String, player: PlayerInfo) {
        val data = PlayerJoinedTeamData(teamName = teamName, player = player)
        broadcastIncremental(UpdateType.PLAYER_JOINED_TEAM, json.encodeToString(data))
    }

    /** 玩家离开队伍 */
    fun notifyPlayerLeftTeam(teamName: String, playerName: String) {
        val data = PlayerLeftTeamData(teamName = teamName, playerName = playerName)
        broadcastIncremental(UpdateType.PLAYER_LEFT_TEAM, json.encodeToString(data))
    }

    // ──────────────── 玩家数据变更推送 ────────────────

    /** 玩家数据更新（通用：血量、护甲、游戏模式、维度、效果、装备等任何字段变化） */
    fun notifyPlayerDataUpdated(teamName: String, player: PlayerInfo) {
        val data = PlayerDataUpdatedData(teamName = teamName, player = player)
        broadcastIncremental(UpdateType.PLAYER_DATA_UPDATED, json.encodeToString(data))
    }

    /** 玩家加入服务器 */
    fun notifyPlayerJoinedServer(teamName: String, player: PlayerInfo) {
        val data = PlayerDataUpdatedData(teamName = teamName, player = player)
        broadcastIncremental(UpdateType.PLAYER_JOINED_SERVER, json.encodeToString(data))
    }

    /** 玩家离开服务器 */
    fun notifyPlayerLeftServer(playerName: String) {
        val data = PlayerLeftServerData(playerName = playerName)
        broadcastIncremental(UpdateType.PLAYER_LEFT_SERVER, json.encodeToString(data))
    }
}

