package io.github.plusls.McAuth.mixin;

import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.util.Translator;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin implements PacketListener {
    @Shadow
    public ServerPlayerEntity player;
    @Shadow
    private int requestedTeleportId;
    @Unique
    private int moveCancel = 0;

    @Inject(method = "executeCommand", at = @At("HEAD"), cancellable = true)
    private void onExecuteCommand(String command, CallbackInfo ci) {
        if (McAuthMod.auth.loggedIn(this.player))
            return;
        if (!command.startsWith("/") || command.length() <= 1) {
            command = "";
        } else {
            command = command.substring(1);
        }
        if (command.startsWith("login ") || command.startsWith("register ") || command.startsWith("setOnlineLogin "))
            return;
        player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
        ci.cancel();
    }

    @Inject(method = "onPlayerMove", at = @At("HEAD"), cancellable = true)
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
                                this.player.getYaw(),
                                this.player.getPitch(),
                                Collections.emptySet(), this.requestedTeleportId, false
                        )
                );
                moveCancel = 0;
            }
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    public void onOnPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            // tell client block update when break block
            this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(this.player.world, packet.getPos()));
            // update client inventory
            // -2 means inventory
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(ScreenHandlerSlotUpdateS2CPacket.UPDATE_PLAYER_INVENTORY_SYNC_ID,
                            this.player.currentScreenHandler.getRevision(),
                            this.player.getInventory().selectedSlot,
                            this.player.getInventory().getStack(this.player.getInventory().selectedSlot)
                    ));
            // 40 means offhand
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(ScreenHandlerSlotUpdateS2CPacket.UPDATE_PLAYER_INVENTORY_SYNC_ID,
                            this.player.currentScreenHandler.getRevision(),
                            PlayerInventory.OFF_HAND_SLOT,
                            this.player.getInventory().getStack(PlayerInventory.OFF_HAND_SLOT)
                    ));
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerInteractBlock", at = @At("HEAD"), cancellable = true)
    public void onOnPlayerInteractBlock(PlayerInteractBlockC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            // tell client block update when InteractBlock
            // such as use or place block
            this.player.networkHandler.sendPacket(new BlockUpdateS2CPacket(this.player.world, packet.getBlockHitResult().getBlockPos()));
            // update client inventory
            // -2 means inventory
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(ScreenHandlerSlotUpdateS2CPacket.UPDATE_PLAYER_INVENTORY_SYNC_ID,
                            this.player.currentScreenHandler.getRevision(),
                            this.player.getInventory().selectedSlot,
                            this.player.getInventory().getStack(this.player.getInventory().selectedSlot)
                    ));
            // 40 means offhand
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(ScreenHandlerSlotUpdateS2CPacket.UPDATE_PLAYER_INVENTORY_SYNC_ID,
                            this.player.currentScreenHandler.getRevision(),
                            PlayerInventory.OFF_HAND_SLOT,
                            this.player.getInventory().getStack(PlayerInventory.OFF_HAND_SLOT)
                    ));
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
            ci.cancel();
        }

    }

    @Inject(method = "onPlayerInteractItem", at = @At("HEAD"), cancellable = true)
    public void onOnPlayerInteractItem(PlayerInteractItemC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
            ci.cancel();
        }
    }

    @Inject(method = "onPlayerInteractEntity", at = @At("HEAD"), cancellable = true)
    public void onOnPlayerInteractEntity(PlayerInteractEntityC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), Util.NIL_UUID);
            ci.cancel();
        }
    }


    // 防止从gui中移动物品和丢弃物品
    @Inject(method = "onClickSlot", at = @At("HEAD"), cancellable = true)
    public void onClickSlot(ClickSlotC2SPacket packet, CallbackInfo ci) {
        if (!McAuthMod.auth.loggedIn(this.player)) {
            this.player.currentScreenHandler.syncState();
            // update client
            // 这会导致客户端断开连接
            // WIP
            this.player.networkHandler.sendPacket(
                    new ScreenHandlerSlotUpdateS2CPacket(this.player.currentScreenHandler.syncId, 0,
                            packet.getSlot(),
                            this.player.currentScreenHandler.getSlot(packet.getSlot()).getStack()
                    ));
            player.sendSystemMessage(new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), player.getUuid());
            ci.cancel();
        }
    }
}