package com.RCUTANF.herobrinehud.client

/**
 * 旁观状态追踪器
 * 
 * 维护当前客户端正在旁观的玩家UUID，
 * 通过 Mixin 注入更新，避免每帧查询开销。
 */
object SpectatorTracker {
    
    /**
     * 当前正在旁观的玩家UUID（字符串格式）
     * null 表示未旁观任何玩家或旁观的不是玩家实体
     */
    @Volatile
    var currentSpectatingPlayerUuid: String? = null
        private set
    
    /**
     * 更新当前旁观的玩家UUID
     * 
     * @param uuid 玩家UUID字符串，null表示未旁观玩家
     */
    fun updateSpectatingPlayer(uuid: String?) {
        currentSpectatingPlayerUuid = uuid
    }
    
    /**
     * 检查是否正在旁观指定玩家
     * 
     * @param playerUuid 要检查的玩家UUID
     * @return true 如果正在旁观该玩家
     */
    fun isSpectating(playerUuid: String): Boolean {
        return currentSpectatingPlayerUuid == playerUuid
    }
    
    /**
     * 清空旁观状态（断开连接时调用）
     */
    fun clear() {
        currentSpectatingPlayerUuid = null
    }
}
