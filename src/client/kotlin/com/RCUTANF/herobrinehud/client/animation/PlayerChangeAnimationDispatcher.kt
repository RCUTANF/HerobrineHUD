package com.RCUTANF.herobrinehud.client.animation

import com.RCUTANF.herobrinehud.client.event.PlayerChangeEvent
import com.RCUTANF.herobrinehud.client.event.PlayerChangeType

/**
 * 玩家变化动画分发器
 * 根据变化事件创建对应的动画
 */
object PlayerChangeAnimationDispatcher {
    
    /**
     * 处理变化事件并创建动画
     */
    fun dispatchChangeEvents(events: List<PlayerChangeEvent>) {
        events.forEach { event ->
            val animation = createAnimation(event)
            if (animation != null) {
                PlayerAnimationManager.addAnimation(animation)
            }
        }
    }
    
    /**
     * 根据变化事件创建动画
     */
    private fun createAnimation(event: PlayerChangeEvent): PlayerAnimation? {
        return when (event.changeType) {
            PlayerChangeType.HEALTH -> createHealthAnimation(event)
            // TODO: 后续可扩展其他类型动画
            // PlayerChangeType.ARMOR -> createArmorAnimation(event)
            // PlayerChangeType.EFFECT_ADDED -> createEffectAddedAnimation(event)
            else -> null
        }
    }
    
    /**
     * 创建血量变化动画
     */
    private fun createHealthAnimation(event: PlayerChangeEvent): PlayerAnimation? {
        // 检查变化是否显著（> 1.0）
        if (!event.isSignificant(threshold = 1.0)) {
            return null
        }
        
        val change = event.getNumericChange() ?: return null
        
        return NumericChangeAnimation(
            playerUuid = event.playerUuid,
            changeAmount = change,
            color = 0xFFFFFF,  // 白色
            maxOffset = 25,
            duration = 1500
        )
    }
}
