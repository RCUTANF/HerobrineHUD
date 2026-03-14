package com.RCUTANF.herobrinehud.client.animation

import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家动画管理器
 * 统一管理所有玩家的动画实例
 */
object PlayerAnimationManager {
    
    private val animations = ConcurrentHashMap<String, MutableList<PlayerAnimation>>()
    
    /**
     * 添加动画
     */
    fun addAnimation(animation: PlayerAnimation) {
        animations.computeIfAbsent(animation.playerUuid) { 
            mutableListOf() 
        }.add(animation)
    }
    
    /**
     * 获取玩家的所有活跃动画
     */
    fun getAnimations(playerUuid: String): List<PlayerAnimation> {
        val playerAnimations = animations[playerUuid] ?: return emptyList()
        
        // 清理已结束的动画
        playerAnimations.removeIf { it.isFinished() }
        
        return playerAnimations.toList()
    }
    
    /**
     * 清理所有过期动画
     */
    fun cleanupExpiredAnimations() {
        animations.values.forEach { list ->
            list.removeIf { it.isFinished() }
        }
        // 移除空列表
        animations.entries.removeIf { it.value.isEmpty() }
    }
    
    /**
     * 清空所有动画
     */
    fun clearAll() {
        animations.clear()
    }
    
    /**
     * 清空指定玩家的动画
     */
    fun clearPlayer(playerUuid: String) {
        animations.remove(playerUuid)
    }
}
