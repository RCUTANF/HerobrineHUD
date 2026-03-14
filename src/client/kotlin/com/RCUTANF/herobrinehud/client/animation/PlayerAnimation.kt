package com.RCUTANF.herobrinehud.client.animation

import net.minecraft.client.gui.GuiGraphics

/**
 * 玩家动画基类
 */
abstract class PlayerAnimation(
    val playerUuid: String,
    val startTime: Long = System.currentTimeMillis(),
    val duration: Long = 1500
) {
    /**
     * 计算动画进度 (0.0 ~ 1.0)
     */
    fun getProgress(): Float {
        val elapsed = System.currentTimeMillis() - startTime
        return (elapsed.toFloat() / duration).coerceIn(0f, 1f)
    }
    
    /**
     * 检查动画是否已结束
     */
    fun isFinished(): Boolean = getProgress() >= 1.0f
    
    /**
     * 渲染动画
     * 
     * @param ctx GuiGraphics上下文
     * @param cardX 卡片X坐标
     * @param cardY 卡片Y坐标
     * @param opacity 卡片不透明度
     */
    abstract fun render(ctx: GuiGraphics, cardX: Int, cardY: Int, opacity: Int)
}
