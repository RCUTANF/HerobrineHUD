package com.RCUTANF.herobrinehud

import com.RCUTANF.herobrinehud.network.*
import com.RCUTANF.herobrinehud.collector.PlayerDataCallback
import com.RCUTANF.herobrinehud.collector.TeamManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory

class Herobrinehud : ModInitializer {

    companion object {
        const val MOD_ID = "herobrinehud"
        val LOGGER = LoggerFactory.getLogger(MOD_ID)
    }

    override fun onInitialize() {
        LOGGER.info("HerobrineHUD 正在初始化...")

        // ──────────────── 注册网络 Payload 类型 ────────────────
        registerPayloads()

        // ──────────────── 服务器生命周期 ────────────────

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            TeamManager.onServerStarted(server)
        }

        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            TeamManager.onServerStopping(server)
            TeamSyncManager.onServerStopping()
        }

        // ──────────────── Fabric API 事件 ────────────────

        // 玩家加入服务器
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            PlayerDataCallback.PLAYER_JOINED_SERVER.invoker().onPlayerJoinedServer(handler.player)
        }

        // 玩家离开服务器
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            PlayerDataCallback.PLAYER_LEFT_SERVER.invoker().onPlayerLeftServer(handler.player)
            // 清理该玩家的订阅
            TeamSyncManager.onPlayerDisconnect(handler.player)
        }

        // 玩家死亡
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            if (entity is ServerPlayer) {
                PlayerDataCallback.PLAYER_DIED.invoker().onPlayerDied(entity)
            }
        }

        // ──────────────── C2S 网络接收器 ────────────────
        registerC2SReceivers()

        LOGGER.info("HerobrineHUD 初始化完成！")
    }

    /**
     * 注册所有自定义 Payload 类型
     */
    private fun registerPayloads() {
        // S2C payloads（服务端发给客户端）
        PayloadTypeRegistry.playS2C().register(HudPayloadIds.FULL_SYNC, FullSyncPayload.STREAM_CODEC)
        PayloadTypeRegistry.playS2C().register(HudPayloadIds.INCREMENTAL_UPDATE, IncrementalUpdatePayload.STREAM_CODEC)

        // C2S payloads（客户端发给服务端）
        PayloadTypeRegistry.playC2S().register(HudPayloadIds.SUBSCRIBE, SubscribePayload.STREAM_CODEC)
        PayloadTypeRegistry.playC2S().register(HudPayloadIds.UNSUBSCRIBE, UnsubscribePayload.STREAM_CODEC)

        PayloadTypeRegistry.playC2S().register(HudPayloadIds.SPECTATE_PLAYER, SpectatePlayerPayload.STREAM_CODEC)
    }

    /**
     * 注册 C2S 网络接收器（处理客户端的订阅/取消订阅请求）
     */
    private fun registerC2SReceivers() {
        // 订阅请求
        ServerPlayNetworking.registerGlobalReceiver(HudPayloadIds.SUBSCRIBE) { _, context ->
            TeamSyncManager.subscribe(context.player())
        }

        // 取消订阅请求
        ServerPlayNetworking.registerGlobalReceiver(HudPayloadIds.UNSUBSCRIBE) { _, context ->
            TeamSyncManager.unsubscribe(context.player())
        }

        // 旁观玩家请求
        ServerPlayNetworking.registerGlobalReceiver(HudPayloadIds.SPECTATE_PLAYER) { payload, context ->
            val requester = context.player()
            val targetUuid = payload.targetPlayerUuid

            // 在服务器主线程执行
            context.server().execute {
                val targetPlayer = context.server().playerList.getPlayer(targetUuid)

                if (targetPlayer == null) {
                    LOGGER.warn("玩家 {} 请求旁观不存在的玩家: {}", requester.name.string, targetUuid)
                    return@execute
                }

                // 检查请求者是否处于旁观模式
                if (!requester.isSpectator) {
                    LOGGER.warn("玩家 {} 不在旁观模式，无法切换旁观目标", requester.name.string)
                    return@execute
                }

                // 设置旁观目标
                requester.setCamera(targetPlayer)
                LOGGER.info("玩家 {} 开始旁观 {}", requester.name.string, targetPlayer.name.string)
            }
        }
    }
}
