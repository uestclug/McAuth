package io.github.plusls.McAuth.mixin;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.db.User;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.ServerLoginPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;
import java.util.UUID;


@Mixin(ServerLoginNetworkHandler.class)
public abstract class MixinServerLoginNetworkHandler implements ServerLoginPacketListener {
    @Final
    @Shadow
    public ClientConnection connection;
    @Shadow
    GameProfile profile;

    @Redirect(method = "onHello",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnlineMode()Z", ordinal = 0))
    private boolean redirectIsOnlineMode(MinecraftServer server) {
        String username = this.profile.getName();
        UUID uuid = this.profile.getId();
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.profile.getName()).getBytes(Charsets.UTF_8));
        String hostname = ((InetSocketAddress) this.connection.getAddress()).getHostName();
        User user = McAuthMod.auth.getUserByUsername(username);

        McAuthMod.LOGGER.info("{} try to login! ip:{}", this.profile, hostname);
        // 若是不存在 FabricProxy 同时 user 的设置里不为 online mode,
        // 则说明是盗版用户直接连接， 返回 false 让它在游戏中进行验证
        if (user != null && user.onlineMode) {
            // uuid != null 标明存在 FabricProxy
            // 如果 uuid 为盗版 uuid, 则说明是盗版用户尝试使用正版 username 登陆
            // 返回 true 让它走正版的验证流程让盗版用户直接连接失败
            // 走正版验证的直连用户则可以正常登陆

            if (uuid != null && !uuid.equals(offlineUuid) && hostname.equals("localhost")) {
                // uuid != offlineUuid 则认为 uuid 为正版的uuid
                // 由于是通过 FabricProxy 进行连接的，不支持正版验证
                // 返回 false 直接让正版用户以盗版的方式进行登陆， FabricProxy 会自动处理好 uuid 的映射
                McAuthMod.LOGGER.info("{}:{} login from FabricProxy!", username, uuid);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }
}
