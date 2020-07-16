package io.github.plusls.McAuth.mixin;

import io.github.plusls.McAuth.McAuthMod;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "loadWorld()V", at = @At("RETURN"))
    private void onLoadWorld(CallbackInfo info) {
        McAuthMod.init();
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    public void onShutdown(CallbackInfo info) {
        McAuthMod.shutdown();
    }
}
