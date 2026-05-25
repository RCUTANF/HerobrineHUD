package com.RCUTANF.herobrinehud.network

import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.RCUTANF.herobrinehud.data.TeamInfo
import kotlinx.serialization.Serializable

// ══════════════════════════════════════════════════════════════
//  全量同步数据包
// ══════════════════════════════════════════════════════════════

/**
 * 全量同步时的根数据结构
 */
@Serializable
data class FullSyncData(
    val teams: Map<String, TeamInfo>
)

// ══════════════════════════════════════════════════════════════
//  增量更新数据包（各类型对应不同结构）
// ══════════════════════════════════════════════════════════════

/** 队伍新增/修改：携带完整队伍数据 */
@Serializable
data class TeamUpdateData(
    val team: TeamInfo
)

/** 队伍移除：仅需队伍名 */
@Serializable
data class TeamRemovedData(
    val teamName: String
)

/** 玩家加入队伍 */
@Serializable
data class PlayerJoinedTeamData(
    val teamName: String,
    val player: PlayerInfo
)

/** 玩家离开队伍 */
@Serializable
data class PlayerLeftTeamData(
    val teamName: String,
    val playerName: String
)

/** 玩家数据更新（定位到某队伍中的某玩家，替换其数据） */
@Serializable
data class PlayerDataUpdatedData(
    val teamName: String,
    val player: PlayerInfo
)

/** 玩家离开服务器（标记离线字段） */
@Serializable
data class PlayerLeftServerData(
    val playerName: String
)

