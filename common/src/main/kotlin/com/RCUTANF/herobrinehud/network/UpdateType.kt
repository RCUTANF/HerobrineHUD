package com.RCUTANF.herobrinehud.network

/**
 * Incremental update type identifiers shared by client and server.
 */
object UpdateType {
    // Team-level changes
    const val TEAM_ADDED = "team_added"
    const val TEAM_REMOVED = "team_removed"
    const val TEAM_MODIFIED = "team_modified"

    // Team membership changes
    const val PLAYER_JOINED_TEAM = "player_joined_team"
    const val PLAYER_LEFT_TEAM = "player_left_team"

    // Player data changes
    const val PLAYER_DATA_UPDATED = "player_data_updated"
    const val PLAYER_JOINED_SERVER = "player_joined_server"
    const val PLAYER_LEFT_SERVER = "player_left_server"
}


