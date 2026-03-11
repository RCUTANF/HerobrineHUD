package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.util.AvatarTextureCache
import com.RCUTANF.herobrinehud.client.HudConfig
import com.RCUTANF.herobrinehud.client.ClientTeamData
import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.mojang.authlib.GameProfile
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.UUID

/**
 * 玩家卡片渲染器
 *
 * 负责渲染单张玩家信息卡片（竖向布局），包含：
 *  - 顶部：头像（左）+ 主副手物品（右）
 *  - 头像下方：名称
 *  - 名称下方：心形+血量（左）和盔甲图标+盔甲值（右）横向并列（缩小60%）
 *  - 其下：四个护甲槽位（居中）
 *  - 最下：效果徽章
 */
object PlayerCardRenderer {

    private val L = CardLayout

    /**
     * 渲染完整的玩家卡片（竖向布局）
     *
     * @param ctx       GuiGraphics 上下文
     * @param player    玩家数据
     * @param cardX     卡片左上角 X
     * @param cardY     卡片左上角 Y
     * @param teamName  玩家所属队伍名称
     * @param teamColor 队伍颜色（HEX 格式，如 "#FF5555"）
     * @param opacity   不透明度 (0-255)
     */
    fun renderCard(ctx: GuiGraphics, player: PlayerInfo, cardX: Int, cardY: Int, teamName: String, teamColor: String, opacity: Int) {
        val config = HudConfig.data

        // 卡片背景
        ctx.fill(cardX, cardY, cardX + L.CARD_WIDTH, cardY + L.CARD_HEIGHT, L.bgColor(opacity))

        // 竖向布局：
        // 1. 顶部：头像（左） + 主副手物品（右）
        // 2. 头像下方：名称
        // 3. 名称下方：心形+血量（左）和盔甲图标+盔甲值（右）横向并列（缩小60%）
        // 4. 其下：四个护甲槽位（居中）
        // 5. 最下：效果徽章

        val avatarX = cardX + L.AVATAR_X_OFFSET
        val avatarY = cardY + L.AVATAR_Y_OFFSET

        // 1. 顶部：头像 + 旁观高亮
        if (config.showAvatar) {
            renderAvatar(ctx, player, avatarX, avatarY, opacity)
            if (ClientTeamData.isSpectating(player.uuid)) {
                renderSpectateHighlight(ctx, avatarX, avatarY, opacity)
            }
        }

        // 1. 顶部右侧：主副手物品（与头像同一行）
        if (config.showEquipment) {
            val handX = cardX + L.HAND_X_OFFSET
            val handY = cardY + L.HAND_Y_OFFSET
            renderHandIcons(ctx, player, handX, handY, opacity)
        }

        // 2. 名称（头像下方，居中）
        val nameX = cardX + 2
        val nameY = cardY + L.NAME_Y_OFFSET
        renderName(ctx, player, nameX, nameY, opacity)

        // 3. 队伍名（名称下方）- 暂时不渲染
        // val teamNameY = cardY + L.TEAM_NAME_Y_OFFSET
        // renderTeamName(ctx, teamName, teamColor, nameX, teamNameY, opacity)

        // 4. 血量和盔甲值（竖向排列）
        if (config.showHealthNumber || config.showArmor) {
            val healthArmorY = cardY + L.HEALTH_ARMOR_Y
            renderHealthAndArmorValuesVertical(ctx, player, cardX, healthArmorY, opacity)
        }

        // 5. 护甲槽位（四个槽位，居中排列）
        if (config.showArmor) {
            val armorSlotsY = cardY + L.ARMOR_SLOTS_Y
            renderArmorRowCentered(ctx, player, cardX, armorSlotsY, opacity)
        }

        // 6. 效果徽章（底部，居中排列）
        if (config.showEffects) {
            val effectsY = cardY + L.EFFECTS_Y
            val effectsX = cardX + 2
            val availableWidth = L.CARD_WIDTH - 4
            renderEffectBadges(ctx, player, effectsX, effectsY, availableWidth, opacity)
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  头像
    // ──────────────────────────────────────────────────────────────

    private fun renderAvatar(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val client = Minecraft.getInstance()

        // 优先使用服务端下发的头像 URL（AvatarTextureCache 异步下载并注册）
        val avatarTexture: Identifier? = player.avatar?.let { AvatarTextureCache.getTexture(it) }

        if (avatarTexture != null) {
            blitHead(ctx, avatarTexture, x, y)
            return
        }

        // 降级：尝试从本地 SkinManager 获取（适用于头像 URL 尚未就绪，或玩家正在本地联机）
        val uuid = runCatching { UUID.fromString(player.uuid) }.getOrNull()
        if (uuid != null) {
            try {
                val skinTexture = client.skinManager
                    .createLookup(GameProfile(uuid, player.name), false)
                    .get()
                    .body()
                    .texturePath()
                blitHead(ctx, skinTexture, x, y)
                return
            } catch (_: Exception) {
                // 皮肤未加载，继续 fallback
            }
        }

        // 最终 Fallback：纯色方块 + 名称首字母
        val font = client.font
        ctx.fill(x, y, x + L.AVATAR_SIZE, y + L.AVATAR_SIZE, (opacity shl 24) or 0x336699)
        val initial = player.name.take(1).uppercase()
        ctx.drawString(
            font, initial,
            x + (L.AVATAR_SIZE - font.width(initial)) / 2,
            y + (L.AVATAR_SIZE - 8) / 2,
            (opacity shl 24) or 0xFFFFFF, false
        )
    }

    /**
     * 从标准皮肤贴图中绘制头部（底层 + 覆盖层）
     *
     * @param texture 已注册的皮肤纹理 Identifier（64×64 标准格式）
     */
    private fun blitHead(ctx: GuiGraphics, texture: Identifier, x: Int, y: Int) {
        // 头部底层：UV (8,8)，大小 8×8
        ctx.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x, y,
            8f, 8f,
            L.AVATAR_SIZE, L.AVATAR_SIZE,
            8, 8,
            64, 64
        )
        // 头部覆盖层：UV (40,8)，大小 8×8
        ctx.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x, y,
            40f, 8f,
            L.AVATAR_SIZE, L.AVATAR_SIZE,
            8, 8,
            64, 64
        )
    }

    // ──────────────────────────────────────────────────────────────
    //  名称
    // ──────────────────────────────────────────────────────────────

    private fun renderName(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val nameColor = if (player.isAlive) (opacity shl 24) or 0xFFFFFF else (opacity shl 24) or 0x888888
        val scale = 0.5f
        val displayName = if (font.width(player.name) > L.NAME_MAX_WIDTH) {
            // 截断并加省略号
            var truncated = player.name
            while (truncated.isNotEmpty() && font.width("$truncated…") > L.NAME_MAX_WIDTH) {
                truncated = truncated.dropLast(1)
            }
            "$truncated…"
        } else {
            player.name
        }
        val pose = ctx.pose()
        pose.pushMatrix()
        // 头像中心 X = x + L.AVATAR_SIZE / 2
        // 将缩放后的文本中心对齐到头像中心
        val textWidth = font.width(displayName)
        val textX = (L.AVATAR_SIZE / 2f - textWidth * scale / 2f).toInt()
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        ctx.drawString(font, displayName, textX, 0, nameColor, false)
        pose.popMatrix()

    }

    // ──────────────────────────────────────────────────────────────
    //  队伍名称
    // ──────────────────────────────────────────────────────────────

    private fun renderTeamName(ctx: GuiGraphics, teamName: String, teamColor: String, x: Int, y: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val rgb = try {
            teamColor.trimStart('#').toInt(16) and 0xFFFFFF
        } catch (_: Exception) {
            0xAAAAAA
        }
        val teamColorInt = (opacity shl 24) or rgb
        val scale = 0.5f
        val pose = ctx.pose()
        pose.pushMatrix()
        // 头像中心 X = x + L.AVATAR_SIZE / 2
        // 将缩放后的文本中心对齐到头像中心
        val textWidth = font.width(teamName)
        val textX = (L.AVATAR_SIZE / 2f - textWidth * scale / 2f).toInt()
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        ctx.drawString(font, teamName, textX, 0, teamColorInt, false)
        pose.popMatrix()

    }

    // ──────────────────────────────────────────────────────────────
    //  快捷键编号
    // ──────────────────────────────────────────────────────────────

    private fun renderHotkeyNumber(ctx: GuiGraphics, hotkeyNumber: Int, x: Int, y: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val hotkeyText = "[$hotkeyNumber]"
        val hotkeyColor = (opacity shl 24) or 0xFFFF55  // 黄色
        val scale = 0.5f
        val pose = ctx.pose()
        pose.pushMatrix()
        // 将缩放后的文本中心对齐到头像中心
        val textWidth = font.width(hotkeyText)
        val textX = (L.AVATAR_SIZE / 2f - textWidth * scale / 2f).toInt()
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        ctx.drawString(font, hotkeyText, textX, 0, hotkeyColor, false)
        pose.popMatrix()
    }

    // ──────────────────────────────────────────────────────────────
    //  生命值条
    // ──────────────────────────────────────────────────────────────

    private fun renderHealthBar(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val healthPercent = if (player.maxHealth > 0)
            (player.health / player.maxHealth).coerceIn(0.0, 1.0) else 0.0

        val barBg = (opacity / 2 shl 24) or 0x333333
        val barFg = (opacity shl 24) or getHealthColor(healthPercent)

        val barY = y + 1
        ctx.fill(x, barY, x + L.HEALTH_BAR_WIDTH, barY + L.HEALTH_BAR_HEIGHT, barBg)
        ctx.fill(x, barY, x + (L.HEALTH_BAR_WIDTH * healthPercent).toInt(), barY + L.HEALTH_BAR_HEIGHT, barFg)

        if (HudConfig.data.showHealthNumber) {
            val font = Minecraft.getInstance().font
            val healthText = "${player.health.toInt()}/${player.maxHealth.toInt()}"
            val textWidth = font.width(healthText)
            val textX = x + (L.HEALTH_BAR_WIDTH - textWidth) / 2
            ctx.drawString(font, healthText, textX, y, (opacity shl 24) or 0xFFFFFF, true)
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  维度徽章
    // ──────────────────────────────────────────────────────────────

    private fun renderDimensionBadge(ctx: GuiGraphics, player: PlayerInfo, cardX: Int, y: Int, opacity: Int) {
        val dim = player.dimension ?: return

        // 通过枚举查找对应方块物品 ID；未知维度则跳过渲染
        val dimIcon = CardLayout.DimensionIcon.fromDimensionId(dim) ?: return

        val iconSize = L.DIM_BADGE_ICON_SIZE
        val iconX = cardX + L.CARD_WIDTH - iconSize - L.DIM_BADGE_X_FROM_RIGHT
        // 垂直居中对齐到血量条（ROW1_Y 区域高度约为 HEALTH_BAR_HEIGHT，取中间）
        val iconY = y + (L.HEALTH_BAR_HEIGHT - iconSize) / 2

        renderSmallItemSlot(ctx, dimIcon.blockItemId, iconX, iconY, iconSize, opacity)
    }

    // ──────────────────────────────────────────────────────────────
    //  护甲行（护甲值 + 四个护甲槽位）
    // ──────────────────────────────────────────────────────────────

    // 新增：竖向布局专用 - 横向并列心形图标+血量和盔甲图标+盔甲值（缩小尺寸）
    private fun renderHealthAndArmorValuesVertical(ctx: GuiGraphics, player: PlayerInfo, cardX: Int, y: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val iconSize = L.HEART_ICON_SIZE
        val scale = 0.6f  // 缩放到60%大小
        
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate(cardX.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        
        // 左侧：心形图标 + 血量文本
        val healthText = "${player.health.toInt()}"
        val heartTexture = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full")
        val leftX = 2
        
        ctx.blitSprite(RenderPipelines.GUI_TEXTURED, heartTexture, leftX, 0, iconSize, iconSize)
        ctx.drawString(font, healthText, leftX + iconSize + L.ICON_TEXT_GAP, 0, (opacity shl 24) or 0xFFFFFF, false)

        // 右侧：盔甲图标 + 盔甲值
        val armorValueText = "${player.armor}"
        val armorIconX = 26  // 右侧位置
        
        // 临时保存当前矩阵状态
        pose.popMatrix()
        pose.pushMatrix()
        pose.translate(cardX.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        
        renderSmallItemSlot(ctx, "minecraft:iron_chestplate", armorIconX, 0, iconSize, opacity)
        ctx.drawString(font, armorValueText, armorIconX + iconSize + L.ICON_TEXT_GAP, 0, (opacity shl 24) or 0xFFFFFF, false)
        
        pose.popMatrix()
    }

    // 新增：竖向布局专用 - 居中渲染四个护甲槽位
    private fun renderArmorRowCentered(ctx: GuiGraphics, player: PlayerInfo, cardX: Int, y: Int, opacity: Int) {
        val eq = player.equipment
        val armorItems = listOf(eq.helmet, eq.chestplate, eq.leggings, eq.boots)
        
        // 计算总宽度并居中
        val totalWidth = (L.SLOT_SIZE * 4) + (L.SLOT_GAP * 3)
        val startX = cardX + (L.CARD_WIDTH - totalWidth) / 2
        
        var slotX = startX
        for (itemId in armorItems) {
            renderSmallItemSlot(ctx, itemId, slotX, y, L.SLOT_SIZE, opacity)
            slotX += L.SLOT_SIZE + L.SLOT_GAP
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  主副手图标（竖向排列）
    // ──────────────────────────────────────────────────────────────

    private fun renderHandIcons(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val eq = player.equipment
        // 主手在上方
        renderItemSlot(ctx, eq.mainHand, x, y, L.HAND_SLOT_SIZE, opacity)
        // 副手在下方（紧贴主手）
        renderItemSlot(ctx, eq.offHand, x, y + L.HAND_SLOT_SIZE + L.HAND_GAP, L.HAND_SLOT_SIZE, opacity)
    }

    // ──────────────────────────────────────────────────────────────
    //  效果徽章
    // ──────────────────────────────────────────────────────────────

    private fun renderEffectBadges(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, availableWidth: Int, opacity: Int) {
        if (player.effects.isEmpty()) return
        val font = Minecraft.getInstance().font
        val badgeStep = L.EFFECT_BADGE_SIZE + L.EFFECT_BADGE_GAP + 2  // 每个徽章占用的宽度（含外框）
        val maxBadges = (availableWidth / badgeStep).coerceAtLeast(1)

        for ((index, effect) in player.effects.take(maxBadges).withIndex()) {
            val bx = x + index * badgeStep

            // 绘制外框背景（深色半透明）
            val frameBg = ((opacity * 3 / 4) shl 24) or 0x222222
            ctx.fill(bx - 1, y - 1, bx + L.EFFECT_BADGE_SIZE + 1, y + L.EFFECT_BADGE_SIZE + 1, frameBg)

            // 获取效果图标的 Identifier
            val iconId = getEffectIcon(effect.identifier)

            // 使用 blitSprite 渲染效果图标
            val pose = ctx.pose()
            pose.pushMatrix()
            val scale = L.EFFECT_BADGE_SIZE / 18f  // 原生图标大小是 18x18
            pose.translate(bx.toFloat(), y.toFloat())
            pose.scale(scale, scale)
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, iconId, 0, 0, 18, 18)
            pose.popMatrix()

            // 右上角渲染罗马数字等级（amplifier >= 1 时显示，即 II 级及以上）
            if (effect.amplifier >= 1) {
                val roman = toRomanNumeral(effect.amplifier + 1)  // amplifier 0 = Level I, 1 = Level II, etc.
                // 缩小罗马数字渲染
                pose.pushMatrix()
                val numScale = 0.5f
                // 计算右上角位置
                val romanWidth = (font.width(roman) * numScale).toInt()
                val romanX = bx + L.EFFECT_BADGE_SIZE - romanWidth
                val romanY = y - 1
                pose.translate(romanX.toFloat(), romanY.toFloat())
                pose.scale(numScale, numScale)
                ctx.drawString(font, roman, 0, 0, (opacity shl 24) or 0xFFFFFF, true)
                pose.popMatrix()
            }
        }
    }

    /**
     * 根据效果标识符获取效果图标的 Identifier
     * 例如 "minecraft:speed" -> Identifier("minecraft", "mob_effect/speed")
     */
    private fun getEffectIcon(effectKey: String): Identifier {
        // 提取效果名称（去掉命名空间）
        val key = if (effectKey.contains(":")) {
            effectKey.substringAfter(":")
        } else {
            effectKey
        }
        // 原版效果图标在 GUI atlas 中的路径为 mob_effect/<effectKey>
        return Identifier.withDefaultNamespace("mob_effect/${key.lowercase()}")
    }

    /**
     * 将数字转换为罗马数字字符串
     */
    private fun toRomanNumeral(num: Int): String {
        if (num <= 0 || num > 10) return num.toString()
        return when (num) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            6 -> "VI"
            7 -> "VII"
            8 -> "VIII"
            9 -> "IX"
            10 -> "X"
            else -> num.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  辅助：渲染物品图标（16×16 缩放到指定大小的槽位）
    // ──────────────────────────────────────────────────────────────

    /**
     * 渲染一个小尺寸物品槽位（SLOT_SIZE x SLOT_SIZE）
     * 使用矩阵缩放将 16px 图标缩小
     */
    private fun renderSmallItemSlot(ctx: GuiGraphics, itemId: String?, x: Int, y: Int, size: Int, opacity: Int) {
        // 背景
        ctx.fill(x, y, x + size, y + size, CardLayout.slotBgColor(opacity))

        if (itemId == null) return
        val stack = resolveItemStack(itemId) ?: return

        // 缩放矩阵渲染
        val pose = ctx.pose()
        pose.pushMatrix()
        val scale = size / 16f
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        try {
            ctx.renderItem(stack, 0, 0)
        } catch (_: Exception) {
            // 物品渲染异常时静默忽略
        }
        pose.popMatrix()
    }

    /**
     * 渲染一个标准（14px）物品槽位，用于主副手
     */
    private fun renderItemSlot(ctx: GuiGraphics, itemId: String?, x: Int, y: Int, size: Int, opacity: Int) {
        ctx.fill(x, y, x + size, y + size, CardLayout.slotBgColor(opacity))

        if (itemId == null) return
        val stack = resolveItemStack(itemId) ?: return

        val pose = ctx.pose()
        pose.pushMatrix()
        val scale = size / 16f
        pose.translate(x.toFloat(), y.toFloat())
        pose.scale(scale, scale)
        try {
            ctx.renderItem(stack, 0, 0)
        } catch (_: Exception) {
            // 物品渲染异常时静默忽略
        }
        pose.popMatrix()
    }

    /**
     * 将物品 ID 字符串解析为 ItemStack
     * 例如 "minecraft:diamond_helmet" -> ItemStack(Items.DIAMOND_HELMET)
     */
    private fun resolveItemStack(itemId: String): ItemStack? {
        return try {
            val loc = Identifier.parse(itemId)
            val item = BuiltInRegistries.ITEM.getOptional(loc).orElse(null) ?: return null
            if (item == Items.AIR) return null
            ItemStack(item)
        } catch (_: Exception) {
            null
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  辅助：生命值颜色
    // ──────────────────────────────────────────────────────────────

    private fun getHealthColor(percent: Double): Int = when {
        percent > 0.6 -> 0x55FF55
        percent > 0.3 -> 0xFFFF55
        percent > 0.0 -> 0xFF5555
        else -> 0x555555
    }

    // ──────────────────────────────────────────────────────────────
    //  辅助：渲染旁观高亮框
    // ──────────────────────────────────────────────────────────────

    /**
     * 在头像周围渲染黄色高亮框
     */
    private fun renderSpectateHighlight(ctx: GuiGraphics, x: Int, y: Int, opacity: Int) {
        val highlightColor = (opacity shl 24) or 0xFFFF00  // 黄色
        val borderWidth = 2
        
        // 绘制四条边框线
        // 上边
        ctx.fill(x - borderWidth, y - borderWidth, x + L.AVATAR_SIZE + borderWidth, y, highlightColor)
        // 下边
        ctx.fill(x - borderWidth, y + L.AVATAR_SIZE, x + L.AVATAR_SIZE + borderWidth, y + L.AVATAR_SIZE + borderWidth, highlightColor)
        // 左边
        ctx.fill(x - borderWidth, y, x, y + L.AVATAR_SIZE, highlightColor)
        // 右边
        ctx.fill(x + L.AVATAR_SIZE, y, x + L.AVATAR_SIZE + borderWidth, y + L.AVATAR_SIZE, highlightColor)
    }
}
