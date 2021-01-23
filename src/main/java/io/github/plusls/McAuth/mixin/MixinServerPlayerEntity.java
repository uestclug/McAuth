package io.github.plusls.McAuth.mixin;

import com.mojang.authlib.GameProfile;
import io.github.plusls.McAuth.McAuthMod;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity {

    public MixinServerPlayerEntity(World world, BlockPos blockPos, GameProfile gameProfile) {
        super(world, blockPos, 0, gameProfile);
    }

    // no damage
    @Inject(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    public void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
        if (McAuthMod.auth.loggedIn((ServerPlayerEntity) (PlayerEntity) this)) return;
        ci.setReturnValue(false);
        ci.cancel();
    }
}