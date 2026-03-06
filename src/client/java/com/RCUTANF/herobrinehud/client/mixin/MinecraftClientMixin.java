package com.RCUTANF.herobrinehud.client.mixin;

import com.RCUTANF.herobrinehud.client.SpectatorTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 注入 Minecraft 客户端
 * 
 * 监听摄像机实体变化，更新旁观状态追踪器
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {
    
    /**
     * 注入 setCameraEntity 方法
     * 当摄像机实体改变时更新旁观状态
     */
    @Inject(method = "setCameraEntity", at = @At("TAIL"))
    private void onSetCameraEntity(Entity entity, CallbackInfo ci) {
        if (entity instanceof Player player) {
            // 摄像机切换到玩家实体，记录UUID
            SpectatorTracker.INSTANCE.updateSpectatingPlayer(player.getUUID().toString());
        } else {
            // 摄像机不是玩家实体，清空旁观状态
            SpectatorTracker.INSTANCE.updateSpectatingPlayer(null);
        }
    }
}
