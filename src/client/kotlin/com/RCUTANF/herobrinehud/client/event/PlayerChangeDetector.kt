package com.RCUTANF.herobrinehud.client.event

import com.RCUTANF.herobrinehud.data.PlayerInfo

/**
 * 玩家数据变化检测器
 * 负责比对新旧数据并生成变化事件
 */
object PlayerChangeDetector {
    
    /**
     * 检测玩家数据的所有变化
     * 
     * @param oldPlayer 旧的玩家数据（可为null，表示新玩家）
     * @param newPlayer 新的玩家数据
     * @return 变化事件列表
     */
    fun detectChanges(oldPlayer: PlayerInfo?, newPlayer: PlayerInfo): List<PlayerChangeEvent> {
        if (oldPlayer == null) {
            // 新玩家，不生成变化事件
            return emptyList()
        }
        
        val events = mutableListOf<PlayerChangeEvent>()
        
        // 检测血量变化
        if (oldPlayer.health != newPlayer.health) {
            events.add(PlayerChangeEvent(
                playerUuid = newPlayer.uuid,
                changeType = PlayerChangeType.HEALTH,
                oldValue = oldPlayer.health,
                newValue = newPlayer.health
            ))
        }
        
        // 检测最大血量变化
        if (oldPlayer.maxHealth != newPlayer.maxHealth) {
            events.add(PlayerChangeEvent(
                playerUuid = newPlayer.uuid,
                changeType = PlayerChangeType.MAX_HEALTH,
                oldValue = oldPlayer.maxHealth,
                newValue = newPlayer.maxHealth
            ))
        }
        
        // 检测护甲变化
        if (oldPlayer.armor != newPlayer.armor) {
            events.add(PlayerChangeEvent(
                playerUuid = newPlayer.uuid,
                changeType = PlayerChangeType.ARMOR,
                oldValue = oldPlayer.armor,
                newValue = newPlayer.armor
            ))
        }
        
        // 检测游戏模式变化
        if (oldPlayer.gamemode != newPlayer.gamemode) {
            events.add(PlayerChangeEvent(
                playerUuid = newPlayer.uuid,
                changeType = PlayerChangeType.GAMEMODE,
                oldValue = oldPlayer.gamemode,
                newValue = newPlayer.gamemode
            ))
        }
        
        // 检测维度变化
        if (oldPlayer.dimension != newPlayer.dimension) {
            events.add(PlayerChangeEvent(
                playerUuid = newPlayer.uuid,
                changeType = PlayerChangeType.DIMENSION,
                oldValue = oldPlayer.dimension,
                newValue = newPlayer.dimension
            ))
        }
        
        // 检测存活状态变化
        if (oldPlayer.isAlive != newPlayer.isAlive) {
            events.add(PlayerChangeEvent(
                playerUuid = newPlayer.uuid,
                changeType = PlayerChangeType.ALIVE_STATUS,
                oldValue = oldPlayer.isAlive,
                newValue = newPlayer.isAlive
            ))
        }
        
        // TODO: 后续可扩展效果和装备变化检测
        
        return events
    }
}
