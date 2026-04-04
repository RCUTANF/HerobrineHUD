package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.animation.PlayerAnimationManager
import com.RCUTANF.herobrinehud.client.HudConfig
import com.RCUTANF.herobrinehud.client.ClientTeamData
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
 * 负责渲染单张玩家信息卡片（竖向布局），包含：
 *  - 顶部：头像（左）+ 主副手物品（右）
 *  - 头像下方：名称
 *  - 名称下方：心形+血量（左）和盔甲图标+盔甲值（右）横向并列（缩小60%）
 *  - 其下：四个护甲槽位（居中）
 *  - 最下：效果徽章
 */
object PlayerCardRenderer {

    private val L = CardLayout
    private const val FULL_BODY_WIDTH = 15
    private const val FULL_BODY_HEIGHT = 30
    private const val FULL_BODY_Y_OFFSET = 9
    private const val NAME_ABOVE_BODY_Y_OFFSET = 2

    // ──────────────────────────────────────────────────────────────
    // 新增：Neo-Pixel 配色常量 (RGB)
    // ──────────────────────────────────────────────────────────────
    private const val COLOR_PANEL_BG = 0x171C24
    private const val COLOR_LINE = 0x3B4656
    private const val COLOR_TEXT = 0xEEF3F8
    private const val COLOR_HP = 0xFF5C5C
    private const val COLOR_HEADER_BG = 0x0D1015
    private const val COLOR_HEADER_BORDER = 0x2E3744
    private const val COLOR_LINE_SOFT = 0x576274

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
        renderCardBackground(ctx, cardX, cardY, teamColor, opacity)

        if (ClientTeamData.isSpectating(player.uuid)) {
            renderCardSpectateHighlight(ctx, cardX, cardY, opacity)
        }

        // 竖向布局：
        // 1. 顶部：全身像
        // 2. 名称（全身像上方，占据整个卡片宽度，居中）
        // 3. 血量（主副手下方）
        // 4. 效果徽章（底部，居中排列）或队伍名称（当没有效果时）

        val avatarX = cardX + L.AVATAR_X_OFFSET
        val bodyX = avatarX + (L.AVATAR_SIZE - FULL_BODY_WIDTH) / 2
        val bodyY = cardY + FULL_BODY_Y_OFFSET
        val handX = cardX + L.HAND_X_OFFSET
        val handY = cardY + L.HAND_Y_OFFSET

        // 1. 顶部：全身像 + 名称共用一个层次化背景框
        if (config.showAvatar) {
            renderAvatarNameFrame(ctx, bodyX, cardY + NAME_ABOVE_BODY_Y_OFFSET, bodyY, opacity, teamColor)
            renderAvatar(ctx, player, bodyX, bodyY, opacity)
        }

        // 保留主副手图标渲染（与现有布局兼容）
        if (config.showEquipment) {
            renderHandIcons(ctx, player, handX, handY, opacity)
        }

        // 2. 名称（全身像上方，占据整个卡片宽度，居中）
        val nameX = if (config.showAvatar) bodyX else cardX
        val nameWidth = if (config.showAvatar) FULL_BODY_WIDTH else L.CARD_WIDTH
        val nameY = cardY + NAME_ABOVE_BODY_Y_OFFSET
        renderName(ctx, player, nameX, nameY, nameWidth, opacity)

        // 3. 队伍名（名称下方）- 暂时不渲染
        // val teamNameY = cardY + L.TEAM_NAME_Y_OFFSET
        // renderTeamName(ctx, teamName, teamColor, nameX, teamNameY, opacity)

        // 4. 血量（主副手下方）
        if (config.showHealthNumber) {
            val healthArmorY = cardY + L.HEALTH_ARMOR_Y
            renderHealthValueBelowHands(ctx, player, handX, healthArmorY, opacity)
        }

        // 护甲槽位已移除，为全身像留出更大空间

        // 5. 效果徽章（底部，居中排列）或队伍名称（当没有效果时）
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
     * 渲染卡片背景
     * 
     * @param ctx    GuiGraphics 上下文
     * @param cardX  卡片左上角 X
     * @param cardY  卡片左上角 Y
     * @param teamColor 队伍颜色（HEX 格式）
     * @param opacity 不透明度 (0-255)
     */
    private fun renderCardBackground(ctx: GuiGraphics, cardX: Int, cardY: Int, teamColor: String, opacity: Int) {
        val alpha = (opacity * 0.92f).toInt().coerceIn(0, 255) // rgba(23, 28, 36, 0.92)
        val bgColor = (alpha shl 24) or COLOR_PANEL_BG

        // 绘制主背景
        ctx.fill(cardX, cardY, cardX + L.CARD_WIDTH, cardY + L.CARD_HEIGHT, bgColor)

        // 绘制顶部 Header 背景
        val headerAlpha = (opacity * 0.72f).toInt().coerceIn(0, 255)
        val headerBgColor = (headerAlpha shl 24) or COLOR_HEADER_BG
        val headerHeight = L.AVATAR_SIZE + L.AVATAR_Y_OFFSET * 2
        ctx.fill(cardX, cardY, cardX + L.CARD_WIDTH, cardY + headerHeight, headerBgColor)

        // 绘制 Header 底部分隔线
        val headerBorderColor = (opacity shl 24) or COLOR_HEADER_BORDER
        ctx.fill(cardX, cardY + headerHeight - 1, cardX + L.CARD_WIDTH, cardY + headerHeight, headerBorderColor)

        // 左侧重音线 (Accent Line)
        // 获取与本地玩家的关系颜色（此处改为直接使用玩家队伍颜色）
        val accentRgb = try {
            teamColor.trimStart('#').toInt(16) and 0xFFFFFF
        } catch (_: Exception) {
            0xAAAAAA
        }
        val accentColor = (opacity shl 24) or accentRgb
        val accentWidth = 4
        //ctx.fill(cardX, cardY, cardX + accentWidth, cardY + L.CARD_HEIGHT, accentColor)

        // 绘制卡片边框
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
        val borderColor = (opacity shl 24) or COLOR_LINE
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

    private fun renderAvatarNameFrame(ctx: GuiGraphics, x: Int, nameY: Int, bodyY: Int, opacity: Int, teamColor: String) {
        val frameX = x - 1
        val frameY = nameY - 1
        val frameW = FULL_BODY_WIDTH + 2
        val frameH = bodyY + FULL_BODY_HEIGHT - nameY + 2
        val splitY = bodyY - 1

        // 分层背景：顶部名字区更亮，主体预览区更深，接近 HTML 里 header + panel 的层次感
        val outerBg = ((opacity * 0.24f).toInt().coerceIn(0, 255) shl 24) or COLOR_PANEL_BG
        val nameBg = ((opacity * 0.30f).toInt().coerceIn(0, 255) shl 24) or COLOR_HEADER_BG
        val bodyBg = ((opacity * 0.45f).toInt().coerceIn(0, 255) shl 24) or COLOR_HEADER_BG
        val borderColor = ((opacity * 0.58f).toInt().coerceIn(0, 255) shl 24) or COLOR_LINE_SOFT
        val innerHighlight = ((opacity * 0.24f).toInt().coerceIn(0, 255) shl 24) or 0xFFFFFF
        val sectionLine = ((opacity * 0.40f).toInt().coerceIn(0, 255) shl 24) or COLOR_HEADER_BORDER
        val accentColor = ((opacity * 0.70f).toInt().coerceIn(0, 255) shl 24) or parseTeamColor(teamColor)

        ctx.fill(frameX, frameY, frameX + frameW, frameY + frameH, outerBg)
        ctx.fill(frameX + 1, frameY + 1, frameX + frameW - 1, splitY, nameBg)
        ctx.fill(frameX + 1, splitY, frameX + frameW - 1, frameY + frameH - 1, bodyBg)

        // 细淡外边框
        ctx.fill(frameX, frameY, frameX + frameW, frameY + 1, borderColor)
        ctx.fill(frameX, frameY + frameH - 1, frameX + frameW, frameY + frameH, borderColor)
        ctx.fill(frameX, frameY, frameX + 1, frameY + frameH, borderColor)
        ctx.fill(frameX + frameW - 1, frameY, frameX + frameW, frameY + frameH, borderColor)

        // 内高光 + 区域分隔线，提升层次
        //ctx.fill(frameX + 1, frameY + 1, frameX + frameW - 1, frameY + 2, innerHighlight)
        //ctx.fill(frameX + 1, splitY, frameX + frameW - 1, splitY + 1, sectionLine)

        // 左侧轻量队伍重音线
        //ctx.fill(frameX, frameY + 1, frameX + 1, frameY + frameH - 1, accentColor)
    }

    private fun parseTeamColor(teamColor: String): Int = try {
        teamColor.trimStart('#').toInt(16) and 0xFFFFFF
    } catch (_: Exception) {
        0xAAAAAA
    }

    // ──────────────────────────────────────────────────────────────
    //  头像
    // ──────────────────────────────────────────────────────────────

    private fun renderAvatar(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val client = Minecraft.getInstance()

        // 优先：从本地 SkinManager 获取并绘制全身像
        val uuid = runCatching { UUID.fromString(player.uuid) }.getOrNull()
        if (uuid != null) {
            try {
                val player = client.connection?.getPlayerInfo(uuid)
                val skinTexture = player?.let {
                    client.skinManager
                        .createLookup(player.profile, false)
                }
                    ?.get()
                    ?.body()
                    ?.texturePath()
                skinTexture?.let {
                    blitFullBody(ctx, it, x, y)
                    return
                }
            } catch (_: Exception) {
                // 皮肤未加载，继续 fallback
            }
        }

        // 最终 Fallback：纯色全身像占位 + 名称首字母
        val font = client.font
        ctx.fill(x, y, x + FULL_BODY_WIDTH, y + FULL_BODY_HEIGHT, (opacity shl 24) or 0x2D3A4A)
        val initial = player.name.take(1).uppercase()
        ctx.drawString(
            font, initial,
            x + (FULL_BODY_WIDTH - font.width(initial)) / 2,
            y + (FULL_BODY_HEIGHT - 8) / 2,
            (opacity shl 24) or 0xFFFFFF, false
        )
    }

    /**
     * 从标准皮肤贴图中绘制全身正面（底层 + 覆盖层）
     */
    private fun blitFullBody(ctx: GuiGraphics, texture: Identifier, x: Int, y: Int) {
        withPose(ctx, x.toFloat(), y.toFloat(), FULL_BODY_WIDTH / 16f, FULL_BODY_HEIGHT / 32f) {
            // Head
            blitSkinPart(ctx, texture, 4, 0, 8, 8, 8f, 8f, 8, 8)
            blitSkinPart(ctx, texture, 4, 0, 8, 8, 40f, 8f, 8, 8)
            // Body
            blitSkinPart(ctx, texture, 4, 8, 8, 12, 20f, 20f, 8, 12)
            blitSkinPart(ctx, texture, 4, 8, 8, 12, 20f, 36f, 8, 12)
            // Arms
            blitSkinPart(ctx, texture, 0, 8, 4, 12, 44f, 20f, 4, 12)
            blitSkinPart(ctx, texture, 0, 8, 4, 12, 44f, 36f, 4, 12)
            blitSkinPart(ctx, texture, 12, 8, 4, 12, 36f, 52f, 4, 12)
            blitSkinPart(ctx, texture, 12, 8, 4, 12, 52f, 52f, 4, 12)
            // Legs
            blitSkinPart(ctx, texture, 4, 20, 4, 12, 4f, 20f, 4, 12)
            blitSkinPart(ctx, texture, 4, 20, 4, 12, 4f, 36f, 4, 12)
            blitSkinPart(ctx, texture, 8, 20, 4, 12, 20f, 52f, 4, 12)
            blitSkinPart(ctx, texture, 8, 20, 4, 12, 4f, 52f, 4, 12)
        }
    }

    private fun blitSkinPart(
        ctx: GuiGraphics,
        texture: Identifier,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        u: Float,
        v: Float,
        regionWidth: Int,
        regionHeight: Int
    ) {
        ctx.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x,
            y,
            u,
            v,
            width,
            height,
            regionWidth,
            regionHeight,
            64,
            64
        )
    }

    // ──────────────────────────────────────────────────────────────
    //  名称
    // ──────────────────────────────────────────────────────────────

    private fun renderName(ctx: GuiGraphics, player: PlayerInfo, x: Int, y: Int, width: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val nameColor = if (player.isAlive) (opacity shl 24) or COLOR_TEXT else (opacity shl 24) or 0x888888
        val scale = 0.6f  // 增大文字（从 0.5f 提升到 0.6f）
        
        // 计算在缩放后能容纳的最大宽度
        val maxScaledWidth = minOf(L.NAME_MAX_WIDTH.toFloat(), width.toFloat()) / scale
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
            val textX = (width / scale / 2f - textWidth / 2f).toInt()
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
    //  血量行（主副手下方）
    // ──────────────────────────────────────────────────────────────

    // 仅保留血量，盔甲信息不再渲染
    private fun renderHealthValueBelowHands(ctx: GuiGraphics, player: PlayerInfo, handX: Int, y: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val iconSize = L.HEART_ICON_SIZE
        val scale = 0.6f

        withPose(ctx, handX.toFloat(), y.toFloat(), scale, scale) {
            val healthText = "${player.health.toInt()}"
            val heartTexture = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full")
            val leftX = 0

            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, heartTexture, leftX, 0, iconSize, iconSize)
            ctx.drawString(font, healthText, leftX + iconSize + L.ICON_TEXT_GAP, 0, (opacity shl 24) or COLOR_HP, true)
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
        val bgAlpha = (opacity * 0.4f).toInt().coerceIn(0, 255)
        val bgColor = (bgAlpha shl 24) or 0x000000
        ctx.fill(x, y, x + size, y + size, bgColor)

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
        val bgAlpha = (opacity * 0.4f).toInt().coerceIn(0, 255)
        val bgColor = (bgAlpha shl 24) or 0x000000
        ctx.fill(x, y, x + size, y + size, bgColor)

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
        val bgAlpha = (opacity * 0.4f).toInt().coerceIn(0, 255)
        val bgColor = (bgAlpha shl 24) or 0x000000
        ctx.fill(x, y, x + size, y + size, bgColor)

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
        val bgAlpha = (opacity * 0.4f).toInt().coerceIn(0, 255)
        val bgColor = (bgAlpha shl 24) or 0x000000
        ctx.fill(x, y, x + size, y + size, bgColor)

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
    private fun renderCardSpectateHighlight(ctx: GuiGraphics, cardX: Int, cardY: Int, opacity: Int) {
        val alpha = (opacity * 0.9f).toInt().coerceIn(0, 255)
        val highlightColor = (alpha shl 24) or 0xFFFFFF
        val borderWidth = 1

        ctx.fill(cardX - borderWidth, cardY - borderWidth, cardX + L.CARD_WIDTH + borderWidth, cardY, highlightColor)
        ctx.fill(cardX - borderWidth, cardY + L.CARD_HEIGHT, cardX + L.CARD_WIDTH + borderWidth, cardY + L.CARD_HEIGHT + borderWidth, highlightColor)
        ctx.fill(cardX - borderWidth, cardY, cardX, cardY + L.CARD_HEIGHT, highlightColor)
        ctx.fill(cardX + L.CARD_WIDTH, cardY, cardX + L.CARD_WIDTH + borderWidth, cardY + L.CARD_HEIGHT, highlightColor)
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
        val borderColor = (opacity shl 24) or COLOR_LINE_SOFT // Line Soft
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
