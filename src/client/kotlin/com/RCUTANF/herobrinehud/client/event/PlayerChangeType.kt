package com.RCUTANF.herobrinehud.client.event

/**
 * 玩家数据变化类型
 */
enum class PlayerChangeType {
    HEALTH,          // 血量变化
    MAX_HEALTH,      // 最大血量变化
    ARMOR,           // 护甲值变化
    GAMEMODE,        // 游戏模式变化
    DIMENSION,       // 维度变化
    ALIVE_STATUS,    // 存活状态变化
    EFFECT_ADDED,    // 效果添加
    EFFECT_REMOVED,  // 效果移除
    EFFECT_UPDATED,  // 效果更新
    EQUIPMENT_CHANGED, // 装备变化
    // 预留扩展...
}
