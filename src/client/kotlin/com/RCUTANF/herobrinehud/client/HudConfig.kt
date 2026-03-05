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
                LOGGER.info("已加载 HUD 配置")
            } else {
                LOGGER.info("未找到 HUD 配置文件，使用默认设置")
                save()
            }
        } catch (e: Exception) {
            LOGGER.error("加载 HUD 配置失败: {}", e.message)
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
            LOGGER.info("HUD 配置已保存")
        } catch (e: Exception) {
            LOGGER.error("保存 HUD 配置失败: {}", e.message)
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

/**
 * 展示槽位配置
 * 每个槽位对应屏幕上的一个队伍展示区域
 */
@Serializable
data class DisplaySlot(
    /** 槽位索引（0 = 最左, 1 = 次左, …） */
    val index: Int,
    /** 绑定的队伍名称 */
    var teamName: String,
    /** 展示侧（LEFT 或 RIGHT），用于决定渲染位置 */
    var side: DisplaySide = DisplaySide.LEFT
)

/**
 * 展示侧枚举
 */
@Serializable
enum class DisplaySide {
    LEFT, RIGHT
}

/**
 * 持久化配置数据
 */
@Serializable
data class ConfigData(
    /**
     * 展示队伍槽位列表（支持任意数量的队伍同时展示）
     * 取代原来的 leftTeam / rightTeam，不再局限于两个队伍
     */
    val displaySlots: MutableList<DisplaySlot> = mutableListOf(),

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

    /** 隐藏的玩家 UUID 列表 */
    val hiddenPlayers: MutableSet<String> = mutableSetOf()
) {
    // ──────────── 向后兼容的便捷属性 ────────────

    /** 左侧队伍名称（兼容旧代码，等价于第一个 LEFT 槽位） */
    var leftTeam: String?
        get() = displaySlots.firstOrNull { it.side == DisplaySide.LEFT }?.teamName
        set(value) {
            displaySlots.removeAll { it.side == DisplaySide.LEFT }
            if (value != null) {
                displaySlots.add(0, DisplaySlot(index = 0, teamName = value, side = DisplaySide.LEFT))
                reindex()
            }
        }

    /** 右侧队伍名称（兼容旧代码，等价于第一个 RIGHT 槽位） */
    var rightTeam: String?
        get() = displaySlots.firstOrNull { it.side == DisplaySide.RIGHT }?.teamName
        set(value) {
            displaySlots.removeAll { it.side == DisplaySide.RIGHT }
            if (value != null) {
                displaySlots.add(DisplaySlot(index = displaySlots.size, teamName = value, side = DisplaySide.RIGHT))
                reindex()
            }
        }

    /** 重新整理槽位索引 */
    private fun reindex() {
        displaySlots.forEachIndexed { i, slot -> slot.index.let { displaySlots[i] = slot.copy(index = i) } }
    }
}

