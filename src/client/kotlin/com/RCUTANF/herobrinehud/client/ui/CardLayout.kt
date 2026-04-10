package com.RCUTANF.herobrinehud.client.ui

/**
 * 玩家卡片布局常量
 *
 * 竖向卡片设计：
 *  - 卡片总宽 30px，高 110px（窄而高）
 *  - 顶部：头像（左）+ 主副手物品（右）
 *  - 头像下方：名称
 *  - 名称下方：心形+血量（左）和盔甲图标+盔甲值（右）横向并列（缩小60%）
 *  - 其下：四个护甲槽位（居中）
 *  - 最下：效果徽章
 */
object CardLayout {

    // ── 卡片整体 ──────────────────────────────────────────────
    const val CARD_WIDTH = 35         // 卡片加宽 5px
    const val CARD_HEIGHT = 62       // 更紧凑：继续回收盔甲移除后的 2px 高度
    const val CARD_GAP = 1            // 卡片之间的间距
    const val TEAM_GAP = 6            // 队伍之间的间距
    const val MARGIN = 4              // 屏幕边距

    // ── 顶部区域：头像 + 主副手 ────────────────────────────────
    const val AVATAR_SIZE = 14        // 头像渲染大小（像素）
    const val AVATAR_X_OFFSET = 2     // 头像在卡片内的 X 偏移
    const val AVATAR_Y_OFFSET = 3     // 头像在卡片内的 Y 偏移（顶部）

    // ── 全身像区域 ──────────────────────────────────────────────
    const val FULL_BODY_AREA_WIDTH = 20     // 全身像区域宽度（仅布局区域，不影响全身像实际渲染尺寸）
    const val FULL_BODY_AREA_HEIGHT = 39    // 增加 5px：为脚底到下边框留出更多空间
    const val FULL_BODY_WIDTH = 15          // 全身像渲染宽度（保持不变）
    const val FULL_BODY_HEIGHT = 30         // 全身像渲染高度（保持不变）
    const val FULL_BODY_FRAME_Y_OFFSET = 9  // 全身像边框区域在卡片内的 Y 偏移（固定区域位置）
    const val FULL_BODY_Y_OFFSET = 5       // 玩家全身像实际渲染起点 Y（可单独调，不影响区域边框）
    const val NAME_ABOVE_BODY_Y_OFFSET = 1  // 名称相对卡片顶部的 Y 偏移
    const val FULL_BODY_TOP_PADDING = 2     // 固定顶部留白，让新增高度主要沉到底部

    // 脚底地面条（在全身像内部，先绘制再渲染实体，营造“站在地面上”的错觉）
    const val AVATAR_GROUND_WIDTH = 17
    const val AVATAR_GROUND_HEIGHT = 7
    const val AVATAR_GROUND_Y_FROM_BOTTOM = -14

    const val HAND_SLOT_SIZE = 8      // 主副手槽位大小（头像高度的一半）
    const val HAND_X_OFFSET = 25      // 主副手起始 X（卡片加宽后保持右侧边距）
    const val HAND_Y_OFFSET = 2       // 主手 Y 偏移（与头像顶部对齐）
    const val HAND_GAP = 1            // 主副手之间的间距（紧贴）

    // ── 名称区域 ──────────────────────────────────────────────
    const val NAME_Y_OFFSET = 20      // 名称文字在卡片内的 Y 偏移（头像下方，保持适当间距）
    const val NAME_HEIGHT = 10        // 名称区域高度（增加以容纳更大的文字）
    const val NAME_MAX_WIDTH = 28     // 名称最大宽度（占据整个卡片宽度 - 小边距）

    // ── 队伍名称区域 ──────────────────────────────────────────
    const val TEAM_NAME_Y_OFFSET = 35 // 队伍名称在卡片内的 Y 偏移（暂时不使用）
    const val HOTKEY_Y_OFFSET = 38    // 快捷键编号在卡片内的 Y 偏移

    // ── 血量/饱食度区域（放在全身像下方）────────────────────────
    const val HEALTH_Y_OFFSET = 53    // 血量行起始 Y 偏移（全身像下方）
    const val FOOD_Y_OFFSET = 52      // 饱食度行起始 Y 偏移（血量下方）
    const val HEART_ICON_SIZE = 7     // 心形图标大小
    const val FOOD_ICON_SIZE = 7      // 饱食度图标大小
    const val ICON_TEXT_GAP = 2       // 图标与数字之间的间距
    const val STATS_CENTER_X_OFFSET = 12 // 统计行中心点 X（用于居中血量/饱食度）
    const val DIMENSION_GAP_FROM_HEALTH = 4 // 维度方块与血量文本的间距

    // ── 护甲槽位区域（已停用） ─────────────────────────────────
    const val ARMOR_SLOTS_Y = 40      // 已停用：保留常量避免其他引用报错
    const val SLOT_SIZE = 6           // 装备槽位大小（缩小以适应窄卡片）
    const val SLOT_GAP = 1            // 槽位之间的间距

    // ── 效果徽章区域（主副手下方右侧竖向排列）───────────────────
    const val EFFECTS_X = 26          // 徽章列 X 偏移（位于主副手区域中线）
    const val EFFECTS_START_Y = 21    // 徽章列起始 Y（主副手槽位下方）
    const val EFFECT_BADGE_SIZE = 5   // 效果徽章大小（缩小以适应窄卡片）
    const val EFFECT_BADGE_GAP = 1    // 效果徽章之间的间距（竖向）

    // ── 维度图标尺寸 ──────────────────────────────────────────
    const val DIM_BADGE_ICON_SIZE = 10    // 维度图标渲染大小（像素，与护甲槽位一致）

    // ── 快捷键编号（右下角）────────────────────────────────────
    const val HOTKEY_MARGIN_RIGHT = 2     // 快捷键编号右侧边距
    const val HOTKEY_MARGIN_BOTTOM = 2    // 快捷键编号底部边距

    // ── 维度图标枚举 ──────────────────────────────────────────
    /**
     * 将维度 ID 映射到方块纹理路径，用于渲染卡片背景
     *  - 主世界 → 草方块纹理 (grass_block_top)
     *  - 地狱   → 地狱岩纹理 (netherrack)
     *  - 末地   → 末地岩纹理 (end_stone)
     */
    enum class DimensionIcon(val dimensionId: String, val textureId: String, val blockItemId: String) {
        OVERWORLD("minecraft:overworld",  "minecraft:textures/block/grass_block_side.png", "minecraft:grass_block"),
        NETHER   ("minecraft:the_nether", "minecraft:textures/block/netherrack.png", "minecraft:netherrack"),
        THE_END  ("minecraft:the_end",    "minecraft:textures/block/end_stone.png", "minecraft:end_stone");

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
}
