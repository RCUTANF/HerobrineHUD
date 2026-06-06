package com.RCUTANF.herobrinehud.network

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

internal interface HudNetworkingBridge {
    fun registerPayloads()
    fun registerSubscribeReceiver(handler: (ServerPlayer) -> Unit)
    fun registerUnsubscribeReceiver(handler: (ServerPlayer) -> Unit)
    fun registerSpectateReceiver(handler: (SpectatePlayerPayload, ServerPlayer, MinecraftServer) -> Unit)
    fun canSendFullSync(player: ServerPlayer): Boolean
    fun sendFullSync(player: ServerPlayer, payload: FullSyncPayload)
    fun canSendIncrementalUpdate(player: ServerPlayer): Boolean
    fun sendIncrementalUpdate(player: ServerPlayer, payload: IncrementalUpdatePayload)
}

internal object HudNetworkingCompat : HudNetworkingBridge by HudNetworkingBridgeImpl()
