package com.RCUTANF.herobrinehud.team

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.world.scores.PlayerTeam

/**
 * 队伍同步事件回调
 * 用于在队伍发生变化时通知监听者
 */
object TeamSyncCallback {

    /**
     * 队伍被添加/创建时触发
     */
    val TEAM_ADDED: Event<TeamAdded> = EventFactory.createArrayBacked(TeamAdded::class.java) { listeners ->
        TeamAdded { team ->
            listeners.forEach { it.onTeamAdded(team) }
        }
    }

    /**
     * 队伍被移除时触发
     */
    val TEAM_REMOVED: Event<TeamRemoved> = EventFactory.createArrayBacked(TeamRemoved::class.java) { listeners ->
        TeamRemoved { team ->
            listeners.forEach { it.onTeamRemoved(team) }
        }
    }

    /**
     * 队伍属性（颜色、前后缀等）被修改时触发
     */
    val TEAM_MODIFIED: Event<TeamModified> = EventFactory.createArrayBacked(TeamModified::class.java) { listeners ->
        TeamModified { team ->
            listeners.forEach { it.onTeamModified(team) }
        }
    }

    /**
     * 玩家加入队伍时触发
     */
    val PLAYER_JOINED: Event<PlayerJoinedTeam> = EventFactory.createArrayBacked(PlayerJoinedTeam::class.java) { listeners ->
        PlayerJoinedTeam { team, playerName ->
            listeners.forEach { it.onPlayerJoined(team, playerName) }
        }
    }

    /**
     * 玩家离开队伍时触发
     */
    val PLAYER_LEFT: Event<PlayerLeftTeam> = EventFactory.createArrayBacked(PlayerLeftTeam::class.java) { listeners ->
        PlayerLeftTeam { team, playerName ->
            listeners.forEach { it.onPlayerLeft(team, playerName) }
        }
    }

    fun interface TeamAdded {
        fun onTeamAdded(team: PlayerTeam)
    }

    fun interface TeamRemoved {
        fun onTeamRemoved(team: PlayerTeam)
    }

    fun interface TeamModified {
        fun onTeamModified(team: PlayerTeam)
    }

    fun interface PlayerJoinedTeam {
        fun onPlayerJoined(team: PlayerTeam, playerName: String)
    }

    fun interface PlayerLeftTeam {
        fun onPlayerLeft(team: PlayerTeam, playerName: String)
    }
}

