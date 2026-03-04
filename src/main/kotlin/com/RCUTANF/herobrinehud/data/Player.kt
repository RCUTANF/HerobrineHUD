package com.RCUTANF.herobrinehud.data

import kotlinx.serialization.Serializable


/**
 * 玩家数据模型
 * 表示单个选手在当前比赛中的信息
 */
@Serializable
data class PlayerInfo(
    val uuid: String,                                    // 玩家唯一标识
    val name: String,                                  // 游戏内名称
    val displayName: String,                           // 显示名称
    val avatar: String? = null,                        // 头像URL

    // 游戏状态
    var health: Double = 20.0,                         // 当前生命值
    var maxHealth: Double = 20.0,                      // 最大生命值
    var armor: Int = 0,                                // 护甲值
    var isAlive: Boolean = true,                       // 存活状态
    var dimension: String? = null,                     // 所在维度

    // 装备信息
    var equipment: Equipment = Equipment(),            // 装备详情

    // 效果列表
    val effects: MutableList<PlayerEffect> = mutableListOf(),

    // 自定义数据（灵活扩展）
    val customData: MutableMap<String, Any> = mutableMapOf()
) {

}

/**
 * 装备信息
 * 可复用的装备数据结构
 */
@Serializable
data class Equipment(
    var weapon: String? = null,                        // 当前武器
    var armor: String? = null,                         // 护甲类型
    var items: MutableList<String> = mutableListOf()   // 物品列表
)

@Serializable
data class PlayerEffect(
    var name: String,                                  // 效果名称
    var identifier: String,                            // 效果标识符
    var amplifier: Int = 0,                            // 强化等级
    var duration: Int = 0                             // 持续时间（秒）
)