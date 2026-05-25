package com.RCUTANF.herobrinehud.mixin;

import com.RCUTANF.herobrinehud.collector.TeamSyncCallback;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into ServerScoreboard to intercept team property changes.
 * ServerScoreboard.onTeamChanged is called whenever team properties
 * (color, prefix, suffix, etc.) are updated.
 */
@Mixin(ServerScoreboard.class)
public abstract class ServerScoreboardMixin {

    /**
     * 当队伍属性发生变化时触发（颜色、前后缀、可见性等）
     */
    @Inject(method = "onTeamChanged", at = @At("HEAD"))
    private void onTeamChanged(PlayerTeam team, CallbackInfo ci) {
        TeamSyncCallback.INSTANCE.getTEAM_MODIFIED().invoker().onTeamModified(team);
    }
}

