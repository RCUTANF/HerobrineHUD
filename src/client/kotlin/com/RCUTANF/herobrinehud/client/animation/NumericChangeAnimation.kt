package com.RCUTANF.herobrinehud.client.animation

import com.RCUTANF.herobrinehud.client.ui.CardLayout
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * 数值变化动画（通用）
 * 用于显示血量、护甲等数值的变化
 */
class NumericChangeAnimation(
    playerUuid: String,
    val changeAmount: Double,
    val displayText: String? = null,  // 自定义显示文本
    val color: Int = 0xFFFFFF,        // 文字颜色
    val maxOffset: Int = 25,          // 最大偏移量
    duration: Long = 1500
) : PlayerAnimation(playerUuid, duration = duration) {
    
    override fun render(ctx: GuiGraphicsExtractor, cardX: Int, cardY: Int, opacity: Int) {
        val progress = getProgress()
        if (progress >= 1.0f) return
        
        val font = Minecraft.getInstance().font
        
        // 计算位置和透明度
        val offsetY = (progress * maxOffset).toInt()
        val alpha = ((1.0f - progress) * 255).toInt().coerceIn(0, 255)
        
        // 格式化文字
        val text = displayText ?: if (changeAmount > 0) {
            "+%.1f".format(changeAmount)
        } else {
            "%.1f".format(changeAmount)
        }
        
        // 计算居中位置
        val textWidth = font.width(text)
        val x = cardX + (CardLayout.CARD_WIDTH - textWidth) / 2
        val y = cardY - offsetY - 10
        
        // 绘制文字（带阴影）
        val finalColor = (alpha shl 24) or (color and 0xFFFFFF)
        ctx.text(font, text, x, y, finalColor, true)
    }
}
