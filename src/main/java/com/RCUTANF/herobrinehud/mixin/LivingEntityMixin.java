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

import java.util.Map;

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
     * 注入 handleEquipmentChanges —— 处理主副手物品变化。
     * 该方法接收一个 Map，包含本 tick 内发生变化的装备槽位及其新物品。
     * 仅当 Map 中包含 MAINHAND 或 OFFHAND 时才触发回调。
     */
    @Inject(method = "handleEquipmentChanges", at = @At("HEAD"))
    private void onHandEquipmentChanged(Map<EquipmentSlot, ItemStack> equipments, CallbackInfo ci) {
        //noinspection ConstantValue
        if ((Object) this instanceof ServerPlayer serverPlayer && serverPlayer.getGameProfile() != null) {
            if (equipments.containsKey(EquipmentSlot.MAINHAND)) {
                ItemStack mainHand = equipments.get(EquipmentSlot.MAINHAND);
                PlayerDataCallback.INSTANCE.getEQUIPMENT_CHANGED().invoker()
                        .onEquipmentChanged(serverPlayer, EquipmentSlot.MAINHAND, mainHand);
            }
            if (equipments.containsKey(EquipmentSlot.OFFHAND)) {
                ItemStack offHand = equipments.get(EquipmentSlot.OFFHAND);
                PlayerDataCallback.INSTANCE.getEQUIPMENT_CHANGED().invoker()
                        .onEquipmentChanged(serverPlayer, EquipmentSlot.OFFHAND, offHand);
            }
        }
    }

    /**
     * 注入 onEquipItem —— 装备真正发生变化时的权威回调（已过滤客户端、旁观者、相同物品）。
     * 用于处理除主副手以外的装备槽位变化（头盔、胸甲、护腿、靴子、身体护甲等）。
     */
    @Inject(method = "onEquipItem", at = @At("HEAD"))
    private void onEquipItemChanged(EquipmentSlot slot, ItemStack oldItem, ItemStack newItem, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer && serverPlayer.getGameProfile() != null) {
            if (!serverPlayer.level().isClientSide() && !ItemStack.isSameItemSameComponents(oldItem, newItem)) {
                // 主副手已由 handleEquipmentChanges 处理，此处跳过避免重复触发
                if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
                    PlayerDataCallback.INSTANCE.getEQUIPMENT_CHANGED().invoker().onEquipmentChanged(serverPlayer, slot, newItem);
                }
            }
        }
    }
}