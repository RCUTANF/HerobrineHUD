package com.RCUTANF.herobrinehud.client.util

import com.RCUTANF.herobrinehud.client.ClientTeamData

/**
 * 旁观状态追踪器
 *
 * 维护当前客户端正在旁观的玩家UUID，
 * 通过 Mixin 注入更新，避免每帧查询开销。
 * 
 * 注意：此类现在作为 ClientTeamData 的代理，所有状态统一存储在 ClientTeamData 中。
 */
object SpectatorTracker {

    /**
     * 更新当前旁观的玩家UUID
     *
     * @param uuid 玩家UUID字符串，null表示未旁观玩家
     */
    fun updateSpectatingPlayer(uuid: String?) {
        ClientTeamData.setSpectatingPlayer(uuid)
    }

    /**
     * 检查是否正在旁观指定玩家
     *
     * @param playerUuid 要检查的玩家UUID
     * @return true 如果正在旁观该玩家
     */
    fun isSpectating(playerUuid: String): Boolean {
        return ClientTeamData.isSpectating(playerUuid)
    }

    /**
     * 清空旁观状态（断开连接时调用）
     */
    fun clear() {
        ClientTeamData.setSpectatingPlayer(null)
    }
}