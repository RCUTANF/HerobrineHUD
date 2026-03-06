package com.RCUTANF.herobrinehud.network

import com.RCUTANF.herobrinehud.Herobrinehud
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import java.util.*

/**
 * C2S: 客户端请求旁观指定玩家
 * @param targetPlayerUuid 目标玩家的 UUID
 */
data class SpectatePlayerPayload(val targetPlayerUuid: UUID) : CustomPacketPayload {

    /**
     * 从 FriendlyByteBuf 读取数据的构造函数
     */
    constructor(buf: FriendlyByteBuf) : this(buf.readUUID())

    /**
     * 将数据写入 FriendlyByteBuf
     */
    fun write(buf: FriendlyByteBuf) {
        buf.writeUUID(targetPlayerUuid)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<SpectatePlayerPayload> =
            CustomPacketPayload.Type(Identifier.fromNamespaceAndPath(Herobrinehud.MOD_ID, "spectate_player"))

        /**
         * StreamCodec 使用 lambda 表达式而不是方法引用
         */
        val STREAM_CODEC: StreamCodec<FriendlyByteBuf, SpectatePlayerPayload> =
            CustomPacketPayload.codec(
                { payload, buf -> payload.write(buf) },
                { buf -> SpectatePlayerPayload(buf) }
            )
    }
}