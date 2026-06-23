package com.RCUTANF.herobrinehud.client

import com.RCUTANF.herobrinehud.client.hud.BuiltInHudProviders
import com.RCUTANF.herobrinehud.client.util.AvatarTextureCache
import com.RCUTANF.herobrinehud.client.util.SpectatorTracker
import com.RCUTANF.herobrinehud.network.FullSyncPayload
import com.RCUTANF.herobrinehud.network.HudPayloadIds
import com.RCUTANF.herobrinehud.network.IncrementalUpdatePayload
import com.RCUTANF.herobrinehud.network.SubscribePayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import org.slf4j.LoggerFactory

class HerobrinehudClient : ClientModInitializer {

    companion object {
        private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/Client")
    }

    override fun onInitializeClient() {
        LOGGER.info("HerobrineHUD client initializing...")

        // 加载持久化配置
        HudConfig.load()

        // ──────────── 注册 HUD Provider 中心 ────────────
        BuiltInHudProviders.registerAll()

        // ──────────── 注册快捷键 ────────────
        ModKeybindings.register()

        // ──────────── 注册 S2C 接收器 ────────────

        // 全量同步
        ClientPlayNetworking.registerGlobalReceiver(HudPayloadIds.FULL_SYNC) { payload: FullSyncPayload, _ ->
            ClientTeamData.handleFullSync(payload.json)
        }

        // 增量更新
        ClientPlayNetworking.registerGlobalReceiver(HudPayloadIds.INCREMENTAL_UPDATE) { payload: IncrementalUpdatePayload, _ ->
            ClientTeamData.handleIncrementalUpdate(payload.updateType, payload.json)
        }

        // ──────────── 连接事件 ────────────

        // 加入服务器后自动发送订阅请求
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
                if (ClientPlayNetworking.canSend(HudPayloadIds.SUBSCRIBE)) {
                ClientPlayNetworking.send(SubscribePayload())
                LOGGER.info("Sent HUD subscription request")
            }
        }

        // 断开连接时清空客户端缓存
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            ClientTeamData.clear()
            AvatarTextureCache.clear()
            SpectatorTracker.clear()
        }

        LOGGER.info("HerobrineHUD client initialization complete!")
    }
}
