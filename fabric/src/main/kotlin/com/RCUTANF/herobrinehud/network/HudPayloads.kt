package com.RCUTANF.herobrinehud.network

import com.RCUTANF.herobrinehud.Herobrinehud
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier

// ══════════════════════════════════════════════════════════════
//  Payload ID 定义
// ══════════════════════════════════════════════════════════════

object HudPayloadIds {
    /** S2C：全量同步（服务端 → 客户端） */
    val FULL_SYNC: CustomPacketPayload.Type<FullSyncPayload> =
        CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Herobrinehud.MOD_ID, "full_sync"))

    /** S2C：增量更新（服务端 → 客户端） */
    val INCREMENTAL_UPDATE: CustomPacketPayload.Type<IncrementalUpdatePayload> =
        CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Herobrinehud.MOD_ID, "incremental_update"))

    /** C2S：客户端请求订阅 */
    val SUBSCRIBE: CustomPacketPayload.Type<SubscribePayload> =
        CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Herobrinehud.MOD_ID, "subscribe"))

    /** C2S：客户端请求取消订阅 */
    val UNSUBSCRIBE: CustomPacketPayload.Type<UnsubscribePayload> =
        CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Herobrinehud.MOD_ID, "unsubscribe"))

    /** C2S：客户端请求旁观玩家 */
    val SPECTATE_PLAYER: CustomPacketPayload.Type<SpectatePlayerPayload> =
        CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Herobrinehud.MOD_ID, "spectate_player"))
}

// ══════════════════════════════════════════════════════════════
//  S2C Payloads
// ══════════════════════════════════════════════════════════════

/**
 * 全量同步：将所有队伍数据的 JSON 序列化后发送给客户端
 */
data class FullSyncPayload(val json: String) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = HudPayloadIds.FULL_SYNC

    companion object {
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, FullSyncPayload> = StreamCodec.of(
            { buf, payload -> buf.writeUtf(payload.json, MAX_JSON_LENGTH) },
            { buf -> FullSyncPayload(buf.readUtf(MAX_JSON_LENGTH)) }
        )
        private const val MAX_JSON_LENGTH = 1_048_576 // 1 MB
    }
}

/**
 * 增量更新：发送变更类型 + 受影响的数据 JSON
 *
 * updateType 枚举值见 [UpdateType]
 */
data class IncrementalUpdatePayload(
    val updateType: String,
    val json: String
) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = HudPayloadIds.INCREMENTAL_UPDATE

    companion object {
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, IncrementalUpdatePayload> = StreamCodec.of(
            { buf, payload ->
                buf.writeUtf(payload.updateType, 64)
                buf.writeUtf(payload.json, MAX_JSON_LENGTH)
            },
            { buf ->
                val type = buf.readUtf(64)
                val json = buf.readUtf(MAX_JSON_LENGTH)
                IncrementalUpdatePayload(type, json)
            }
        )
        private const val MAX_JSON_LENGTH = 1_048_576
    }
}

// ══════════════════════════════════════════════════════════════
//  C2S Payloads
// ══════════════════════════════════════════════════════════════

/**
 * 客户端请求订阅 HUD 数据推送
 */
class SubscribePayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = HudPayloadIds.SUBSCRIBE

    companion object {
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, SubscribePayload> = StreamCodec.of(
            { _, _ -> /* 无数据 */ },
            { _ -> SubscribePayload() }
        )
    }
}

/**
 * 客户端请求取消订阅
 */
class UnsubscribePayload : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = HudPayloadIds.UNSUBSCRIBE

    companion object {
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, UnsubscribePayload> = StreamCodec.of(
            { _, _ -> /* 无数据 */ },
            { _ -> UnsubscribePayload() }
        )
    }
}

// ══════════════════════════════════════════════════════════════
//  增量更新类型枚举
// ══════════════════════════════════════════════════════════════

/**
 * 增量更新类型标识
 */
object UpdateType {
    // 队伍级别
    const val TEAM_ADDED = "team_added"
    const val TEAM_REMOVED = "team_removed"
    const val TEAM_MODIFIED = "team_modified"

    // 队伍成员
    const val PLAYER_JOINED_TEAM = "player_joined_team"
    const val PLAYER_LEFT_TEAM = "player_left_team"

    // 玩家数据
    const val PLAYER_DATA_UPDATED = "player_data_updated"
    const val PLAYER_JOINED_SERVER = "player_joined_server"
    const val PLAYER_LEFT_SERVER = "player_left_server"
}

