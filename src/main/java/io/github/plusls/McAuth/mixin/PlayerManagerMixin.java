package io.github.plusls.McAuth.mixin;

import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.db.User;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    @Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/packet/s2c/play/DifficultyS2CPacket;<init>(Lnet/minecraft/world/Difficulty;Z)V",
                    ordinal = 0
            )
    )
    private void onOnPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        User user = McAuthMod.auth.getUser(player);
        if (user != null) {
            // check online mode
            if (user.onlineMode) {
                McAuthMod.auth.login(player, null);
                return;
            }
            McAuthMod.auth.addHint(player, "Login with §7/login <password>");
        } else {
            McAuthMod.auth.addHint(player, "Please register with §7/register <password> <password>\n" +
                    "or /setOnlineLogin <username> to Use minecraft online mode auth.");
        }
        ScheduledFuture<?> future = McAuthMod.auth.scheduler.scheduleAtFixedRate(
                () -> {
                    String hint = McAuthMod.auth.getHint(player);
                    if (hint == null) {
                        // future.cancel();
                    } else {
                        player.sendSystemMessage(new LiteralText(hint), player.getUuid());
                    }
                }, 0, 3, TimeUnit.SECONDS);

        McAuthMod.auth.scheduler.schedule(() -> {
            future.cancel(false);
            if(!McAuthMod.auth.loggedIn(player)) {
                if (!player.isDisconnected())
                    player.networkHandler.disconnect(new LiteralText("You took too much time!"));
            }
        }, 45, TimeUnit.SECONDS);
    }
}