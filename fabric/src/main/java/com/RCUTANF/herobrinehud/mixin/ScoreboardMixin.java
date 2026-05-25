package com.RCUTANF.herobrinehud.mixin;

import com.RCUTANF.herobrinehud.collector.TeamSyncCallback;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into Scoreboard to intercept team add/remove/player membership changes.
 */
@Mixin(Scoreboard.class)
public abstract class ScoreboardMixin {

    /**
     * 当一个新的队伍被添加到 Scoreboard 时触发
     */
    @Inject(method = "addPlayerTeam", at = @At("RETURN"))
    private void onAddPlayerTeam(String name, CallbackInfoReturnable<PlayerTeam> cir) {
        TeamSyncCallback.INSTANCE.getTEAM_ADDED().invoker().onTeamAdded(cir.getReturnValue());
    }

    /**
     * 当一个队伍从 Scoreboard 中移除时触发
     */
    @Inject(method = "removePlayerTeam", at = @At("HEAD"))
    private void onRemovePlayerTeam(PlayerTeam team, CallbackInfo ci) {
        TeamSyncCallback.INSTANCE.getTEAM_REMOVED().invoker().onTeamRemoved(team);
    }

    /**
     * 当玩家被添加到队伍时触发
     */
    @Inject(method = "addPlayerToTeam", at = @At("RETURN"))
    private void onAddPlayerToTeam(String playerName, PlayerTeam team, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            TeamSyncCallback.INSTANCE.getPLAYER_JOINED().invoker().onPlayerJoined(team, playerName);
        }
    }

    /**
     * 当玩家从队伍中移除时触发
     */
    @Inject(method = "removePlayerFromTeam(Ljava/lang/String;Lnet/minecraft/world/scores/PlayerTeam;)V", at = @At("HEAD"))
    private void onRemovePlayerFromTeam(String playerName, PlayerTeam team, CallbackInfo ci) {
        TeamSyncCallback.INSTANCE.getPLAYER_LEFT().invoker().onPlayerLeft(team, playerName);
    }
}

