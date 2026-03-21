package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.animation.PlayerAnimationManager
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
     * @param ctx          GuiGraphics 上下文
     * @param player       玩家数据
     * @param cardX        卡片左上角 X
     * @param cardY        卡片左上角 Y
     * @param teamName     玩家所属队伍名称
     * @param teamColor    队伍颜色（HEX 格式，如 "#FF5555"）
     * @param opacity      不透明度 (0-255)
     * @param hotkeyNumber 快捷键编号 (1-9, 0)，null 表示无快捷键
     */
    fun renderCard(ctx: GuiGraphics, player: PlayerInfo, cardX: Int, cardY: Int, teamName: String, teamColor: String, opacity: Int, hotkeyNumber: Int? = null) {
        val config = HudConfig.data

        // 卡片背景：使用维度对应方块的纹理
        renderCardBackground(ctx, player, cardX, cardY, opacity)

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

        // 2. 名称（头像下方，占据整个卡片宽度，居中）
        val nameX = cardX
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

        // 6. 效果徽章（底部，居中排列）或队伍名称（当没有效果时）
        val effectsY = cardY + L.EFFECTS_Y
        if (config.showEffects && player.effects.isNotEmpty()) {
            val effectsX = cardX + 2
            val availableWidth = L.CARD_WIDTH - 4
            renderEffectBadges(ctx, player, effectsX, effectsY, availableWidth, opacity)
            // 当有效果时，在卡片右下角绘制快捷键编号
            if (hotkeyNumber != null) {
                renderHotkeyNumberAtBottomRight(ctx, hotkeyNumber, cardX, cardY, opacity)
            }
        } else {
            // 当没有效果时，在效果徽章的位置绘制队伍名称（带快捷键编号前缀）
            renderTeamNameAtEffectPosition(ctx, teamName, teamColor, cardX, effectsY, opacity, hotkeyNumber)
        }
        
        // 7. 渲染玩家变化动画（在所有内容之上）
        renderPlayerAnimations(ctx, player, cardX, cardY, opacity)
    }
    
    /**
     * 渲染玩家的所有活跃动画
     */
    private fun renderPlayerAnimations(ctx: GuiGraphics, player: PlayerInfo, cardX: Int, cardY: Int, opacity: Int) {
        val animations = PlayerAnimationManager.getAnimations(player.uuid)
        animations.forEach { animation ->
            animation.render(ctx, cardX, cardY, opacity)
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  卡片背景
    // ──────────────────────────────────────────────────────────────

    /**
     * 渲染卡片背景，使用维度对应方块的纹理
     * 纹理高度根据玩家血量百分比动态调整，从下往上填充
     * 
     * @param ctx    GuiGraphics 上下文
     * @param player 玩家数据
     * @param cardX  卡片左上角 X
     * @param cardY  卡片左上角 Y
     * @param opacity 不透明度 (0-255)
     */
    private fun renderCardBackground(ctx: GuiGraphics, player: PlayerInfo, cardX: Int, cardY: Int, opacity: Int) {
        val dim = player.dimension
        
        // 如果没有维度信息，使用纯色背景
        if (dim == null) {
            ctx.fill(cardX, cardY, cardX + L.CARD_WIDTH, cardY + L.CARD_HEIGHT, L.bgColor(opacity))
            drawCardBorder(ctx, cardX, cardY, opacity)
            return
        }
        
        // 通过枚举查找对应纹理路径
        val dimIcon = CardLayout.DimensionIcon.fromDimensionId(dim)
        
        // 如果找不到对应的维度图标，使用纯色背景
        if (dimIcon == null) {
            ctx.fill(cardX, cardY, cardX + L.CARD_WIDTH, cardY + L.CARD_HEIGHT, L.bgColor(opacity))
            drawCardBorder(ctx, cardX, cardY, opacity)
            return
        }
        
        // 先绘制半透明黑色底色（整个卡片）
        ctx.fill(cardX, cardY, cardX + L.CARD_WIDTH, cardY + L.CARD_HEIGHT, L.bgColor(opacity))
        
        // 计算血量百分比（0.0 - 1.0）
        val healthPercent = (player.health / player.maxHealth).coerceIn(0.0, 1.0)
        
        // 计算纹理应该占据的高度（从下往上）
        val textureHeight = (L.CARD_HEIGHT * healthPercent).toInt()
        
        // 如果血量为0，不绘制纹理
        if (textureHeight <= 0) {
            drawCardBorder(ctx, cardX, cardY, opacity)
            return
        }
        
        // 计算纹理绘制的起始Y坐标（从底部向上）
        val textureStartY = cardY + L.CARD_HEIGHT - textureHeight
        
        // 构建纹理标识符
        val textureLocation = Identifier.parse(dimIcon.textureId)
        
        // 平铺渲染方块纹理（16x16原始大小），只渲染血量对应的高度
        val tileSize = 16
        
        // 从纹理起始位置开始平铺
        var tileY = 0
        while (tileY < textureHeight) {
            var tileX = 0
            while (tileX < L.CARD_WIDTH) {
                // 计算实际渲染位置（卡片坐标 + 平铺偏移）
                val x0 = cardX + tileX
                val y0 = textureStartY + tileY
                
                // 计算当前tile的实际渲染尺寸（处理边缘裁剪）
                val renderWidth = tileSize.coerceAtMost(L.CARD_WIDTH - tileX)
                val renderHeight = tileSize.coerceAtMost(textureHeight - tileY)
                
                // 计算右下角坐标
                val x1 = x0 + renderWidth
                val y1 = y0 + renderHeight
                
                // 计算UV坐标（纹理坐标范围 0.0 - 1.0）
                val u0 = 0f
                val v0 = 0f
                val u1 = renderWidth.toFloat() / 16f
                val v1 = renderHeight.toFloat() / 16f
                
                // 使用 blit 方法渲染纹理
                ctx.blit(textureLocation, x0, y0, x1, y1, u0, u1, v0, v1)
                
                tileX += tileSize
            }
            tileY += tileSize
        }
        
        // 在纹理上方叠加一层半透明黑色，使纹理不会太亮（只覆盖纹理区域）
        val overlayAlpha = (opacity / 3).coerceIn(0, 255)
        val overlayColor = (overlayAlpha shl 24) or 0x000000
        ctx.fill(cardX, textureStartY, cardX + L.CARD_WIDTH, cardY + L.CARD_HEIGHT, overlayColor)
        
        // 绘制淡灰色边框
        drawCardBorder(ctx, cardX, cardY, opacity)
    }
    
    /**
     * 绘制卡片边框
     *
     * @param ctx    GuiGraphics 上下文
     * @param cardX  卡片左上角 X
     * @param cardY  卡片左上角 Y
     * @param opacity 不透明度 (0-255)
     */
    private fun drawCardBorder(ctx: GuiGraphics, cardX: Int, cardY: Int, opacity: Int) {
        val borderAlpha = (opacity * 2 / 3).coerceIn(0, 255)
        val borderColor = (borderAlpha shl 24) or 0x808080  // 灰色 RGB(128, 128, 128)
        val borderWidth = 1
        
        // 上边框
        ctx.fill(cardX, cardY, cardX + L.CARD_WIDTH, cardY + borderWidth, borderColor)
        // 下边框
        ctx.fill(cardX, cardY + L.CARD_HEIGHT - borderWidth, cardX + L.CARD_WIDTH, cardY + L.CARD_HEIGHT, borderColor)
        // 左边框
        ctx.fill(cardX, cardY, cardX + borderWidth, cardY + L.CARD_HEIGHT, borderColor)
        // 右边框
        ctx.fill(cardX + L.CARD_WIDTH - borderWidth, cardY, cardX + L.CARD_WIDTH, cardY + L.CARD_HEIGHT, borderColor)
    }

    // ──────────────────────────────────────────────────────────────
    //  头像
    // ──────────────────────────────────────────────────────────────

    private fun renderAvatar(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val client = Minecraft.getInstance()

        // 优先：从本地 SkinManager 获取
        val uuid = runCatching { UUID.fromString(player.uuid) }.getOrNull()
        if (uuid != null) {
            try {
                val player = client.level?.getPlayerByUUID(uuid)
                val skinTexture = player?.let {
                    client.skinManager
                        .createLookup(it.gameProfile, false)
                }
                    ?.get()
                    ?.body()
                    ?.texturePath()
                skinTexture?.let { blitHead(ctx, it, x, y) }
                return
            } catch (_: Exception) {
                // 皮肤未加载，继续 fallback
            }
        }

        // 降级使用服务端下发的头像 URL（AvatarTextureCache 异步下载并注册）
//        val avatarTexture: Identifier? = player.avatar?.let { AvatarTextureCache.getTexture(it) }
//
//        if (avatarTexture != null) {
//            blitHead(ctx, avatarTexture, x, y)
//            return
//        }

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
        val scale = 0.6f  // 增大文字（从 0.5f 提升到 0.6f）
        
        // 计算在缩放后能容纳的最大宽度
        val maxScaledWidth = L.NAME_MAX_WIDTH / scale
        val displayName = if (font.width(player.name) > maxScaledWidth) {
            // 截断并加省略号
            var truncated = player.name
            while (truncated.isNotEmpty() && font.width("$truncated…") > maxScaledWidth) {
                truncated = truncated.dropLast(1)
            }
            "$truncated…"
        } else {
            player.name
        }
        
        // 使用通用 pose 辅助简化缩放与居中逻辑
        withPose(ctx, x.toFloat(), y.toFloat(), scale, scale) {
            val textWidth = font.width(displayName)
            val textX = (L.CARD_WIDTH / scale / 2f - textWidth / 2f).toInt()
            ctx.drawString(font, displayName, textX, 0, nameColor, false)
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  队伍名称
    // ──────────────────────────────────────────────────────────────

    @Suppress("unused")
    private fun renderTeamName(ctx: GuiGraphics, teamName: String, teamColor: String, x: Int, y: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val rgb = try {
            teamColor.trimStart('#').toInt(16) and 0xFFFFFF
        } catch (_: Exception) {
            0xAAAAAA
        }
        val teamColorInt = (opacity shl 24) or rgb
        val scale = 0.5f

        withPose(ctx, x.toFloat(), y.toFloat(), scale, scale) {
            // 头像中心 X = x + L.AVATAR_SIZE / 2
            // 将缩放后的文本中心对齐到头像中心
            val textWidth = font.width(teamName)
            val textX = (L.AVATAR_SIZE / 2f - textWidth * scale / 2f).toInt()
            ctx.drawString(font, teamName, textX, 0, teamColorInt, false)
        }

    }

    /**
     * 在效果徽章位置渲染队伍名称（当没有效果时）
     * 使用队伍颜色，居中显示
     * 如果有快捷键编号，则在队伍名称前面添加编号
     */
    private fun renderTeamNameAtEffectPosition(ctx: GuiGraphics, teamName: String, teamColor: String, cardX: Int, y: Int, opacity: Int, hotkeyNumber: Int? = null) {
        val font = Minecraft.getInstance().font
        val rgb = try {
            teamColor.trimStart('#').toInt(16) and 0xFFFFFF
        } catch (_: Exception) {
            0xAAAAAA
        }
        val teamColorInt = (opacity shl 24) or rgb
        val hotkeyColor = (opacity shl 24) or 0xFFFF55  // 黄色
        val scale = 0.6f

        withPose(ctx, cardX.toFloat(), y.toFloat(), scale, scale) {
            // 构建显示文本：如果有快捷键编号，则添加前缀
            val displayText = if (hotkeyNumber != null) {
                "[$hotkeyNumber] $teamName"
            } else {
                teamName
            }
            
            // 将文本居中对齐到卡片中心
            val textWidth = font.width(displayText)
            val textX = (L.CARD_WIDTH / 2f - textWidth * scale / 2f).toInt()
            
            // 如果有快捷键编号，分段渲染（编号用黄色，队伍名用队伍颜色）
            if (hotkeyNumber != null) {
                val hotkeyText = "[$hotkeyNumber] "
                val hotkeyWidth = font.width(hotkeyText)
                ctx.drawString(font, hotkeyText, textX, 0, hotkeyColor, false)
                ctx.drawString(font, teamName, textX + hotkeyWidth, 0, teamColorInt, false)
            } else {
                ctx.drawString(font, displayText, textX, 0, teamColorInt, false)
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  快捷键编号
    // ──────────────────────────────────────────────────────────────

    /**
     * 在卡片右下角渲染快捷键编号（当有效果时使用）
     *
     * @param ctx          GuiGraphics 上下文
     * @param hotkeyNumber 快捷键编号 (1-9, 0)
     * @param cardX        卡片左上角 X
     * @param cardY        卡片左上角 Y
     * @param opacity      不透明度 (0-255)
     */
    private fun renderHotkeyNumberAtBottomRight(ctx: GuiGraphics, hotkeyNumber: Int, cardX: Int, cardY: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val hotkeyText = "[$hotkeyNumber]"
        val hotkeyColor = (opacity shl 24) or 0xFFFF55  // 黄色
        val scale = 0.5f
        
        // 计算右下角位置（留出一些边距）
        val margin = 2
        val textWidth = font.width(hotkeyText)
        val scaledTextWidth = (textWidth * scale).toInt()
        val scaledTextHeight = (8 * scale).toInt()  // 字体高度约为 8
        
        val x = cardX + L.CARD_WIDTH - scaledTextWidth - margin
        val y = cardY + L.CARD_HEIGHT - scaledTextHeight - margin

        withPose(ctx, x.toFloat(), y.toFloat(), scale, scale) {
            ctx.drawString(font, hotkeyText, 0, 0, hotkeyColor, true)  // 使用阴影增强可读性
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  生命值条
    // ──────────────────────────────────────────────────────────────

    @Suppress("unused", "DEPRECATION")
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
    //  护甲行（护甲值 + 四个护甲槽位）
    // ──────────────────────────────────────────────────────────────

    // 新增：竖向布局专用 - 横向并列心形图标+血量和盔甲图标+盔甲值（缩小尺寸）
    private fun renderHealthAndArmorValuesVertical(ctx: GuiGraphics, player: PlayerInfo, cardX: Int, y: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val iconSize = L.HEART_ICON_SIZE
        val scale = 0.6f  // 缩放到60%大小
        
        // 使用 withPose 简化矩阵管理
        withPose(ctx, cardX.toFloat(), y.toFloat(), scale, scale) {
            // 左侧：心形图标 + 血量文本
            val healthText = "${player.health.toInt()}"
            val heartTexture = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full")
            val leftX = 2

            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, heartTexture, leftX, 0, iconSize, iconSize)
            ctx.drawString(font, healthText, leftX + iconSize + L.ICON_TEXT_GAP, 0, (opacity shl 24) or 0xFFFFFF, false)

            // 右侧：盔甲图标 + 盔甲值
            val armorValueText = "${player.armor}"
            val armorIconX = 26  // 右侧位置
            // 渲染盔甲图标（不带灰色底纹）
            renderArmorIconOnly(ctx, "minecraft:iron_chestplate", armorIconX, 0, iconSize, opacity)
            ctx.drawString(font, armorValueText, armorIconX + iconSize + L.ICON_TEXT_GAP, 0, (opacity shl 24) or 0xFFFFFF, false)
        }
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
            renderSmallItemSlotWithBorder(ctx, itemId, slotX, y, L.SLOT_SIZE, opacity)
            slotX += L.SLOT_SIZE + L.SLOT_GAP
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  主副手图标（竖向排列）
    // ──────────────────────────────────────────────────────────────

    private fun renderHandIcons(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val eq = player.equipment
        // 主手在上方
        renderItemSlotWithBorder(ctx, eq.mainHand, x, y, L.HAND_SLOT_SIZE, opacity)
        // 副手在下方（紧贴主手）
        renderItemSlotWithBorder(ctx, eq.offHand, x, y + L.HAND_SLOT_SIZE + L.HAND_GAP, L.HAND_SLOT_SIZE, opacity)
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
            withPose(ctx, bx.toFloat(), y.toFloat(), L.EFFECT_BADGE_SIZE / 18f, L.EFFECT_BADGE_SIZE / 18f) {
                ctx.blitSprite(RenderPipelines.GUI_TEXTURED, iconId, 0, 0, 18, 18)
            }

            // 右上角渲染罗马数字等级（amplifier >= 1 时显示，即 II 级及以上）
            if (effect.amplifier >= 1) {
                val roman = toRomanNumeral(effect.amplifier + 1)  // amplifier 0 = Level I, 1 = Level II, etc.
                // 缩小罗马数字渲染
                val numScale = 0.35f  // 进一步缩小罗马数字
                // 计算右上角位置
                val romanWidth = (font.width(roman) * numScale).toInt()
                val romanX = bx + L.EFFECT_BADGE_SIZE - romanWidth
                val romanY = y - 1
                withPose(ctx, romanX.toFloat(), romanY.toFloat(), numScale, numScale) {
                    ctx.drawString(font, roman, 0, 0, (opacity shl 24) or 0xFFFFFF, true)
                }
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
    @Suppress("unused")
    private fun renderSmallItemSlot(ctx: GuiGraphics, itemId: String?, x: Int, y: Int, size: Int, opacity: Int) {
        // 背景
        ctx.fill(x, y, x + size, y + size, CardLayout.slotBgColor(opacity))

        if (itemId == null) return
        val stack = resolveItemStack(itemId) ?: return

        // 缩放矩阵渲染
        withPose(ctx, x.toFloat(), y.toFloat(), size / 16f, size / 16f) {
            try {
                ctx.renderItem(stack, 0, 0)
            } catch (_: Exception) {
                // 物品渲染异常时静默忽略
            }
        }
    }

    /**
     * 渲染一个标准（14px）物品槽位，用于主副手
     */
    @Suppress("unused")
    private fun renderItemSlot(ctx: GuiGraphics, itemId: String?, x: Int, y: Int, size: Int, opacity: Int) {
        ctx.fill(x, y, x + size, y + size, CardLayout.slotBgColor(opacity))

        if (itemId == null) return
        val stack = resolveItemStack(itemId) ?: return

        withPose(ctx, x.toFloat(), y.toFloat(), size / 16f, size / 16f) {
            try {
                ctx.renderItem(stack, 0, 0)
            } catch (_: Exception) {
                // 物品渲染异常时静默忽略
            }
        }
    }

    /**
     * 渲染带白色外框的小尺寸物品槽位（用于盔甲槽位）
     */
    private fun renderSmallItemSlotWithBorder(ctx: GuiGraphics, itemId: String?, x: Int, y: Int, size: Int, opacity: Int) {
        // 背景
        ctx.fill(x, y, x + size, y + size, CardLayout.slotBgColor(opacity))

        // 绘制细边框
        drawThinBorder(ctx, x, y, size, opacity)

        if (itemId == null) return
        val stack = resolveItemStack(itemId) ?: return

        // 缩放矩阵渲染物品
        withPose(ctx, x.toFloat(), y.toFloat(), size / 16f, size / 16f) {
            try {
                ctx.renderItem(stack, 0, 0)
            } catch (_: Exception) {
                // 物品渲染异常时静默忽略
            }
        }
    }

    /**
     * 渲染带白色外框的标准物品槽位（用于主副手）
     */
    private fun renderItemSlotWithBorder(ctx: GuiGraphics, itemId: String?, x: Int, y: Int, size: Int, opacity: Int) {
        // 背景
        ctx.fill(x, y, x + size, y + size, CardLayout.slotBgColor(opacity))

        // 绘制细边框
        drawThinBorder(ctx, x, y, size, opacity)

        if (itemId == null) return
        val stack = resolveItemStack(itemId) ?: return

        withPose(ctx, x.toFloat(), y.toFloat(), size / 16f, size / 16f) {
            try {
                ctx.renderItem(stack, 0, 0)
            } catch (_: Exception) {
                // 物品渲染异常时静默忽略
            }
        }
    }

    /**
     * 渲染盔甲图标（不带灰色底纹）
     */
    @Suppress("UNUSED_PARAMETER")
    private fun renderArmorIconOnly(ctx: GuiGraphics, itemId: String?, x: Int, y: Int, size: Int, opacity: Int) {
        if (itemId == null) return
        val stack = resolveItemStack(itemId) ?: return

        // 缩放矩阵渲染（不绘制背景）
        withPose(ctx, x.toFloat(), y.toFloat(), size / 16f, size / 16f) {
            try {
                ctx.renderItem(stack, 0, 0)
            } catch (_: Exception) {
                // 物品渲染异常时静默忽略
            }
        }
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

    // ──────────────────────────────────────────────────────────────
    //  Pose 与 边框 辅助函数（用于减少重复的 push/pop 与缩放代码）
    // ──────────────────────────────────────────────────────────────

    /**
     * 在指定的 transform 下执行绘制操作（会自动 push/pop 矩阵）
     */
    private inline fun withPose(ctx: GuiGraphics, tx: Float, ty: Float, sx: Float, sy: Float, block: (GuiGraphics) -> Unit) {
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate(tx, ty)
        pose.scale(sx, sy)
        try {
            block(ctx)
        } finally {
            pose.popMatrix()
        }
    }

    /**
     * 绘制细白色边框（通过 0.5 倍缩放实现更细的边框线），接收任意大小的槽位
     */
    private fun drawThinBorder(ctx: GuiGraphics, x: Int, y: Int, size: Int, opacity: Int) {
        val borderColor = (opacity shl 24) or 0xFFFFFF
        // 使用缩放到 0.5 来绘制更细的边框
        withPose(ctx, x.toFloat(), y.toFloat(), 0.5f, 0.5f) {
            val scaledSize = size * 2
            // 上边框
            ctx.fill(0, 0, scaledSize, 1, borderColor)
            // 下边框
            ctx.fill(0, scaledSize - 1, scaledSize, scaledSize, borderColor)
            // 左边框
            ctx.fill(0, 0, 1, scaledSize, borderColor)
            // 右边框
            ctx.fill(scaledSize - 1, 0, scaledSize, scaledSize, borderColor)
        }
    }
}
