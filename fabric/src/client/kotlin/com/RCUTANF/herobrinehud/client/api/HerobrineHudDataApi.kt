package com.RCUTANF.herobrinehud.client.api

import com.RCUTANF.herobrinehud.client.ClientTeamData
import com.RCUTANF.herobrinehud.client.DisplaySide
import com.RCUTANF.herobrinehud.client.HudConfig
import com.RCUTANF.herobrinehud.client.PlayerPlacement
import com.RCUTANF.herobrinehud.client.ui.HudSelectionState
import com.RCUTANF.herobrinehud.data.PlayerEffect
import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.RCUTANF.herobrinehud.data.TeamInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Read-only facade for HerobrineHUD client data.
 *
 * This API intentionally wraps internal state holders so HUD implementations
 * do not need to depend on ClientTeamData, HudSelectionState, or HudConfig.
 */
object HerobrineHudDataApi {
    private val snapshotJson = Json {
        encodeDefaults = true
    }

    /** Whether the client has received at least one full sync from the server. */
    val isSynced: Boolean
        get() = ClientTeamData.isSynced

    /** The id of the HUD provider currently selected in local config. */
    val currentHudId: String
        get() = HudConfig.data.hudProviderId

    /** A read-only snapshot of user-facing HUD settings. */
    val settings: HudSettingsView
        get() = HudSettingsView(
            hudVisible = HudConfig.data.hudVisible,
            hudOpacity = HudConfig.data.hudOpacity,
            showEquipment = HudConfig.data.showEquipment,
            showEffects = HudConfig.data.showEffects,
            showHealthNumber = HudConfig.data.showHealthNumber,
            showArmor = HudConfig.data.showArmor,
            showDimension = HudConfig.data.showDimension,
            showAvatar = HudConfig.data.showAvatar,
            cardScale = HudConfig.data.cardScale,
            cardStartY = HudConfig.data.cardStartY,
            currentHudId = HudConfig.data.hudProviderId
        )

    /** Returns all synced teams as a list. */
    fun teams(): List<TeamInfo> = ClientTeamData.getAllTeams().values.toList()

    /**
     * Returns synced teams keyed by team name.
     *
     * The returned map is a copy and can be safely read by callers.
     */
    fun teamsByName(): Map<String, TeamInfo> = ClientTeamData.getAllTeams()

    /** Returns a synced team by its internal scoreboard/team name. */
    fun team(name: String): TeamInfo? = ClientTeamData.getTeam(name)

    /** Returns all known players from all synced teams. */
    fun allPlayers(): List<PlayerInfo> = HudSelectionState.getAllKnownPlayers()

    /** Finds a player by UUID across all synced teams. */
    fun playerByUuid(uuid: String): PlayerInfo? =
        allPlayers().firstOrNull { it.uuid == uuid }

    /** Finds a player by Minecraft name across all synced teams. */
    fun playerByName(name: String): PlayerInfo? =
        allPlayers().firstOrNull { it.name == name }

    /**
     * Finds the team containing the supplied player.
     *
     * UUID is preferred when available, with player name used as a fallback.
     */
    fun teamOf(player: PlayerInfo): TeamInfo? =
        teams().firstOrNull { team ->
            team.players.any {
                (it.uuid.isNotEmpty() && it.uuid == player.uuid) || it.name == player.name
            }
        }

    /** Returns players currently assigned to the left side of the HUD. */
    fun leftPlayers(): List<PlayerInfo> = HudSelectionState.getPlayersBySide(DisplaySide.LEFT)

    /** Returns players currently assigned to the right side of the HUD. */
    fun rightPlayers(): List<PlayerInfo> = HudSelectionState.getPlayersBySide(DisplaySide.RIGHT)

    /** Returns synced players that are not currently assigned to either HUD side. */
    fun unassignedPlayers(): List<PlayerInfo> = HudSelectionState.getUnassignedPlayers()

    /** Returns the configured display side for a player UUID. */
    fun playerSide(uuid: String): DisplaySide = HudSelectionState.getPlayerSide(uuid)

    /**
     * Returns configured player placements keyed by UUID.
     *
     * The returned map is a copy and should be treated as read-only.
     */
    fun playerPlacements(): Map<String, PlayerPlacement> =
        HudConfig.data.playerPlacements.toMap()

    /** Returns the HUD hotkey number for a player UUID, or null if none is assigned. */
    fun hotkey(uuid: String): Int? =
        ClientTeamData.getPlayerHotkey(uuid).takeIf { it >= 0 }

    /**
     * Returns hotkey mappings for currently displayed players.
     *
     * Keys are player UUIDs and values are the visible HUD hotkey numbers.
     */
    fun hotkeys(): Map<String, Int> =
        (leftPlayers().mapNotNull { player -> hotkey(player.uuid)?.let { player.uuid to it } } +
            rightPlayers().mapNotNull { player -> hotkey(player.uuid)?.let { player.uuid to it } })
            .toMap()

    /** Returns the player assigned to a HUD hotkey number, or null if unavailable. */
    fun playerByHotkey(number: Int): PlayerInfo? =
        HudSelectionState.getPlayerByHotkeyNumber(number)

    /** Returns the UUID of the player currently being spectated, if tracked. */
    fun spectatingPlayerUuid(): String? = ClientTeamData.getSpectatingPlayer()

    /** Returns the player currently being spectated, if present in synced team data. */
    fun spectatingPlayer(): PlayerInfo? =
        spectatingPlayerUuid()?.let(::playerByUuid)

    /** Returns whether the supplied player UUID is the current spectating target. */
    fun isSpectating(uuid: String): Boolean = ClientTeamData.isSpectating(uuid)

    /**
     * Creates a serializable point-in-time snapshot of the current client HUD data.
     *
     * The returned value is detached from internal maps and lists so callers can
     * serialize or inspect it without mutating HerobrineHUD's live client state.
     */
    fun snapshot(): HerobrineHudSnapshot =
        HerobrineHudSnapshot(
            isSynced = isSynced,
            teams = teams().map(::copyTeam),
            allPlayers = allPlayers().map(::copyPlayer),
            leftPlayers = leftPlayers().map(::copyPlayer),
            rightPlayers = rightPlayers().map(::copyPlayer),
            unassignedPlayers = unassignedPlayers().map(::copyPlayer),
            playerAssignments = playerPlacements().toMap(),
            hotkeys = hotkeys(),
            spectatingPlayerUuid = spectatingPlayerUuid(),
            settings = settings,
            currentHudId = currentHudId
        )

    /**
     * Encodes the current HUD snapshot as JSON.
     *
     * A custom [Json] instance can be supplied by callers that need different
     * formatting or unknown-key behavior.
     */
    fun snapshotJson(json: Json = snapshotJson): String =
        json.encodeToString(snapshot())

    private fun copyTeam(team: TeamInfo): TeamInfo =
        team.copy(
            players = team.players.map(::copyPlayer).toMutableList(),
            customData = team.customData.toMutableMap()
        )

    private fun copyPlayer(player: PlayerInfo): PlayerInfo =
        player.copy(
            equipment = player.equipment.copy(),
            effects = player.effects.map(PlayerEffect::copy).toMutableList(),
            customData = player.customData.toMutableMap()
        )
}

/**
 * Serializable data snapshot for HUD implementations and external UI systems.
 *
 * This type is intentionally a value transfer object: it mirrors the public
 * read-only Data API at the time [HerobrineHudDataApi.snapshot] is called.
 */
@Serializable
data class HerobrineHudSnapshot(
    /** Whether the client has received at least one full sync from the server. */
    val isSynced: Boolean,
    /** All synced teams, including their player lists. */
    val teams: List<TeamInfo>,
    /** All known players from all synced teams. */
    val allPlayers: List<PlayerInfo>,
    /** Players currently assigned to the left HUD side. */
    val leftPlayers: List<PlayerInfo>,
    /** Players currently assigned to the right HUD side. */
    val rightPlayers: List<PlayerInfo>,
    /** Synced players that are not currently assigned to either HUD side. */
    val unassignedPlayers: List<PlayerInfo>,
    /** Player placement assignments keyed by player UUID. */
    val playerAssignments: Map<String, PlayerPlacement>,
    /** Visible HUD hotkey mappings keyed by player UUID. */
    val hotkeys: Map<String, Int>,
    /** UUID of the player currently being spectated, if tracked. */
    val spectatingPlayerUuid: String?,
    /** User-facing HUD display settings. */
    val settings: HudSettingsView,
    /** The id of the currently selected HUD provider. */
    val currentHudId: String
)

/**
 * Read-only view of local HUD display settings.
 *
 * These values mirror [HudConfig] at the time this view is created.
 */
@Serializable
data class HudSettingsView(
    /** Whether HerobrineHUD rendering is globally visible. */
    val hudVisible: Boolean,
    /** Global HUD opacity in the 0.0 to 1.0 range. */
    val hudOpacity: Float,
    /** Whether HUDs should show player equipment when they support it. */
    val showEquipment: Boolean,
    /** Whether HUDs should show player effects when they support it. */
    val showEffects: Boolean,
    /** Whether HUDs should show numeric health when they support it. */
    val showHealthNumber: Boolean,
    /** Whether HUDs should show armor when they support it. */
    val showArmor: Boolean,
    /** Whether HUDs should show dimension information when they support it. */
    val showDimension: Boolean,
    /** Whether HUDs should show player avatars when they support it. */
    val showAvatar: Boolean,
    /** User-selected card scale for HUDs that support card-style layouts. */
    val cardScale: Float,
    /** User-selected card start Y offset for HUDs that support it. */
    val cardStartY: Int,
    /** The id of the currently selected HUD provider. */
    val currentHudId: String
)
