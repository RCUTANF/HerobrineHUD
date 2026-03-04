package com.RCUTANF.herobrinehud

import com.RCUTANF.herobrinehud.team.PlayerDataCallback
import com.RCUTANF.herobrinehud.team.TeamManager
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
        LOGGER.info("HerobrineHUD 正在初始化...")

        // 服务器启动完毕后，全量同步队伍数据
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            TeamManager.onServerStarted(server)
        }

        // 服务器关闭前，清理数据
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            TeamManager.onServerStopping(server)
        }

        // ──────────────── Fabric API 事件 ────────────────

        // 玩家加入服务器
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            PlayerDataCallback.PLAYER_JOINED_SERVER.invoker().onPlayerJoinedServer(handler.player)
        }

        // 玩家离开服务器
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            PlayerDataCallback.PLAYER_LEFT_SERVER.invoker().onPlayerLeftServer(handler.player)
        }

        // 玩家死亡（通过 Fabric API 的生物死亡事件，过滤 ServerPlayer）
        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            if (entity is ServerPlayer) {
                PlayerDataCallback.PLAYER_DIED.invoker().onPlayerDied(entity)
            }
        }

        LOGGER.info("HerobrineHUD 初始化完成！")
    }
}
