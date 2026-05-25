package com.RCUTANF.herobrinehud.client.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import java.util.function.Consumer;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Mixin 注入聊天组件
 * 修改聊天消息的Y坐标，使其向上移动以避免与玩家卡片重叠
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Unique
    private static final float CHAT_OFFSET_Y = -30.0F;

    /**
     * 修改聊天渲染的Y坐标
     * 将聊天位置向上移动x像素，避免与底部的玩家卡片重叠
     */
    @ModifyArg(
        method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;updatePose(Ljava/util/function/Consumer;)V"
        ),
        index = 0
    )
    private Consumer<Matrix3x2f> moveChat(Consumer<Matrix3x2f> original) {
        return pose -> {
            original.accept(pose);
            pose.translate(0.0F, CHAT_OFFSET_Y);
        };
    }
}
