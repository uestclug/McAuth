package io.github.plusls.McAuth.mixin;

import com.mojang.authlib.GameProfile;
import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.db.User;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;


@Mixin(ServerLoginNetworkHandler.class)
public abstract class ServerLoginNetworkHandlerMixin extends Object {
    @Shadow
    private MinecraftServer server;
    @Shadow
    public ClientConnection connection;
    @Shadow
    private GameProfile profile;

    @Redirect(method = "onHello(Lnet/minecraft/network/packet/c2s/login/LoginHelloC2SPacket;)V",
            at=@At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z", ordinal = 0))
    private boolean redirectIsOnlineMode(MinecraftServer server) {
        String username = profile.getName();
        User user = McAuthMod.auth.getUserByUsername(username);
        if (user != null && user.onlineMode) {
            return true;
        } else {
            return false;
        }
    }
}
