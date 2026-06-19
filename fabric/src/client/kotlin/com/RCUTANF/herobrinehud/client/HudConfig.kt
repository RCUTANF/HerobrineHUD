package com.RCUTANF.herobrinehud.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files

/**
 * HUD 配置持久化
 *
 * 保存用户选择的队伍、HUD 显示偏好等到本地 JSON 文件。
 */
object HudConfig {

    private val LOGGER = LoggerFactory.getLogger("HerobrineHUD/Config")

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val configPath = FabricLoader.getInstance().configDir.resolve("herobrinehud.json")

    var data: ConfigData = ConfigData()
        private set

    /**
     * 从磁盘加载配置
     */
    fun load() {
        try {
            if (Files.exists(configPath)) {
                val content = Files.readString(configPath)
                data = json.decodeFromString<ConfigData>(content)
                LOGGER.info("HUD configuration loaded")
            } else {
                LOGGER.info("HUD config not found, using defaults")
                save()
            }
        } catch (e: Exception) {
            LOGGER.error("Failed to load HUD config: {}", e.message)
            data = ConfigData()
        }
    }

    /**
     * 保存配置到磁盘
     */
    fun save() {
        try {
            Files.createDirectories(configPath.parent)
            Files.writeString(configPath, json.encodeToString<ConfigData>(data))
            LOGGER.info("HUD configuration saved")
        } catch (e: Exception) {
            LOGGER.error("Failed to save HUD config: {}", e.message)
        }
    }

    /**
     * 更新配置并持久化
     */
    fun update(block: ConfigData.() -> Unit) {
        data.block()
        save()
    }
}

// ──────────────────────────────────────────────────────────────
//  展示侧枚举
// ──────────────────────────────────────────────────────────────

@Serializable
enum class DisplaySide {
    LEFT, RIGHT,
    /** 玩家不上屏 */
    NONE
}

// ──────────────────────────────────────────────────────────────
//  玩家上屏分配（新模型：以玩家 UUID 为键，独立于队伍）
// ──────────────────────────────────────────────────────────────

@Serializable
data class PlayerPlacement(
    val uuid: String,
    val side: DisplaySide = DisplaySide.NONE
)

// ──────────────────────────────────────────────────────────────
//  旧版队伍槽位（保留用于 JSON 反序列化迁移）
// ──────────────────────────────────────────────────────────────

@Serializable
data class DisplaySlot(
    val index: Int,
    var teamName: String,
    var side: DisplaySide = DisplaySide.LEFT
)

// ──────────────────────────────────────────────────────────────
//  持久化配置数据
// ──────────────────────────────────────────────────────────────

@Serializable
data class ConfigData(
    /**
     * 玩家上屏分配表（新主键）
     * key = uuid, value = PlayerPlacement
     */
    val playerPlacements: MutableMap<String, PlayerPlacement> = mutableMapOf(),

    /** 当前选中的 HUD Provider */
    var hudProviderId: String = "herobrinehud:classic",

    /** HUD 是否可见 */
    var hudVisible: Boolean = true,

    /** HUD 不透明度 (0.0~1.0) */
    var hudOpacity: Float = 1.0f,

    /** 是否显示装备信息 */
    var showEquipment: Boolean = true,
    /** 是否显示效果信息 */
    var showEffects: Boolean = true,
    /** 是否显示生命值数字 */
    var showHealthNumber: Boolean = true,
    /** 是否显示护甲值 */
    var showArmor: Boolean = true,
    /** 是否显示所在维度徽章 */
    var showDimension: Boolean = true,
    /** 是否显示玩家头像 */
    var showAvatar: Boolean = true,
    /** 卡片缩放系数 (0.5~2.0) */
    var cardScale: Float = 1.0f,
    /** 卡片起始 Y 坐标偏移（从屏幕顶部开始的像素距离） */
    var cardStartY: Int = 50
)
