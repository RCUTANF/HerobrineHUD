package com.RCUTANF.herobrinehud.mixin;

import com.RCUTANF.herobrinehud.collector.PlayerDataCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into FoodData to detect food level changes during server tick updates.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow
    private int foodLevel;

    @Unique
    private int herobrinehud$prevFoodLevel;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(ServerPlayer player, CallbackInfo ci) {
        this.herobrinehud$prevFoodLevel = this.foodLevel;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTickReturn(ServerPlayer player, CallbackInfo ci) {
        if (this.foodLevel != this.herobrinehud$prevFoodLevel) {
            PlayerDataCallback.INSTANCE.getFOOD_LEVEL_CHANGED().invoker().onFoodLevelChanged(player);
        }
    }
}

