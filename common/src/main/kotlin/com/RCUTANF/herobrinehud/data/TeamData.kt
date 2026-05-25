package com.RCUTANF.herobrinehud.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TeamInfo(
    var name: String,                                  // 队伍名称
    var displayName: String,                           // 队伍标签/简称（2-4字母）
    var color: String = "#FFFFFF",                     // 主题色（HEX格式）

    // 队员列表
    val players: MutableList<PlayerInfo> = mutableListOf(),

    // 自定义数据（灵活扩展）
    val customData: MutableMap<String, JsonElement> = mutableMapOf()
) {

}

