package com.RCUTANF.herobrinehud.client

import com.RCUTANF.herobrinehud.data.PlayerInfo
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
 * 负责渲染单张玩家信息卡片，包含：
 *  - 左侧：头像 + 名称
 *  - 右侧第一行：生命值条 + 维度徽章
 *  - 右侧第二行：护甲值 + 四个护甲槽位图标
 *  - 右侧第三行：主副手图标 + 效果徽章
 */
object PlayerCardRenderer {

    private val L = CardLayout

    /**
     * 渲染完整的玩家卡片
     *
     * @param ctx       GuiGraphics 上下文
     * @param player    玩家数据
     * @param cardX     卡片左上角 X
     * @param cardY     卡片左上角 Y
     * @param opacity   不透明度 (0-255)
     */
    fun renderCard(ctx: GuiGraphics, player: PlayerInfo, cardX: Int, cardY: Int, opacity: Int) {
        val config = HudConfig.data

        // 卡片背景
        ctx.fill(cardX, cardY, cardX + L.CARD_WIDTH, cardY + L.CARD_HEIGHT, L.bgColor(opacity))

        // ── 左侧：头像 + 名称 ──────────────────────────────
        if (config.showAvatar) {
            renderAvatar(ctx, player, cardX + L.AVATAR_X_OFFSET, cardY + L.AVATAR_Y_OFFSET, opacity)
        }
        renderName(ctx, player, cardX + L.AVATAR_X_OFFSET, cardY + L.NAME_Y_OFFSET, opacity)

        // ── 右侧第一行：生命值条 + 维度 ──────────────────────
        val infoX = cardX + L.INFO_X
        renderHealthBar(ctx, player, infoX, cardY + L.ROW1_Y, opacity)
        if (config.showDimension) {
            renderDimensionBadge(ctx, player, cardX, cardY + L.ROW1_Y, opacity)
        }

        // ── 右侧第二行：护甲值 + 护甲槽 ──────────────────────
        if (config.showArmor) {
            renderArmorRow(ctx, player, infoX, cardY + L.ROW2_Y, opacity)
        }

        // ── 右侧第三行：主副手 + 效果徽章 ────────────────────
        if (config.showEquipment) {
            renderHandIcons(ctx, player, infoX, cardY + L.ROW3_Y, opacity)
        }
        if (config.showEffects) {
            val effectStartX = infoX + L.HAND_SLOT_SIZE * 2 + L.EFFECT_BADGE_GAP * 3 + 2
            renderEffectBadges(ctx, player, effectStartX, cardY + L.ROW3_Y, opacity)
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
                    .createLookup(com.mojang.authlib.GameProfile(uuid, player.name), false)
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
        ctx.drawString(font, displayName, x, y, nameColor, false)
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
        val font = Minecraft.getInstance().font
        val name = CardLayout.DIMENSION_NAMES[dim] ?: dim.substringAfterLast(':').take(5)
        val rgb = CardLayout.DIMENSION_COLORS[dim] ?: 0xAAAAAA
        val color = (opacity shl 24) or rgb

        val textWidth = font.width(name)
        val badgeX = cardX + L.CARD_WIDTH - textWidth - L.DIM_BADGE_X_FROM_RIGHT - 2
        ctx.drawString(font, name, badgeX, y, color, true)
    }

    // ──────────────────────────────────────────────────────────────
    //  护甲行（护甲值 + 四个护甲槽位）
    // ──────────────────────────────────────────────────────────────

    private fun renderArmorRow(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val font = Minecraft.getInstance().font

        // 护甲值数字
        val armorText = "\uD83D\uDEE1${player.armor}"   // 盾牌emoji + 数字（fallback为纯数字）
        val armorDisplay = "A:${player.armor}"
        ctx.drawString(font, armorDisplay, x, y + 1, (opacity shl 24) or 0xAAAAAA, false)

        // 四个护甲槽位
        val eq = player.equipment
        val armorItems = listOf(eq.helmet, eq.chestplate, eq.leggings, eq.boots)
        var slotX = x + L.ARMOR_VALUE_WIDTH + 2
        for (itemId in armorItems) {
            renderSmallItemSlot(ctx, itemId, slotX, y, L.SLOT_SIZE, opacity)
            slotX += L.SLOT_SIZE + L.SLOT_GAP
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  主副手图标
    // ──────────────────────────────────────────────────────────────

    private fun renderHandIcons(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val eq = player.equipment
        renderItemSlot(ctx, eq.mainHand, x, y, L.HAND_SLOT_SIZE, opacity)
        renderItemSlot(ctx, eq.offHand, x + L.HAND_SLOT_SIZE + L.EFFECT_BADGE_GAP, y, L.HAND_SLOT_SIZE, opacity)
    }

    // ──────────────────────────────────────────────────────────────
    //  效果徽章
    // ──────────────────────────────────────────────────────────────

    private fun renderEffectBadges(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        if (player.effects.isEmpty()) return
        val font = Minecraft.getInstance().font
        var bx = x
        val maxBadges = ((L.CARD_WIDTH - (x)) / (L.EFFECT_BADGE_SIZE + L.EFFECT_BADGE_GAP)).coerceAtLeast(1)
        for (effect in player.effects.take(maxBadges)) {
            val rgb = CardLayout.EFFECT_COLORS[effect.identifier] ?: 0x7777CC
            val bgColor = ((opacity * 3 / 4) shl 24) or rgb
            ctx.fill(bx, y, bx + L.EFFECT_BADGE_SIZE, y + L.EFFECT_BADGE_SIZE, bgColor)
            // 缩写首字母
            val abbr = effect.name.take(1).uppercase()
            ctx.drawString(
                font, abbr,
                bx + (L.EFFECT_BADGE_SIZE - font.width(abbr)) / 2,
                y + (L.EFFECT_BADGE_SIZE - 8) / 2,
                (opacity shl 24) or 0xFFFFFF, false
            )
            bx += L.EFFECT_BADGE_SIZE + L.EFFECT_BADGE_GAP
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
}

