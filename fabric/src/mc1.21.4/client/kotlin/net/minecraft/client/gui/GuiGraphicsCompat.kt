package net.minecraft.client.gui

import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component

fun GuiGraphicsExtractor.text(font: Font, text: String, x: Int, y: Int, color: Int, shadow: Boolean) {
    drawString(font, text, x, y, color, shadow)
}

fun GuiGraphicsExtractor.text(font: Font, text: Component, x: Int, y: Int, color: Int, shadow: Boolean) {
    drawString(font, text, x, y, color, shadow)
}
