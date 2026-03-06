package com.RCUTANF.herobrinehud.mixin;

import com.RCUTANF.herobrinehud.team.PlayerDataCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into ServerPlayer to intercept dimension changes and respawn events.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    // ──────────────── 玩家数据加载完成 ────────────────

    /**
     * 当玩家数据从存档反序列化完成后触发
     * readAdditionalSaveData 在玩家加入服务器时从 playerdata 文件加载数据，
     * 包含装备、效果、游戏模式等所有持久化数据，
     * 此时机比 ServerPlayConnectionEvents.JOIN 更适合获取完整的玩家状态
     */
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void onPlayerDataLoaded(ValueInput input, CallbackInfo ci) {
        PlayerDataCallback.INSTANCE.getPLAYER_DATA_LOADED().invoker()
                .onPlayerDataLoaded((ServerPlayer) (Object) this);
    }

    // ──────────────── 维度切换 ────────────────

    /**
     * 当玩家完成维度切换后触发
     * triggerDimensionChangeTriggers 在玩家跨维度传送完成后被调用，
     * 是最可靠的维度变更检测点
     */
    @Inject(method = "triggerDimensionChangeTriggers", at = @At("HEAD"))
    private void onDimensionChange(ServerLevel level, CallbackInfo ci) {
        PlayerDataCallback.INSTANCE.getDIMENSION_CHANGED().invoker()
                .onDimensionChanged((ServerPlayer) (Object) this);
    }

    // ──────────────── 重生 ────────────────

    /**
     * 当玩家重生后（新 ServerPlayer 实例从旧实例复制数据）触发
     * restoreFrom 在重生时被调用，用于从旧玩家实例恢复数据
     */
    @Inject(method = "restoreFrom", at = @At("RETURN"))
    private void onRestoreFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo ci) {
        PlayerDataCallback.INSTANCE.getPLAYER_RESPAWNED().invoker()
                .onPlayerRespawned((ServerPlayer) (Object) this);
    }
}

