package com.RCUTANF.herobrinehud.client.event

import kotlin.math.abs

/**
 * 玩家数据变化事件
 * 
 * @param playerUuid 玩家UUID
 * @param changeType 变化类型
 * @param oldValue 旧值（可为null）
 * @param newValue 新值（可为null）
 * @param metadata 额外元数据（可选）
 */
data class PlayerChangeEvent(
    val playerUuid: String,
    val changeType: PlayerChangeType,
    val oldValue: Any?,
    val newValue: Any?,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 计算数值变化量（仅适用于数值类型）
     */
    fun getNumericChange(): Double? {
        return when {
            oldValue is Number && newValue is Number -> 
                newValue.toDouble() - oldValue.toDouble()
            else -> null
        }
    }
    
    /**
     * 检查变化是否显著（用于过滤小变化）
     */
    fun isSignificant(threshold: Double = 1.0): Boolean {
        val change = getNumericChange() ?: return true
        return abs(change) > threshold
    }
}
