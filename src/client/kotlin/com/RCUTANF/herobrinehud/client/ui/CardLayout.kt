package com.RCUTANF.herobrinehud.client.ui

/**
 * 玩家卡片布局常量
 *
 * 竖向卡片设计：
 *  - 卡片总宽 60px，高 110px（窄而高）
 *  - 顶部：头像（左）+ 主副手物品（右）
 *  - 头像下方：名称
 *  - 名称下方：左侧心形+血量，右侧盔甲图标+盔甲值
 *  - 其下：四个护甲槽位
 *  - 最下：效果徽章
 */
object CardLayout {

    // ── 卡片整体 ──────────────────────────────────────────────
    const val CARD_WIDTH = 60         // 窄卡片
    const val CARD_HEIGHT = 110       // 高卡片
    const val CARD_GAP = 3            // 卡片之间的间距
    const val TEAM_GAP = 6            // 队伍之间的间距
    const val MARGIN = 4              // 屏幕边距

    // ── 顶部区域：头像 + 主副手 ────────────────────────────────
    const val AVATAR_SIZE = 16        // 头像渲染大小（像素）
    const val AVATAR_X_OFFSET = 2     // 头像在卡片内的 X 偏移
    const val AVATAR_Y_OFFSET = 2     // 头像在卡片内的 Y 偏移（顶部）
    
    const val HAND_SLOT_SIZE = 8      // 主副手槽位大小（头像高度的一半）
    const val HAND_X_OFFSET = 20      // 主副手起始 X（头像右侧）
    const val HAND_Y_OFFSET = 2       // 主手 Y 偏移（与头像顶部对齐）
    const val HAND_GAP = 0            // 主副手之间的间距（紧贴）

    // ── 名称区域 ──────────────────────────────────────────────
    const val NAME_Y_OFFSET = 20      // 名称文字在卡片内的 Y 偏移（头像下方）
    const val NAME_MAX_WIDTH = 56     // 名称最大宽度（卡片宽度 - 边距）
    
    // ── 队伍名称区域 ──────────────────────────────────────────
    const val TEAM_NAME_Y_OFFSET = 30 // 队伍名称在卡片内的 Y 偏移
    const val HOTKEY_Y_OFFSET = 38    // 快捷键编号在卡片内的 Y 偏移

    // ── 血量和盔甲值区域 ──────────────────────────────────────
    const val HEALTH_ARMOR_Y = 48     // 血量/盔甲值行的 Y 偏移
    const val HEART_ICON_SIZE = 9     // 心形图标大小
    const val ARMOR_ICON_SIZE = 9     // 护甲图标大小
    const val ICON_TEXT_GAP = 2       // 图标与数字之间的间距
    const val HEALTH_X_OFFSET = 2     // 血量（心形）起始 X
    const val ARMOR_X_OFFSET = 32     // 盔甲值起始 X（右侧）

    // ── 护甲槽位区域 ──────────────────────────────────────────
    const val ARMOR_SLOTS_Y = 60      // 护甲槽位行的 Y 偏移
    const val SLOT_SIZE = 12          // 装备槽位大小
    const val SLOT_GAP = 2            // 槽位之间的间距

    // ── 效果徽章区域 ──────────────────────────────────────────
    const val EFFECTS_Y = 76          // 效果徽章起始 Y 偏移
    const val EFFECT_BADGE_SIZE = 10  // 效果徽章大小
    const val EFFECT_BADGE_GAP = 2    // 效果徽章之间的间距

    // ── 废弃的常量（保留以防其他地方引用） ──────────────────
    @Deprecated("使用新的竖向布局")
    const val AVATAR_SECTION_WIDTH = 20
    @Deprecated("使用新的竖向布局")
    const val INFO_X = 23
    @Deprecated("使用新的竖向布局")
    const val ROW1_Y = 4
    @Deprecated("使用新的竖向布局")
    const val HEALTH_BAR_WIDTH = 60
    @Deprecated("使用新的竖向布局")
    const val HEALTH_BAR_HEIGHT = 6
    @Deprecated("使用新的竖向布局")
    const val HEALTH_NUMBER_Y = 4
    @Deprecated("使用新的竖向布局")
    const val DIM_BADGE_X_FROM_RIGHT = 6
    @Deprecated("使用新的竖向布局")
    const val ROW2_Y = 16
    @Deprecated("使用新的竖向布局")
    const val ARMOR_ICON_GAP = 2
    @Deprecated("使用新的竖向布局")
    const val ARMOR_VALUE_WIDTH = 28
    @Deprecated("使用新的竖向布局")
    const val ROW3_Y = 30

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
