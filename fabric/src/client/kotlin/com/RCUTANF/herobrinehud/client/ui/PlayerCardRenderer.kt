package com.RCUTANF.herobrinehud.client.ui

import com.RCUTANF.herobrinehud.client.animation.PlayerAnimationManager
import com.RCUTANF.herobrinehud.client.HudConfig
import com.RCUTANF.herobrinehud.client.ClientTeamData
import com.RCUTANF.herobrinehud.client.ConfigData
import com.RCUTANF.herobrinehud.data.PlayerInfo
import com.mojang.authlib.GameProfile
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.entity
import net.minecraft.client.gui.item
import net.minecraft.client.gui.text
import net.minecraft.client.player.RemotePlayer
import net.minecraft.client.renderer.entity.EntityRenderDispatcher
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.ceil
import java.util.UUID

/**
 * 玩家卡片渲染器
 *
 * 负责渲染单张玩家信息卡片（竖向布局），包含：
 *  - 顶部：全身像（左）+ 主副手物品（右）
 *  - 全身像上方：名称
 *  - 主副手下方：心形+血量
 *  - 最下：效果徽章
 */
object PlayerCardRenderer {

    // Runtime snapshot of resolved positions; CardLayout remains the single source of constants.
    private data class RenderLayoutSnapshot(
        val cardX: Int,
        val cardY: Int,
        val bodyAreaX: Int,
        val bodyAreaY: Int,
        val bodyAreaHeight: Int,
        val bodyX: Int,
        val bodyY: Int,
        val handX: Int,
        val handY: Int,
        val frameNameY: Int,
        val nameX: Int,
        val nameY: Int,
        val nameWidth: Int,
        val statsCenterX: Int,
        val healthY: Int,
        val healthRowLeftX: Int,
        val healthRowRightX: Int,
        val healthBarX: Int,
        val healthBarY: Int,
        val healthBarWidth: Int,
        val effectsX: Int,
        val effectsY: Int,
        val frameTop: Int,
        val frameBottom: Int,
        val showAvatar: Boolean,
        val showEquipment: Boolean,
        val showHealthNumber: Boolean,
        val showDimension: Boolean,
        val showEffects: Boolean
    )

    private val L = CardLayout
    private val previewPlayers = mutableMapOf<UUID, RemotePlayer>()

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
    private const val COLOR_BADGE_BG = 0x141B25
    private const val COLOR_BADGE_BORDER = 0x7E90A8
    private const val COLOR_BADGE_TEXT_BG = 0x0A0F16
    private const val COLOR_GROUND_FALLBACK = 0x3A4658
    private const val COLOR_HOTKEY_BADGE_BG = 0xF7C928
    private const val COLOR_HOTKEY_BADGE_HIGHLIGHT = 0xFFE88C
    private const val COLOR_HOTKEY_BADGE_BORDER = 0xA87800
    private const val COLOR_HOTKEY_BADGE_FOLD = 0xD79A00
    private const val COLOR_HOTKEY_TEXT = 0x202020
    private const val COLOR_HP_BAR_TRACK = 0x2A0E12
    private const val COLOR_HP_BAR_FILL = 0xFF4D4D
    private const val COLOR_HP_BAR_BORDER = 0x8C2A34
    private const val HEALTH_BAR_X_OFFSET = -1
    private const val HEALTH_BAR_Y_OFFSET = 0
    private const val HEALTH_BAR_SIDE_INSET = 2
    private const val HEALTH_VALUE_ROW_Y_OFFSET = 4

    private val defaultGroundTexture = Identifier.parse(CardLayout.DimensionIcon.OVERWORLD.textureId)
    private val vanillaEffectBadgeBackground = Identifier.withDefaultNamespace("hud/effect_background")

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
    fun renderCard(ctx: GuiGraphicsExtractor, player: PlayerInfo, cardX: Int, cardY: Int, teamName: String, teamColor: String, opacity: Int, hotkeyNumber: Int? = null) {
        val config = HudConfig.data

        val layout = buildLayout(cardX, cardY, config)

        // Stage 1: base panel and highlights.
        renderCardBackground(ctx, layout, opacity)
        if (ClientTeamData.isSpectating(player.uuid)) {
            renderCardSpectateHighlight(ctx, layout, opacity)
        }

        // Stage 2: avatar + equipment blocks.
        renderAvatarSection(ctx, player, layout, opacity)
        renderEquipmentSection(ctx, player, layout, opacity)

        // Stage 3: text, stats, and effects.
        if (layout.showHealthNumber) {
            renderHealthBar(ctx, player, layout, opacity)
        }
        renderName(ctx, player, teamColor, layout.nameX, layout.nameY, layout.nameWidth, opacity)
        if (layout.showHealthNumber) {
            renderHealthValue(ctx, player, layout, opacity)
        }
        if (layout.showEffects && player.effects.isNotEmpty()) {
            renderEffectBadgesVertical(ctx, player, layout.effectsX, layout.effectsY, opacity)
        }

        if (hotkeyNumber != null) {
            renderHotkeyNumberAtBottomRight(ctx, hotkeyNumber, layout, opacity)
        }

        // Stage 4: transient overlays (hotkey + animations).
        renderPlayerAnimations(ctx, player, layout, opacity)
    }

    private fun buildLayout(cardX: Int, cardY: Int, config: ConfigData): RenderLayoutSnapshot {
        val bodyAreaX = cardX + L.AVATAR_X_OFFSET
        val bodyAreaY = cardY + L.FULL_BODY_FRAME_Y_OFFSET
        val bodyAreaHeight = L.FULL_BODY_AREA_HEIGHT
        val frameBottom = bodyAreaY + bodyAreaHeight

        val bodyX = bodyAreaX + (L.FULL_BODY_AREA_WIDTH - L.FULL_BODY_WIDTH) / 2
        val bodyY = cardY + L.FULL_BODY_Y_OFFSET
        val handX = cardX + L.HAND_X_OFFSET
        val handY = cardY + L.HAND_Y_OFFSET
        val frameNameY = cardY + L.NAME_ABOVE_BODY_Y_OFFSET

        val nameX = if (config.showAvatar) bodyAreaX else cardX
        val nameWidth = if (config.showAvatar) L.FULL_BODY_AREA_WIDTH else L.CARD_WIDTH
        val nameY = if (config.showAvatar) frameNameY + 5 else frameNameY
        val healthRowLeftX = cardX + 2
        val healthRowRightX = if (config.showEffects) {
            cardX + L.EFFECTS_X - 2
        } else {
            cardX + L.CARD_WIDTH - 2
        }
        val healthBarX = bodyAreaX + HEALTH_BAR_X_OFFSET + HEALTH_BAR_SIDE_INSET
        val healthBarY = frameNameY + 1 + HEALTH_BAR_Y_OFFSET
        val healthBarWidth = (L.FULL_BODY_AREA_WIDTH - HEALTH_BAR_SIDE_INSET * 2).coerceAtLeast(6)

        return RenderLayoutSnapshot(
            cardX = cardX,
            cardY = cardY,
            bodyAreaX = bodyAreaX,
            bodyAreaY = bodyAreaY,
            bodyAreaHeight = bodyAreaHeight,
            bodyX = bodyX,
            bodyY = bodyY,
            handX = handX,
            handY = handY,
            frameNameY = frameNameY,
            nameX = nameX,
            nameY = nameY,
            nameWidth = nameWidth,
            statsCenterX = cardX + L.STATS_CENTER_X_OFFSET,
            healthY = cardY + L.HEALTH_Y_OFFSET,
            healthRowLeftX = healthRowLeftX,
            healthRowRightX = healthRowRightX,
            healthBarX = healthBarX,
            healthBarY = healthBarY,
            healthBarWidth = healthBarWidth,
            effectsX = cardX + L.EFFECTS_X,
            effectsY = cardY + L.EFFECTS_START_Y,
            frameTop = bodyAreaY,
            frameBottom = frameBottom,
            showAvatar = config.showAvatar,
            showEquipment = config.showEquipment,
            showHealthNumber = config.showHealthNumber,
            showDimension = config.showDimension,
            showEffects = config.showEffects
        )
    }

    private fun renderAvatarSection(ctx: GuiGraphicsExtractor, player: PlayerInfo, layout: RenderLayoutSnapshot, opacity: Int) {
        if (!layout.showAvatar) return

        renderAvatarNameFrame(
            ctx,
            layout.bodyAreaX,
            L.FULL_BODY_AREA_WIDTH,
            layout.bodyAreaY,
            layout.bodyAreaHeight,
            layout.frameNameY,
            opacity
        )
        renderAvatar(
            ctx,
            player,
            layout.bodyX,
            layout.bodyY,
            layout.frameTop,
            layout.frameBottom,
            opacity
        )
    }

    private fun renderEquipmentSection(ctx: GuiGraphicsExtractor, player: PlayerInfo, layout: RenderLayoutSnapshot, opacity: Int) {
        if (!layout.showEquipment) return
        renderHandIcons(ctx, player, layout.handX, layout.handY, opacity)
    }
    
    /**
     * 渲染玩家的所有活跃动画
     */
    private fun renderPlayerAnimations(ctx: GuiGraphicsExtractor, player: PlayerInfo, layout: RenderLayoutSnapshot, opacity: Int) {
        val animations = PlayerAnimationManager.getAnimations(player.uuid)
        animations.forEach { animation ->
            animation.render(ctx, layout.cardX, layout.cardY, opacity)
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  卡片背景
    // ──────────────────────────────────────────────────────────────

    /**
     * 渲染卡片背景
     * 
     * @param ctx    GuiGraphicsExtractor 上下文
     * @param cardX  卡片左上角 X
     * @param cardY  卡片左上角 Y
     * @param opacity 不透明度 (0-255)
     */
    private fun renderCardBackground(ctx: GuiGraphicsExtractor, layout: RenderLayoutSnapshot, opacity: Int) {
        val cardX = layout.cardX
        val cardY = layout.cardY
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
        //ctx.fill(cardX, cardY + headerHeight - 1, cardX + L.CARD_WIDTH, cardY + headerHeight, headerBorderColor)

        // 绘制卡片边框
        drawCardBorder(ctx, cardX, cardY, opacity)
    }
    
    /**
     * 绘制卡片边框
     *
     * @param ctx    GuiGraphicsExtractor 上下文
     * @param cardX  卡片左上角 X
     * @param cardY  卡片左上角 Y
     * @param opacity 不透明度 (0-255)
     */
    private fun drawCardBorder(ctx: GuiGraphicsExtractor, cardX: Int, cardY: Int, opacity: Int) {
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

    private fun renderAvatarNameFrame(
        ctx: GuiGraphicsExtractor,
        areaX: Int,
        areaWidth: Int,
        areaY: Int,
        areaHeight: Int,
        nameY: Int,
        opacity: Int
    ) {
        val frameX = areaX - 1
        val frameY = nameY - 1
        val frameW = areaWidth + 2
        val frameH = areaY + areaHeight - nameY + 2
        val splitY = areaY - 1

        // 分层背景：顶部名字区更亮，主体预览区更深，接近 HTML 里 header + panel 的层次感
        val outerBg = ((opacity * 0.24f).toInt().coerceIn(0, 255) shl 24) or COLOR_PANEL_BG
        val nameBg = ((opacity * 0.30f).toInt().coerceIn(0, 255) shl 24) or COLOR_HEADER_BG
        val bodyBg = ((opacity * 0.45f).toInt().coerceIn(0, 255) shl 24) or COLOR_HEADER_BG
        val borderColor = ((opacity * 0.58f).toInt().coerceIn(0, 255) shl 24) or COLOR_LINE_SOFT

        ctx.fill(frameX, frameY, frameX + frameW, frameY + frameH, outerBg)
        ctx.fill(frameX + 1, frameY + 1, frameX + frameW - 1, splitY, nameBg)
        ctx.fill(frameX + 1, splitY, frameX + frameW - 1, frameY + frameH - 1, bodyBg)

        // 细淡外边框
        //ctx.fill(frameX, frameY, frameX + frameW, frameY + 1, borderColor)
        ctx.fill(frameX, frameY + frameH - 1, frameX + frameW, frameY + frameH, borderColor)
        //ctx.fill(frameX, frameY, frameX + 1, frameY + frameH, borderColor)
        ctx.fill(frameX + frameW - 1, frameY, frameX + frameW, frameY + frameH, borderColor)
    }

    private fun parseTeamColor(teamColor: String): Int = try {
        teamColor.trimStart('#').toInt(16) and 0xFFFFFF
    } catch (_: Exception) {
        0xAAAAAA
    }

    // ──────────────────────────────────────────────────────────────
    //  全身像
    // ──────────────────────────────────────────────────────────────

    private fun renderAvatar(
        ctx: GuiGraphicsExtractor,
        player: PlayerInfo,
        x: Int,
        y: Int,
        frameTop: Int,
        frameBottom: Int,
        opacity: Int
    ) {
        renderAvatarGround(ctx, player, x, y, frameTop, frameBottom, opacity)

        if (renderAvatar3D(ctx, player, x, y, frameBottom)) {
            return
        }

        renderAvatarFallback2D(ctx, player, x, y, opacity)
    }

    private fun renderAvatarGround(
        ctx: GuiGraphicsExtractor,
        player: PlayerInfo,
        x: Int,
        y: Int,
        frameTop: Int,
        frameBottom: Int,
        opacity: Int
    ) {
        val groundWidth = L.AVATAR_GROUND_WIDTH
        val groundHeight = L.AVATAR_GROUND_HEIGHT
        val groundX = x + (L.FULL_BODY_WIDTH - groundWidth) / 2
        val groundY = y + L.FULL_BODY_HEIGHT - L.AVATAR_GROUND_Y_FROM_BOTTOM - groundHeight

        val groundTexture = player.dimension
            ?.let(CardLayout.DimensionIcon::fromDimensionId)
            ?.let { runCatching { Identifier.parse(it.textureId) }.getOrNull() }
            ?: defaultGroundTexture

        val shadowColor = (((opacity * 0.24f).toInt().coerceIn(0, 255)) shl 24)
        val topEdgeColor = (((opacity * 0.45f).toInt().coerceIn(0, 255)) shl 24) or 0xC8D2DD
        val bottomEdgeColor = (((opacity * 0.35f).toInt().coerceIn(0, 255)) shl 24) or 0x11161F

        val groundTop = groundY.coerceAtLeast(frameTop)
        val groundBottom = (groundY + groundHeight).coerceAtMost(frameBottom)
        if (groundTop >= groundBottom) return

        val visibleHeight = groundBottom - groundTop
        val vOffset = (groundTop - groundY).toFloat()

        val shadowTop = (groundY + groundHeight - 1).coerceAtLeast(frameTop)
        val shadowBottom = (groundY + groundHeight + 2).coerceAtMost(frameBottom)
        if (shadowTop < shadowBottom) {
            // Clip the contact shadow to the full-body frame, not the avatar body box.
            ctx.fill(groundX - 1, shadowTop, groundX + groundWidth + 1, shadowBottom, shadowColor)
        }

        val drewTexture = runCatching {
            ctx.blit(
                RenderPipelines.GUI_TEXTURED,
                groundTexture,
                groundX,
                groundTop,
                0f,
                vOffset,
                groundWidth,
                visibleHeight,
                16,
                16
            )
        }.isSuccess

        if (!drewTexture) {
            val fallbackColor = (((opacity * 0.70f).toInt().coerceIn(0, 255)) shl 24) or COLOR_GROUND_FALLBACK
            ctx.fill(groundX, groundTop, groundX + groundWidth, groundBottom, fallbackColor)
        }

        if (groundTop <= groundY) {
            ctx.fill(groundX, groundTop, groundX + groundWidth, (groundTop + 1).coerceAtMost(groundBottom), topEdgeColor)
        }
        if (groundBottom >= groundY + groundHeight) {
            ctx.fill(groundX, (groundBottom - 1).coerceAtLeast(groundTop), groundX + groundWidth, groundBottom, bottomEdgeColor)
        }
    }

    /**
     * 使用简化版实体渲染：固定正面朝向，不跟随鼠标。
     */
    private fun renderAvatar3D(ctx: GuiGraphicsExtractor, player: PlayerInfo, x: Int, y: Int, frameBottom: Int): Boolean {
        val preview = getOrCreatePreviewPlayer(player) ?: return false

        return runCatching {
            val rotation = Quaternionf().rotateZ(Math.PI.toFloat())
            val cameraAngle = Quaternionf()
            val state = extractRenderState(preview)

            if (state is LivingEntityRenderState) {
                state.bodyRot = 180.0F
                state.yRot = 0.0F
                state.xRot = 0.0F
                if (state.pose == Pose.FALL_FLYING) {
                    state.xRot = 0.0F
                }

                state.boundingBoxWidth /= state.scale
                state.boundingBoxHeight /= state.scale
                state.boundingBoxWidth *= 0.88F
                state.scale = 1.0F
            }

            val translation = Vector3f(0.0F, state.boundingBoxHeight / 2.0F + 0.01F, 0.0F)
            val scale = (L.FULL_BODY_HEIGHT * 0.6f).coerceAtLeast(1f)
            val clipTop = y - 3
            // Bottom clip follows the full-body frame bottom rather than the avatar's feet box.
            val clipBottom = frameBottom
            suppressEntityNameTag(state)
            ctx.entity(state, scale, translation, rotation, cameraAngle, x, clipTop, x + L.FULL_BODY_WIDTH, clipBottom)
        }.isSuccess
    }

    private fun suppressEntityNameTag(state: EntityRenderState) {
        val booleanFields = setOf("showNameTag", "shouldShowName", "nameTagVisible", "renderNameTag", "displayNameVisible")
        val textFields = setOf("nameTag", "displayName", "name", "customName", "nameTagText")

        var type: Class<*>? = state.javaClass
        while (type != null && type != Any::class.java) {
            type.declaredFields.forEach { field ->
                runCatching {
                    field.isAccessible = true
                    when {
                        field.type == java.lang.Boolean.TYPE && field.name in booleanFields -> field.setBoolean(state, false)
                        !field.type.isPrimitive && field.name in textFields -> field.set(state, null)
                    }
                }
            }
            type = type.superclass
        }
    }

    private fun getOrCreatePreviewPlayer(player: PlayerInfo): RemotePlayer? {
        val client = Minecraft.getInstance()
        val level = client.level ?: return null
        val uuid = runCatching { UUID.fromString(player.uuid) }.getOrNull() ?: return null

        val profileName = player.name.takeIf { it.isNotBlank() } ?: "Player"
        val remote = previewPlayers[uuid]?.takeIf { it.level() == level }
            ?: RemotePlayer(level, GameProfile(uuid, profileName)).also { previewPlayers[uuid] = it }

        applyEquipment(remote, player)
        remote.setOnGround(true)
        remote.setShiftKeyDown(false)
        remote.isSprinting = false
        remote.isSwimming = false
        return remote
    }

    private fun applyEquipment(remote: RemotePlayer, player: PlayerInfo) {
        val eq = player.equipment
        remote.setItemSlot(EquipmentSlot.HEAD, resolveItemStack(eq.helmet) ?: ItemStack.EMPTY)
        remote.setItemSlot(EquipmentSlot.CHEST, resolveItemStack(eq.chestplate) ?: ItemStack.EMPTY)
        remote.setItemSlot(EquipmentSlot.LEGS, resolveItemStack(eq.leggings) ?: ItemStack.EMPTY)
        remote.setItemSlot(EquipmentSlot.FEET, resolveItemStack(eq.boots) ?: ItemStack.EMPTY)
        remote.setItemSlot(EquipmentSlot.MAINHAND, resolveItemStack(eq.mainHand) ?: ItemStack.EMPTY)
        remote.setItemSlot(EquipmentSlot.OFFHAND, resolveItemStack(eq.offHand) ?: ItemStack.EMPTY)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractRenderState(entity: LivingEntity): EntityRenderState {
        val dispatcher: EntityRenderDispatcher = Minecraft.getInstance().entityRenderDispatcher
        val renderer = dispatcher.getRenderer(entity) as EntityRenderer<LivingEntity, EntityRenderState>
        val state = renderer.createRenderState()
        state.lightCoords = 15728880
        state.shadowPieces.clear()
        state.outlineColor = 0
        return state
    }

    private fun renderAvatarFallback2D(ctx: GuiGraphicsExtractor, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val client = Minecraft.getInstance()

        // 优先：从本地 SkinManager 获取并绘制全身像
        val uuid = runCatching { UUID.fromString(player.uuid) }.getOrNull()
        if (uuid != null) {
            try {
                val playerInfo = client.connection?.getPlayerInfo(uuid)
                val skinTexture = playerInfo?.let {
                    client.skinManager
                        .createLookup(playerInfo.profile, false)
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
        ctx.fill(x, y, x + L.FULL_BODY_WIDTH, y + L.FULL_BODY_HEIGHT, (opacity shl 24) or 0x2D3A4A)
        val initial = player.name.take(1).uppercase()
        ctx.text(
            font, initial,
            x + (L.FULL_BODY_WIDTH - font.width(initial)) / 2,
            y + (L.FULL_BODY_HEIGHT - 8) / 2,
            (opacity shl 24) or 0xFFFFFF, false
        )
    }

    /**
     * 从标准皮肤贴图中绘制全身正面（底层 + 覆盖层）
     */
    private fun blitFullBody(ctx: GuiGraphicsExtractor, texture: Identifier, x: Int, y: Int) {
        withPose(ctx, x.toFloat(), y.toFloat(), L.FULL_BODY_WIDTH / 16f, L.FULL_BODY_HEIGHT / 32f) {
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
        ctx: GuiGraphicsExtractor,
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

    private fun renderName(ctx: GuiGraphicsExtractor, player: PlayerInfo, teamColor: String, x: Int, y: Int, width: Int, opacity: Int) {
        val font = Minecraft.getInstance().font
        val nameColor = (opacity shl 24) or parseTeamColor(teamColor)
        val scale = 0.4f  // 增大文字（从 0.5f 提升到 0.6f）

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
            ctx.text(font, displayName, textX, 0, nameColor, false)
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  队伍名称
    // ──────────────────────────────────────────────────────────────

    /**
     * 在效果徽章位置渲染队伍名称（当没有效果时）
     * 使用队伍颜色，居中显示
     * 如果有快捷键编号，则在队伍名称前面添加编号
     */
    private fun renderTeamNameAtEffectPosition(ctx: GuiGraphicsExtractor, teamName: String, teamColor: String, cardX: Int, y: Int, opacity: Int, hotkeyNumber: Int? = null) {
        val font = Minecraft.getInstance().font
        val rgb = parseTeamColor(teamColor)
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
                ctx.text(font, hotkeyText, textX, 0, hotkeyColor, false)
                ctx.text(font, teamName, textX + hotkeyWidth, 0, teamColorInt, false)
            } else {
                ctx.text(font, displayText, textX, 0, teamColorInt, false)
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  快捷键编号
    // ──────────────────────────────────────────────────────────────

    /**
     * 在卡片右下角渲染快捷键编号（当有效果时使用）
     *
     * @param ctx          GuiGraphicsExtractor 上下文
     * @param hotkeyNumber 快捷键编号 (1-9, 0)
     * @param cardX        卡片左上角 X
     * @param cardY        卡片左上角 Y
     * @param opacity      不透明度 (0-255)
     */
    private fun renderHotkeyNumberAtBottomRight(ctx: GuiGraphicsExtractor, hotkeyNumber: Int, layout: RenderLayoutSnapshot, opacity: Int) {
        val font = Minecraft.getInstance().font
        val hotkeyText = hotkeyNumber.toString()
        val badgeScale = 0.5f

        val textWidth = font.width(hotkeyText)
        val rawBadgeWidth = (textWidth + 8).coerceAtLeast(12)
        val rawBadgeHeight = 12
        val badgeWidth = (rawBadgeWidth * badgeScale).toInt().coerceAtLeast(1)
        val badgeHeight = (rawBadgeHeight * badgeScale).toInt().coerceAtLeast(1)
        val x = layout.cardX + L.CARD_WIDTH - badgeWidth - L.HOTKEY_MARGIN_RIGHT
        val y = layout.cardY + L.CARD_HEIGHT - badgeHeight - L.HOTKEY_MARGIN_BOTTOM

        val shadowColor = (((opacity * 0.35f).toInt().coerceIn(0, 255)) shl 24)
        val badgeColor = (((opacity * 0.96f).toInt().coerceIn(0, 255)) shl 24) or COLOR_HOTKEY_BADGE_BG
        val highlightColor = (((opacity * 0.98f).toInt().coerceIn(0, 255)) shl 24) or COLOR_HOTKEY_BADGE_HIGHLIGHT
        val borderColor = (((opacity * 0.98f).toInt().coerceIn(0, 255)) shl 24) or COLOR_HOTKEY_BADGE_BORDER
        val foldColor = (((opacity * 0.98f).toInt().coerceIn(0, 255)) shl 24) or COLOR_HOTKEY_BADGE_FOLD
        val textColor = (((opacity * 0.98f).toInt().coerceIn(0, 255)) shl 24) or COLOR_HOTKEY_TEXT

        withPose(ctx, x.toFloat(), y.toFloat(), badgeScale, badgeScale) {
            // soft drop-shadow to lift the badge off the card background
            ctx.fill(1, 1, rawBadgeWidth + 1, rawBadgeHeight + 1, shadowColor)
            ctx.fill(0, 0, rawBadgeWidth, rawBadgeHeight, badgeColor)

            // top-left highlight + bottom-right border for a modern raised look
            ctx.fill(0, 0, rawBadgeWidth, 1, highlightColor)
            ctx.fill(0, 0, 1, rawBadgeHeight, highlightColor)
            ctx.fill(0, rawBadgeHeight - 1, rawBadgeWidth, rawBadgeHeight, borderColor)
            ctx.fill(rawBadgeWidth - 1, 0, rawBadgeWidth, rawBadgeHeight, borderColor)

            // folded bottom-right corner (pixel-triangle rows)
            val foldSize = 4.coerceAtMost(rawBadgeWidth / 2).coerceAtMost(rawBadgeHeight / 2)
            for (row in 0 until foldSize) {
                val rowY = rawBadgeHeight - foldSize + row
                val rowX = rawBadgeWidth - foldSize + row
                ctx.fill(rowX, rowY, rawBadgeWidth, rowY + 1, foldColor)
            }

            val textX = (rawBadgeWidth - textWidth) / 2
            val textY = (rawBadgeHeight - font.lineHeight) / 2
            ctx.text(font, hotkeyText, textX, textY, textColor, false)
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  血量行（主副手下方）
    // ──────────────────────────────────────────────────────────────

    // 血量条：简洁纯色深红样式
    private fun renderHealthBar(ctx: GuiGraphicsExtractor, player: PlayerInfo, layout: RenderLayoutSnapshot, opacity: Int) {
        val clampedHealth = player.health.coerceAtLeast(0.0)
        val clampedMaxHealth = player.maxHealth.coerceAtLeast(1.0)
        val healthRatio = (clampedHealth / clampedMaxHealth).toFloat().coerceIn(0f, 1f)

        val barHeight = 2
        val barX = layout.healthBarX
        val barWidth = layout.healthBarWidth.coerceAtLeast(8)
        val barY = layout.healthBarY

        drawHealthBar(ctx, barX, barY, barWidth, barHeight, healthRatio, opacity)
    }

    // 底部血量值：心形与数字共用同一垂直偏移。
    private fun renderHealthValue(ctx: GuiGraphicsExtractor, player: PlayerInfo, layout: RenderLayoutSnapshot, opacity: Int) {
        val font = Minecraft.getInstance().font
        val startX = layout.healthRowLeftX
        val iconSize = L.HEART_ICON_SIZE.coerceAtLeast(6)
        val scale = 0.6f
        val textColor = (opacity shl 24) or COLOR_HP

        val clampedHealth = player.health.coerceAtLeast(0.0)
        val healthText = clampedHealth.toInt().toString()
        val contentGap = L.ICON_TEXT_GAP

        withPose(ctx, startX.toFloat(), layout.healthY.toFloat(), scale, scale) {
            val heartTexture = Identifier.fromNamespaceAndPath("minecraft", "hud/heart/full")
            val leftX = 0
            val iconY = HEALTH_VALUE_ROW_Y_OFFSET
            val textX = leftX + iconSize + contentGap
            val textY = ((iconSize - font.lineHeight) / 2).coerceAtLeast(0) + HEALTH_VALUE_ROW_Y_OFFSET

            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, heartTexture, leftX, iconY, iconSize, iconSize)
            ctx.text(font, healthText, textX, textY, textColor, true)
        }
    }

    private fun drawHealthBar(ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, ratio: Float, opacity: Int) {
        if (width <= 0 || height <= 0) return

        val trackColor = (((opacity * 0.50f).toInt().coerceIn(0, 255)) shl 24) or COLOR_HP_BAR_TRACK
        val fillColor = (((opacity * 0.98f).toInt().coerceIn(0, 255)) shl 24) or COLOR_HP_BAR_FILL
        val borderColor = (((opacity * 0.92f).toInt().coerceIn(0, 255)) shl 24) or COLOR_HP_BAR_BORDER

        ctx.fill(x, y, x + width, y + height, trackColor)

        val innerX = x
        val innerY = y
        val innerW = width.coerceAtLeast(1)
        val innerH = height.coerceAtLeast(1)

        val rawFillWidth = (innerW * ratio).toInt()
        val fillWidth = if (ratio > 0f) rawFillWidth.coerceAtLeast(1) else 0
        if (fillWidth > 0) {
            val fillRight = (innerX + fillWidth).coerceAtMost(innerX + innerW)
            ctx.fill(innerX, innerY, fillRight, innerY + innerH, fillColor)
        }

        // Draw a 0.5-scale thin frame as the final pass so it stays visible over the fill.
        drawThinRectBorderWithColor(ctx, x, y, width, height, borderColor)
    }

    // ──────────────────────────────────────────────────────────────
    //  主副手图标（竖向排列）
    // ──────────────────────────────────────────────────────────────

    private fun renderHandIcons(ctx: GuiGraphicsExtractor, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        val eq = player.equipment
        // 主手在上方
        renderItemSlotWithBorder(ctx, eq.mainHand, x, y, opacity)
        // 副手在下方（紧贴主手）
        renderItemSlotWithBorder(ctx, eq.offHand, x, y + L.HAND_SLOT_SIZE + L.HAND_GAP, opacity)
    }

    // ──────────────────────────────────────────────────────────────
    //  效果徽章
    // ──────────────────────────────────────────────────────────────

    private fun renderEffectBadgesVertical(ctx: GuiGraphicsExtractor, player: PlayerInfo, x: Int, y: Int, opacity: Int) {
        if (player.effects.isEmpty()) return
        val font = Minecraft.getInstance().font
        val badgeStep = L.EFFECT_BADGE_SIZE + L.EFFECT_BADGE_GAP + 2  // 每个徽章占用的高度（含外框）
        val availableHeight = (L.CARD_HEIGHT - L.EFFECTS_START_Y - 2).coerceAtLeast(badgeStep)
        val maxBadges = (availableHeight / badgeStep).coerceAtLeast(1)

        for ((index, effect) in player.effects.take(maxBadges).withIndex()) {
            val by = y + index * badgeStep
            val badgeLeft = x - 1
            val badgeTop = by - 1
            val badgeSize = L.EFFECT_BADGE_SIZE + 2

            renderVanillaEffectBadgeBackground(ctx, badgeLeft, badgeTop, badgeSize, opacity)

            // 获取效果图标的 Identifier
            val iconId = getEffectIcon(effect.identifier)

            // 使用 blitSprite 渲染效果图标
            val iconScale = (L.EFFECT_BADGE_SIZE + 1f) / 18f
            withPose(ctx, x - 0.5f, by - 0.5f, iconScale, iconScale) {
                ctx.blitSprite(RenderPipelines.GUI_TEXTURED, iconId, 0, 0, 18, 18)
            }

            // 右上角渲染罗马数字等级（amplifier >= 1 时显示，即 II 级及以上）
            if (effect.amplifier >= 1) {
                val roman = toRomanNumeral(effect.amplifier + 1)  // amplifier 0 = Level I, 1 = Level II, etc.
                val numScale = 0.2f
                val scaledTextWidth = ceil(font.width(roman) * numScale).toInt().coerceAtLeast(1)
                val scaledTextHeight = ceil(font.lineHeight * numScale).toInt().coerceAtLeast(1)
                // Align to the badge outer frame so the numeral appears truly top-right.
                val badgeRight = x + L.EFFECT_BADGE_SIZE + 1
                val badgeBottom = by + L.EFFECT_BADGE_SIZE + 1
                val inset = 0
                val romanX = (badgeRight - scaledTextWidth - inset).coerceAtLeast(badgeLeft + inset)
                val romanY = (badgeTop + inset).coerceAtMost(badgeBottom - scaledTextHeight - inset)

                val numeralBg = ((opacity * 0.80f).toInt().coerceIn(0, 255) shl 24) or COLOR_BADGE_TEXT_BG
                val bgPadX = 0
                val bgLeft = (romanX - bgPadX).coerceAtLeast(badgeLeft)
                val bgTop = romanY
                val bgRight = (romanX + scaledTextWidth + bgPadX).coerceAtMost(badgeRight)
                val bgBottom = (romanY + scaledTextHeight).coerceAtMost(badgeBottom)
                ctx.fill(bgLeft, bgTop, bgRight, bgBottom, numeralBg)

                withPose(ctx, romanX.toFloat(), romanY.toFloat(), numScale, numScale) {
                    ctx.text(font, roman, 0, 0, (opacity shl 24) or 0xFFFFFF, true)
                }
            }
        }
    }

    private fun renderVanillaEffectBadgeBackground(ctx: GuiGraphicsExtractor, x: Int, y: Int, size: Int, opacity: Int) {
        val drewVanilla = runCatching {
            ctx.blitSprite(RenderPipelines.GUI_TEXTURED, vanillaEffectBadgeBackground, x, y, size, size)
        }.isSuccess

        if (!drewVanilla) {
            val shadowColor = ((opacity * 0.36f).toInt().coerceIn(0, 255) shl 24)
            val panelColor = ((opacity * 0.92f).toInt().coerceIn(0, 255) shl 24) or COLOR_BADGE_BG
            val borderColor = ((opacity * 0.95f).toInt().coerceIn(0, 255) shl 24) or COLOR_BADGE_BORDER

            ctx.fill(x, y, x + size + 1, y + size + 1, shadowColor)
            ctx.fill(x, y, x + size, y + size, panelColor)
            drawThinBorderWithColor(ctx, x, y, size, borderColor)
            return
        }

        // Sprite backgrounds are opaque; darken it as opacity decreases to keep fade behavior close to the rest of the card.
        val fadeMaskAlpha = ((255 - opacity) * 0.75f).toInt().coerceIn(0, 255)
        if (fadeMaskAlpha > 0) {
            ctx.fill(x, y, x + size, y + size, (fadeMaskAlpha shl 24))
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
     * 渲染带白色外框的标准物品槽位（用于主副手）
     */
    private fun renderItemSlotWithBorder(ctx: GuiGraphicsExtractor, itemId: String?, x: Int, y: Int, opacity: Int) {
        val size = L.HAND_SLOT_SIZE
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
                ctx.item(stack, 0, 0)
            } catch (_: Exception) {
                // 物品渲染异常时静默忽略
            }
        }
    }

    /**
     * 将物品 ID 字符串解析为 ItemStack
     * 例如 "minecraft:diamond_helmet" -> ItemStack(Items.DIAMOND_HELMET)
     */
    private fun resolveItemStack(itemId: String?): ItemStack? {
        if (itemId.isNullOrBlank()) return null
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
    //  辅助：渲染旁观高亮框
    // ──────────────────────────────────────────────────────────────

    /**
     * 在头像周围渲染黄色高亮框
     */
    private fun renderCardSpectateHighlight(ctx: GuiGraphicsExtractor, layout: RenderLayoutSnapshot, opacity: Int) {
        val cardX = layout.cardX
        val cardY = layout.cardY
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
    private inline fun withPose(ctx: GuiGraphicsExtractor, tx: Float, ty: Float, sx: Float, sy: Float, block: (GuiGraphicsExtractor) -> Unit) {
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
    private fun drawThinBorder(ctx: GuiGraphicsExtractor, x: Int, y: Int, size: Int, opacity: Int) {
        val borderColor = (opacity shl 24) or COLOR_LINE_SOFT // Line Soft
        drawThinBorderWithColor(ctx, x, y, size, borderColor)
    }

    private fun drawThinBorderWithColor(ctx: GuiGraphicsExtractor, x: Int, y: Int, size: Int, borderColor: Int) {
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

    private fun drawThinRectBorderWithColor(ctx: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, borderColor: Int) {
        if (width <= 0 || height <= 0) return
        withPose(ctx, x.toFloat(), y.toFloat(), 0.5f, 0.5f) {
            val scaledW = width * 2
            val scaledH = height * 2
            ctx.fill(0, 0, scaledW, 1, borderColor)
            ctx.fill(0, scaledH - 1, scaledW, scaledH, borderColor)
            ctx.fill(0, 0, 1, scaledH, borderColor)
            ctx.fill(scaledW - 1, 0, scaledW, scaledH, borderColor)
        }
    }

}
