package com.RCUTANF.herobrinehud.client.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin 注入聊天组件
 * 
 * 修改聊天消息的Y坐标，使其向上移动以避免与玩家卡片重叠
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    
    /**
     * 修改聊天渲染的Y坐标
     * 将聊天位置向上移动x像素，避免与底部的玩家卡片重叠
     */
    @ModifyVariable(
        method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V",
            at = @At("STORE"),
            ordinal = 4,
            name = "m"
    )
    private int moveChat(int m) {
        return m - 30;
    }
}
