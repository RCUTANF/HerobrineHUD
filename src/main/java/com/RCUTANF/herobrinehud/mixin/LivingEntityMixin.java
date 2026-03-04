package com.RCUTANF.herobrinehud.mixin;

import com.RCUTANF.herobrinehud.team.PlayerDataCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into LivingEntity to intercept health, effect, and equipment changes.
 * All callbacks guard with instanceof ServerPlayer to only fire for server-side players.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // ──────────────── 血量变化 ────────────────

    /**
     * 当 setHealth 被调用时触发（包括受伤、治疗、自然回血等所有路径）
     */
    @Inject(method = "setHealth", at = @At("RETURN"))
    private void onSetHealth(float health, CallbackInfo ci) {
        //noinspection ConstantValue
        if ((Object) this instanceof ServerPlayer serverPlayer && serverPlayer.getGameProfile() != null) {
            PlayerDataCallback.INSTANCE.getHEALTH_CHANGED().invoker().onHealthChanged(serverPlayer);
        }
    }

    // ──────────────── 药水效果变化 ────────────────

    /**
     * 当添加药水效果时触发
     */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("RETURN"))
    private void onAddEffect(MobEffectInstance effectInstance, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            //noinspection ConstantValue
            if ((Object) this instanceof ServerPlayer serverPlayer && serverPlayer.getGameProfile() != null) {
                PlayerDataCallback.INSTANCE.getEFFECT_CHANGED().invoker().onEffectChanged(serverPlayer);
            }
        }
    }

    /**
     * 当移除单个药水效果时触发
     */
    @Inject(method = "removeEffect", at = @At("RETURN"))
    private void onRemoveEffect(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            //noinspection ConstantValue
            if ((Object) this instanceof ServerPlayer serverPlayer && serverPlayer.getGameProfile() != null) {
                PlayerDataCallback.INSTANCE.getEFFECT_CHANGED().invoker().onEffectChanged(serverPlayer);
            }
        }
    }

    /**
     * 当清除所有药水效果时触发
     */
    @Inject(method = "removeAllEffects", at = @At("RETURN"))
    private void onRemoveAllEffects(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            //noinspection ConstantValue
            if ((Object) this instanceof ServerPlayer serverPlayer && serverPlayer.getGameProfile() != null) {
                PlayerDataCallback.INSTANCE.getEFFECT_CHANGED().invoker().onEffectChanged(serverPlayer);
            }
        }
    }

    // ──────────────── 装备变化 ────────────────

    /**
     * 当装备槽位发生变化时触发（包括武器、护甲、副手等）
     */
    @Inject(method = "setItemSlot", at = @At("RETURN"))
    private void onSetItemSlot(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        //noinspection ConstantValue
        if ((Object) this instanceof ServerPlayer serverPlayer && serverPlayer.getGameProfile() != null) {
            PlayerDataCallback.INSTANCE.getEQUIPMENT_CHANGED().invoker().onEquipmentChanged(serverPlayer, slot, stack);
        }
    }
}

