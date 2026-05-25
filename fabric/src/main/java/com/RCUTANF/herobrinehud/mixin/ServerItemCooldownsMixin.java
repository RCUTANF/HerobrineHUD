package com.RCUTANF.herobrinehud.mixin;

import com.RCUTANF.herobrinehud.collector.PlayerDataCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ServerItemCooldowns;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into ServerItemCooldowns to intercept item cooldown changes.
 * Used to track cooldown state for main-hand and off-hand items.
 */
@Mixin(ServerItemCooldowns.class)
public abstract class ServerItemCooldownsMixin {

    @Shadow
    @Final
    private ServerPlayer player;

    /**
     * 当物品冷却开始时触发
     */
    @Inject(method = "onCooldownStarted", at = @At("RETURN"))
    private void onCooldownStarted(Identifier group, int cooldown, CallbackInfo ci) {
        PlayerDataCallback.INSTANCE.getCOOLDOWN_CHANGED().invoker()
                .onCooldownChanged(this.player, group, cooldown);
    }

    /**
     * 当物品冷却结束时触发
     */
    @Inject(method = "onCooldownEnded", at = @At("RETURN"))
    private void onCooldownEnded(Identifier group, CallbackInfo ci) {
        PlayerDataCallback.INSTANCE.getCOOLDOWN_CHANGED().invoker()
                .onCooldownChanged(this.player, group, 0);
    }
}

