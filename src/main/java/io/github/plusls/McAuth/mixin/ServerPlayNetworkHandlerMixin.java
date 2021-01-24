package io.github.plusls.McAuth.mixin;

import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.util.Translator;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ConfirmScreenActionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements PacketListener {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "executeCommand(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void onExecuteCommand(String command, CallbackInfo ci) {
        if (McAuthMod.auth.loggedIn(this.player))
            return;
        if (!command.startsWith("/") || command.length() <= 1) {
            command = "";
        } else {
            command = command.substring(1);
        }
        if (command.startsWith("login") || command.startsWith("register") || command.startsWith("setOnlineLogin"))
            return;
        player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
        ci.cancel();
    }

    private int moveCancel = 0;

    @Inject(method = "onPlayerMove(Lnet/minecraft/network/packet/c2s/play/PlayerMoveC2SPacket;)V", at = @At("HEAD"), cancellable = true)
    public void onOnPlayerMove(PlayerMoveC2SPacket playerMoveC2SPacket_1, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            // tell client player real pos
            moveCancel++;
            if (moveCancel > 5) {
                this.player.networkHandler.sendPacket(
                        new PlayerPositionLookS2CPacket(
                                this.player.getX(),
                                this.player.getY(),
                                this.player.getZ(),
                                this.player.yaw,
                                this.player.pitch,
                                Collections.emptySet(), 0
                        )
                );
                moveCancel = 0;
            }
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerAction(Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket;)V", at = @At("HEAD"), cancellable = true)
    public void onOnPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            // tell client block update when break block
            this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(this.player.world, packet.getPos()));
            // update client inventory
            // -2 means inventory
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(-2,
                            this.player.inventory.selectedSlot,
                            this.player.inventory.getStack(this.player.inventory.selectedSlot)
                    ));
            // 40 means offhand
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(-2,
                            40,
                            this.player.inventory.getStack(40)
                    ));
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerInteractBlock(Lnet/minecraft/network/packet/c2s/play/PlayerInteractBlockC2SPacket;)V", at = @At("HEAD"), cancellable = true)
    public void onOnPlayerInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            // tell client block update when InteractBlock
            // such as use or place block
            this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(this.player.world, packet.getBlockHitResult().getBlockPos()));
            // update client inventory
            // -2 means inventory
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(-2,
                            this.player.inventory.selectedSlot,
                            this.player.inventory.getStack(this.player.inventory.selectedSlot)
                    ));
            // 40 means offhand
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(-2,
                            40,
                            this.player.inventory.getStack(40)
                    ));
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
            ci.cancel();
        }

    }

    @Inject(method = "onPlayerInteractItem(Lnet/minecraft/network/packet/c2s/play/PlayerInteractItemC2SPacket;)V", at = @At("HEAD"), cancellable = true)
    public void onOnPlayerInteractItem(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerInteractEntity(Lnet/minecraft/network/packet/c2s/play/PlayerInteractEntityC2SPacket;)V", at = @At("HEAD"), cancellable = true)
    public void onOnPlayerInteractEntity(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
            ci.cancel();
        }
    }


    // 防止从gui中移动物品和丢弃物品
    @Inject(method = "onClickSlot(Lnet/minecraft/network/packet/c2s/play/ClickSlotC2SPacket;)V", at = @At("HEAD"), cancellable = true)
    public void onClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            this.player.networkHandler.sendPacket(
                    new ConfirmScreenActionS2CPacket(packet.getSyncId(), packet.getActionId(), false));

            // update client
            // 这会导致客户端断开连接
            // WIP
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(this.player.currentScreenHandler.syncId,
                            packet.getSlot(),
                            this.player.currentScreenHandler.getSlot(packet.getSlot()).getStack()
                    ));
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), player.getUuid());
            ci.cancel();
        }
    }
}