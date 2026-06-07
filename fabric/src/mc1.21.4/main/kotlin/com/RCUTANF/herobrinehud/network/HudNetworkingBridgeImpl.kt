package com.RCUTANF.herobrinehud.network

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

internal class HudNetworkingBridgeImpl : HudNetworkingBridge {
    override fun registerPayloads() {
        PayloadTypeRegistry.playS2C().register(HudPayloadIds.FULL_SYNC, FullSyncPayload.STREAM_CODEC)
        PayloadTypeRegistry.playS2C().register(HudPayloadIds.INCREMENTAL_UPDATE, IncrementalUpdatePayload.STREAM_CODEC)
        PayloadTypeRegistry.playC2S().register(HudPayloadIds.SUBSCRIBE, SubscribePayload.STREAM_CODEC)
        PayloadTypeRegistry.playC2S().register(HudPayloadIds.UNSUBSCRIBE, UnsubscribePayload.STREAM_CODEC)
        PayloadTypeRegistry.playC2S().register(HudPayloadIds.SPECTATE_PLAYER, SpectatePlayerPayload.STREAM_CODEC)
    }

    override fun registerSubscribeReceiver(handler: (ServerPlayer) -> Unit) {
        ServerPlayNetworking.registerGlobalReceiver(HudPayloadIds.SUBSCRIBE) { _, context ->
            handler(context.player())
        }
    }

    override fun registerUnsubscribeReceiver(handler: (ServerPlayer) -> Unit) {
        ServerPlayNetworking.registerGlobalReceiver(HudPayloadIds.UNSUBSCRIBE) { _, context ->
            handler(context.player())
        }
    }

    override fun registerSpectateReceiver(handler: (SpectatePlayerPayload, ServerPlayer, MinecraftServer) -> Unit) {
        ServerPlayNetworking.registerGlobalReceiver(HudPayloadIds.SPECTATE_PLAYER) { payload, context ->
            handler(payload, context.player(), context.server())
        }
    }

    override fun canSendFullSync(player: ServerPlayer): Boolean {
        return ServerPlayNetworking.canSend(player, HudPayloadIds.FULL_SYNC)
    }

    override fun sendFullSync(player: ServerPlayer, payload: FullSyncPayload) {
        ServerPlayNetworking.send(player, payload)
    }

    override fun canSendIncrementalUpdate(player: ServerPlayer): Boolean {
        return ServerPlayNetworking.canSend(player, HudPayloadIds.INCREMENTAL_UPDATE)
    }

    override fun sendIncrementalUpdate(player: ServerPlayer, payload: IncrementalUpdatePayload) {
        ServerPlayNetworking.send(player, payload)
    }
}
