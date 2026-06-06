package com.RCUTANF.herobrinehud

import com.RCUTANF.herobrinehud.network.*
import com.RCUTANF.herobrinehud.collector.PlayerDataCallback
import com.RCUTANF.herobrinehud.collector.TeamManager
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory

class Herobrinehud : ModInitializer {

    companion object {
        const val MOD_ID = "herobrinehud"
        val LOGGER = LoggerFactory.getLogger(MOD_ID)
    }

    override fun onInitialize() {
        LOGGER.info("HerobrineHUD initializing...")

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

        LOGGER.info("HerobrineHUD initialization complete!")
    }

    /**
     * 注册所有自定义 Payload 类型
     */
    private fun registerPayloads() {
        HudNetworkingCompat.registerPayloads()
    }

    /**
     * 注册 C2S 网络接收器（处理客户端的订阅/取消订阅请求）
     */
    private fun registerC2SReceivers() {
        HudNetworkingCompat.registerSubscribeReceiver { player ->
            TeamSyncManager.subscribe(player)
        }

        HudNetworkingCompat.registerUnsubscribeReceiver { player ->
            TeamSyncManager.unsubscribe(player)
        }

        HudNetworkingCompat.registerSpectateReceiver { payload, requester, server ->
            val targetUuid = payload.targetPlayerUuid

            server.execute {
                val targetPlayer = server.playerList.getPlayer(targetUuid)

                if (targetPlayer == null) {
                    LOGGER.warn("Player {} requested to spectate non-existent player: {}", requester.name.string, targetUuid)
                    return@execute
                }

                if (!requester.isSpectator) {
                    LOGGER.warn("Player {} is not in spectator mode and cannot change spectate target", requester.name.string)
                    return@execute
                }

                requester.setCamera(targetPlayer)
                LOGGER.info("Player {} started spectating {}", requester.name.string, targetPlayer.name.string)
            }
        }
    }
}
