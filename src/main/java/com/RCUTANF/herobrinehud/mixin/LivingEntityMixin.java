package com.RCUTANF.herobrinehud.mixin;

import com.RCUTANF.herobrinehud.team.PlayerDataCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
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

    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void onEffectAdded(MobEffectInstance effectInstance, @Nullable Entity entity, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            PlayerDataCallback.INSTANCE.getEFFECT_CHANGED().invoker().onEffectChanged(serverPlayer);
        }
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void onEffectUpdated(MobEffectInstance effectInstance, boolean forced, @Nullable Entity entity, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            PlayerDataCallback.INSTANCE.getEFFECT_CHANGED().invoker().onEffectChanged(serverPlayer);
        }
    }

    @Inject(method = "onEffectsRemoved", at = @At("TAIL"))
    private void onEffectsRemoved(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            PlayerDataCallback.INSTANCE.getEFFECT_CHANGED().invoker().onEffectChanged(serverPlayer);
        }
    }

    // ──────────────── 装备变化 ────────────────

    /**
     * 注入 handleEquipmentChanges —— 处理所有装备槽位变化（主副手、头盔、胸甲、护腿、靴子等）。
     * 该方法由引擎在每 tick 检测到任何装备变化时调用，涵盖玩家操作、指令、死亡掉落等所有路径。
     */
    @Inject(method = "handleEquipmentChanges", at = @At("TAIL"))
    private void onHandEquipmentChanged(Map<EquipmentSlot, ItemStack> equipments, CallbackInfo ci) {
        //noinspection ConstantValue
        if ((Object) this instanceof ServerPlayer serverPlayer && serverPlayer.getGameProfile() != null) {
            for (Map.Entry<EquipmentSlot, ItemStack> entry : equipments.entrySet()) {
                PlayerDataCallback.INSTANCE.getEQUIPMENT_CHANGED().invoker()
                        .onEquipmentChanged(serverPlayer, entry.getKey(), entry.getValue());
            }
        }
    }
}