package com.RCUTANF.herobrinehud.mixin;

import com.RCUTANF.herobrinehud.collector.PlayerDataCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into ServerPlayerGameMode to intercept game mode changes.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

    @Shadow
    private GameType gameModeForPlayer;

    @Shadow
    @Final
    protected ServerPlayer player;

    /**
     * 当玩家游戏模式发生变化时触发
     * 在方法开始时捕获旧模式，在方法返回时如果确实发生了变化则触发事件
     */
    @Inject(method = "changeGameModeForPlayer", at = @At("HEAD"))
    private void onChangeGameModeHead(GameType newGameMode, CallbackInfoReturnable<Boolean> cir) {
        // 保存旧的游戏模式到线程局部变量
        herobrinehud$previousGameMode = this.gameModeForPlayer;
    }

    @Inject(method = "changeGameModeForPlayer", at = @At("RETURN"))
    private void onChangeGameModeReturn(GameType newGameMode, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            GameType oldMode = herobrinehud$previousGameMode;
            if (oldMode != null) {
                PlayerDataCallback.INSTANCE.getGAMEMODE_CHANGED().invoker()
                        .onGamemodeChanged(this.player, oldMode, newGameMode);
            }
        }
        herobrinehud$previousGameMode = null;
    }

    /**
     * 用于临时保存旧游戏模式的字段（Mixin unique field）
     */
    @Unique
    private GameType herobrinehud$previousGameMode;
}

