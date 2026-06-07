package net.minecraft.client.gui

import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionf
import org.joml.Vector3f

fun GuiGraphicsExtractor.text(font: Font, text: String, x: Int, y: Int, color: Int, shadow: Boolean) {
    drawString(font, text, x, y, color, shadow)
}

fun GuiGraphicsExtractor.text(font: Font, text: Component, x: Int, y: Int, color: Int, shadow: Boolean) {
    drawString(font, text, x, y, color, shadow)
}

fun GuiGraphicsExtractor.entity(
    state: EntityRenderState,
    scale: Float,
    translation: Vector3f,
    rotation: Quaternionf,
    cameraAngle: Quaternionf,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int
) {
    submitEntityRenderState(state, scale, translation, rotation, cameraAngle, left, top, right, bottom)
}

fun GuiGraphicsExtractor.item(stack: ItemStack, x: Int, y: Int) {
    renderItem(stack, x, y)
}

fun GuiGraphicsExtractor.centeredText(font: Font, text: String, x: Int, y: Int, color: Int) {
    drawCenteredString(font, text, x, y, color)
}

fun GuiGraphicsExtractor.centeredText(font: Font, text: Component, x: Int, y: Int, color: Int) {
    drawCenteredString(font, text, x, y, color)
}
