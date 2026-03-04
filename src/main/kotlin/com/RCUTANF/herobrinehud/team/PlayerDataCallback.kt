package com.RCUTANF.herobrinehud.team

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.GameType

/**
 * 玩家数据变更事件回调
 * 用于在玩家数据发生变化时通知监听者，替代 tick 轮询刷新
 */
object PlayerDataCallback {

    // ──────────────── 生命值 ────────────────

    /** 玩家血量变化时触发 */
    val HEALTH_CHANGED: Event<HealthChanged> = EventFactory.createArrayBacked(HealthChanged::class.java) { listeners ->
        HealthChanged { player ->
            listeners.forEach { it.onHealthChanged(player) }
        }
    }

    // ──────────────── 游戏模式 ────────────────

    /** 玩家游戏模式变化时触发 */
    val GAMEMODE_CHANGED: Event<GamemodeChanged> = EventFactory.createArrayBacked(GamemodeChanged::class.java) { listeners ->
        GamemodeChanged { player, oldMode, newMode ->
            listeners.forEach { it.onGamemodeChanged(player, oldMode, newMode) }
        }
    }

    // ──────────────── 维度 ────────────────

    /** 玩家切换维度时触发 */
    val DIMENSION_CHANGED: Event<DimensionChanged> = EventFactory.createArrayBacked(DimensionChanged::class.java) { listeners ->
        DimensionChanged { player ->
            listeners.forEach { it.onDimensionChanged(player) }
        }
    }

    // ──────────────── 生死状态 ────────────────

    /** 玩家死亡时触发 */
    val PLAYER_DIED: Event<PlayerDied> = EventFactory.createArrayBacked(PlayerDied::class.java) { listeners ->
        PlayerDied { player ->
            listeners.forEach { it.onPlayerDied(player) }
        }
    }

    /** 玩家重生时触发 */
    val PLAYER_RESPAWNED: Event<PlayerRespawned> = EventFactory.createArrayBacked(PlayerRespawned::class.java) { listeners ->
        PlayerRespawned { player ->
            listeners.forEach { it.onPlayerRespawned(player) }
        }
    }

    // ──────────────── 药水效果 ────────────────

    /** 玩家药水效果发生变化时触发（添加/移除/清除） */
    val EFFECT_CHANGED: Event<EffectChanged> = EventFactory.createArrayBacked(EffectChanged::class.java) { listeners ->
        EffectChanged { player ->
            listeners.forEach { it.onEffectChanged(player) }
        }
    }

    // ──────────────── 装备 ────────────────

    /** 玩家装备/物品变化时触发（同时影响护甲值） */
    val EQUIPMENT_CHANGED: Event<EquipmentChanged> = EventFactory.createArrayBacked(EquipmentChanged::class.java) { listeners ->
        EquipmentChanged { player ->
            listeners.forEach { it.onEquipmentChanged(player) }
        }
    }

    // ──────────────── 加入/离开服务器 ────────────────

    /** 玩家加入服务器时触发 */
    val PLAYER_JOINED_SERVER: Event<PlayerJoinedServer> = EventFactory.createArrayBacked(PlayerJoinedServer::class.java) { listeners ->
        PlayerJoinedServer { player ->
            listeners.forEach { it.onPlayerJoinedServer(player) }
        }
    }

    /** 玩家离开服务器时触发 */
    val PLAYER_LEFT_SERVER: Event<PlayerLeftServer> = EventFactory.createArrayBacked(PlayerLeftServer::class.java) { listeners ->
        PlayerLeftServer { player ->
            listeners.forEach { it.onPlayerLeftServer(player) }
        }
    }

    // ──────────────── 函数式接口 ────────────────

    fun interface HealthChanged {
        fun onHealthChanged(player: ServerPlayer)
    }

    fun interface GamemodeChanged {
        fun onGamemodeChanged(player: ServerPlayer, oldMode: GameType, newMode: GameType)
    }

    fun interface DimensionChanged {
        fun onDimensionChanged(player: ServerPlayer)
    }

    fun interface PlayerDied {
        fun onPlayerDied(player: ServerPlayer)
    }

    fun interface PlayerRespawned {
        fun onPlayerRespawned(player: ServerPlayer)
    }

    fun interface EffectChanged {
        fun onEffectChanged(player: ServerPlayer)
    }

    fun interface EquipmentChanged {
        fun onEquipmentChanged(player: ServerPlayer)
    }

    fun interface PlayerJoinedServer {
        fun onPlayerJoinedServer(player: ServerPlayer)
    }

    fun interface PlayerLeftServer {
        fun onPlayerLeftServer(player: ServerPlayer)
    }
}

