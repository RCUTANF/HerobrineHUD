package com.RCUTANF.herobrinehud.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


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
    var gamemode: String,                              // 游戏模式（生存、创造、冒险等）
    var health: Double = 20.0,                         // 当前生命值
    var maxHealth: Double = 20.0,                      // 最大生命值
    var foodLevel: Int = 20,                           // 饱食度（0-20）
    var armor: Int = 0,                                // 护甲值
    var isAlive: Boolean = true,                       // 存活状态
    var dimension: String? = null,                     // 所在维度

    // 装备信息
    var equipment: Equipment = Equipment(),            // 装备详情

    // 效果列表
    val effects: MutableList<PlayerEffect> = mutableListOf(),

    // 自定义数据（灵活扩展）
    val customData: MutableMap<String, JsonElement> = mutableMapOf()
) {

}

/**
 * 装备信息
 * 可复用的装备数据结构
 */
@Serializable
data class Equipment(
    var helmet: String? = null,                        // 头盔类型
    var chestplate: String? = null,                    // 胸甲类型
    var leggings: String? = null,                      // 护腿类型
    var boots: String? = null,                         // 鞋子类型
    var mainHand: String? = null,                     // 主手物品类型
    var mainHandCD: String? = null,                   //主手冷却时间
    var offHand: String? = null,                      // 副手物品类型
    var offHandCD: String? = null                     //副手冷却时间
)

@Serializable
data class PlayerEffect(
    var name: String,                                  // 效果名称
    var identifier: String,                            // 效果标识符
    var amplifier: Int = 0,                            // 强化等级
    var duration: Int = 0                             // 持续时间（秒）
)