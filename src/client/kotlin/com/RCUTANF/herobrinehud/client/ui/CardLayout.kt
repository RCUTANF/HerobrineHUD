package com.RCUTANF.herobrinehud.client.ui

/**
 * 玩家卡片布局常量
 *
 * 对应 HTML 原型设计：
 *  - 卡片总宽 200px，高 52px
 *  - 左侧头像区宽 44px（头像16×16 + 名称）
 *  - 右侧信息区三行：血量+维度 / 护甲+装备槽 / 主副手+效果
 */
object CardLayout {

    // ── 卡片整体 ──────────────────────────────────────────────
    const val CARD_WIDTH = 105
    const val CARD_HEIGHT = 45
    const val CARD_GAP = 3            // 卡片之间的间距
    const val TEAM_GAP = 6            // 卡片之间的间距（队伍名称已移到卡片内）
    const val MARGIN = 4              // 屏幕边距

    // ── 左侧头像区 ────────────────────────────────────────────
    const val AVATAR_SECTION_WIDTH = 20
    const val AVATAR_SIZE = 16        // 头像渲染大小（像素）
    const val AVATAR_X_OFFSET = 2    // 头像在卡片内的 X 偏移（居中）
    const val AVATAR_Y_OFFSET = 2    // 头像在卡片内的 Y 偏移
    const val NAME_Y_OFFSET = 20     // 名称文字在卡片内的 Y 偏移（头像下方）
    const val TEAM_NAME_Y_OFFSET = 25 // 队伍名称在卡片内的 Y 偏移（底部）
    const val HOTKEY_Y_OFFSET = 31   // 快捷键编号在卡片内的 Y 偏移（队伍名称下方）
    const val NAME_MAX_WIDTH = 40    // 名称最大宽度（超出截断）

    // ── 右侧信息区 ────────────────────────────────────────────
    const val INFO_X = AVATAR_SECTION_WIDTH + 3   // 信息区起始 X

    // 第一行：血量 + 维度 (y + ROW1_Y)
    const val ROW1_Y = 4
    const val HEALTH_BAR_WIDTH = 60
    const val HEALTH_BAR_HEIGHT = 6
    const val HEALTH_NUMBER_Y = ROW1_Y            // 血量数字与条同行
    const val DIM_BADGE_X_FROM_RIGHT = 6          // 维度徽章距卡片右边距

    // 第二行：护甲值 + 盔甲槽位 (y + ROW2_Y)
    const val ROW2_Y = 16
    const val SLOT_SIZE = 10          // 装备槽位大小（小图标）
    const val SLOT_GAP = 2
    const val ARMOR_ICON_SIZE = 10    // 护甲图标（铁胸甲）大小
    const val ARMOR_ICON_GAP = 2      // 图标与数字之间的间距
    const val ARMOR_VALUE_WIDTH = ARMOR_ICON_SIZE + ARMOR_ICON_GAP + 16  // 护甲图标 + 间距 + 数字区宽度

    // 第三行：主副手 + 效果徽章 (y + ROW3_Y)
    const val ROW3_Y = 30
    const val HAND_SLOT_SIZE = 14     // 主副手槽位大小
    const val EFFECT_BADGE_SIZE = 10  // 效果徽章大小
    const val EFFECT_BADGE_GAP = 2

    /** 将 alpha (0-255) 组合为半透明黑色背景 ARGB */
    fun bgColor(opacity: Int): Int = ((opacity / 2) shl 24) or 0x000000
    // ── 深色槽位背景色 ────────────────────────────────────────
    /** 将 alpha 组合为深色背景 ARGB（用于槽位） */
    fun slotBgColor(opacity: Int): Int = ((opacity * 2 / 3) shl 24) or 0x161b22

    // ── 维度图标尺寸 ──────────────────────────────────────────
    const val DIM_BADGE_ICON_SIZE = 10    // 维度图标渲染大小（像素，与护甲槽位一致）

    // ── 维度图标枚举 ──────────────────────────────────────────
    /**
     * 将维度 ID 映射到原版方块物品 ID，用于渲染图标徽章
     *  - 主世界 → 草方块 (grass_block)
     *  - 地狱   → 地狱岩 (netherrack)
     *  - 末地   → 末地岩 (end_stone)
     */
    enum class DimensionIcon(val dimensionId: String, val blockItemId: String) {
        OVERWORLD("minecraft:overworld",  "minecraft:grass_block"),
        NETHER   ("minecraft:the_nether", "minecraft:netherrack"),
        THE_END  ("minecraft:the_end",    "minecraft:end_stone");

        companion object {
            /** 根据维度 ID 查找对应枚举项，找不到返回 null */
            fun fromDimensionId(id: String): DimensionIcon? =
                entries.firstOrNull { it.dimensionId == id }
        }
    }

    // ── 维度颜色映射（保留，可供其他地方使用） ────────────────────
    val DIMENSION_COLORS = mapOf(
        "minecraft:overworld" to 0x55FF55,  // 绿
        "minecraft:the_nether" to 0xFF5555, // 红
        "minecraft:the_end" to 0xAA55FF     // 紫
    )

    val DIMENSION_NAMES = mapOf(
        "minecraft:overworld" to "主世界",
        "minecraft:the_nether" to "下界",
        "minecraft:the_end" to "末地"
    )

    // ── 效果颜色映射（部分常见效果） ──────────────────────────────
    val EFFECT_COLORS = mapOf(
        "minecraft:speed" to 0x7CAFC6,
        "minecraft:slowness" to 0x5A6C81,
        "minecraft:strength" to 0x932423,
        "minecraft:weakness" to 0x484D48,
        "minecraft:regeneration" to 0xCD5CAB,
        "minecraft:poison" to 0x4E9331,
        "minecraft:wither" to 0x352A27,
        "minecraft:fire_resistance" to 0xE49A3A,
        "minecraft:water_breathing" to 0x2E5299,
        "minecraft:resistance" to 0x99453A,
        "minecraft:jump_boost" to 0x786297,
        "minecraft:haste" to 0xD9C043,
        "minecraft:mining_fatigue" to 0x4A4217,
        "minecraft:night_vision" to 0x1F1FA1,
        "minecraft:invisibility" to 0x7F8392,
        "minecraft:absorption" to 0x2552A5,
        "minecraft:saturation" to 0xF82423
    )
}
