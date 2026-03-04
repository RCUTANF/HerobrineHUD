package com.RCUTANF.herobrinehud.client

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
        LOGGER.info("HerobrineHUD 客户端初始化...")

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
                LOGGER.info("已发送 HUD 数据订阅请求")
            }
        }

        // 断开连接时清空客户端缓存
        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            ClientTeamData.clear()
        }

        LOGGER.info("HerobrineHUD 客户端初始化完成！")
    }
}
