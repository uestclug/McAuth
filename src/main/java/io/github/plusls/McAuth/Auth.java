package io.github.plusls.McAuth;

import io.github.plusls.McAuth.db.Database;
import io.github.plusls.McAuth.db.User;
import io.github.plusls.McAuth.util.Translator;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

public class Auth {
    private final Set<UUID> loggedIn = new HashSet<>();
    private final Map<UUID, String> hint = new HashMap<>();
    private final Database db;

    public Auth() throws SQLException {
        Path path = Paths.get(FabricLoader.getInstance().getConfigDir().toString(), "McAuth.db");
        this.db = new Database(path);
    }

    public static void onDisconnect(ServerPlayNetworkHandler serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        // save last disconnect pos.
        if (McAuthMod.auth.loggedIn(serverPlayNetworkHandler.player)) {
            McAuthMod.auth.logout(serverPlayNetworkHandler.player);
            // tp player to spawn pos
//            ServerWorld overworld = server.getWorld(World.OVERWORLD);
//            // Apparently you cant getSpawnPos() from server, kind of weird its client-only
//            WorldProperties properties = overworld.getLevelProperties();
//            BlockPos spawn = new BlockPos(properties.getSpawnX(), properties.getSpawnY(), properties.getSpawnZ());
//            if (!overworld.getWorldBorder().contains(spawn)) {
//                spawn = overworld.getTopPosition(Heightmap.Type.MOTION_BLOCKING, new BlockPos(overworld.getWorldBorder().getCenterX(), 0.0D, overworld.getWorldBorder().getCenterZ()));
//            }
//            player.teleport(overworld, spawn.getX(), spawn.getY(), spawn.getZ(), player.yaw, player.pitch);
        }
    }

    public static void onJoin(ServerPlayNetworkHandler serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {
        User user = McAuthMod.auth.getUser(serverPlayNetworkHandler.player);
        if (user != null) {
            // check online mode
            if (user.onlineMode) {
                McAuthMod.auth.login(serverPlayNetworkHandler.player, null);
                return;
            }
            McAuthMod.auth.addHint(serverPlayNetworkHandler.player, "mc_auth_mod.hint.login");
        } else {
            McAuthMod.auth.addHint(serverPlayNetworkHandler.player, "mc_auth_mod.hint.register");
        }
        serverPlayNetworkHandler.player.sendSystemMessage(
                new LiteralText(Translator.tr(McAuthMod.auth.getHint(serverPlayNetworkHandler.player))),
                serverPlayNetworkHandler.player.getUuid());
    }

    public boolean register(ServerPlayerEntity player, String password, boolean onlineMode, UUID onlineUuid) {
        UUID uuid;
        if (onlineUuid == null)
            uuid = player.getUuid();
        else
            uuid = onlineUuid;
        if (this.db.userExistsByUUID(uuid))
            return false;
        if (onlineMode) {
            password = null;
        } else {
            password = BCrypt.hashpw(password, BCrypt.gensalt());
        }
        User user = new User(uuid,
                player.getName().getString(),
                password,
                onlineMode,
                player.getX(), player.getY(), player.getZ(), player.world.getRegistryKey());
        if (this.db.createUser(user)) {
            this.deleteHint(player);
            if (onlineMode) {
                this.addHint(player, "mc_auth_mod.hint.reconnect");
            } else {
                this.addHint(player, "mc_auth_mod.hint.login");
            }
            return true;
        } else {
            return false;
        }
    }


    public boolean userExistsByUUID(UUID uuid) {
        return this.db.userExistsByUUID(uuid);
    }

    public boolean userExists(ServerPlayerEntity player) {
        return userExistsByUUID(player.getUuid());
    }

    public User getUserByUUID(UUID uuid) {
        return this.db.getUserByUUID(uuid);
    }

    public User getUser(ServerPlayerEntity player) {
        return getUserByUUID(player.getUuid());
    }

    public List<User> getUserList() {
        return this.db.getUserList();
    }

    public User getUserByUsername(String username) {
        return this.db.getUserByUsername(username);
    }

    public boolean login(ServerPlayerEntity player, String password) {
        User user = this.getUser(player);
        if (user == null) {
            return false;
        }
        // When user is onlineMode, it will be checked by minecraft.
        // If connect success it means that it passed the check of minecraft.
        // So it will login directly.
        if (user.onlineMode) {
            this.loggedIn.add(player.getUuid());
            this.deleteHint(player);
            return true;
        }
        if (BCrypt.checkpw(password, user.password)) {
            player.teleport(player.server.getWorld(user.world), user.x, user.y, user.z, player.yaw, player.pitch);
            this.loggedIn.add(player.getUuid());
            this.deleteHint(player);
            return true;
        } else {
            return false;
        }
    }

    public boolean changePassword(ServerPlayerEntity player, String password) {
        if (!this.userExists(player)) {
            return false;
        }
        password = BCrypt.hashpw(password, BCrypt.gensalt());
        return this.db.updatePasswordByUUID(player.getUuid(), password);
    }

    private boolean updatePos(ServerPlayerEntity player) {
        if (!this.userExists(player)) {
            return false;
        }
        return this.db.updatePosByUUID(player.getUuid(), player.getX(), player.getY(), player.getZ(), player.world.getRegistryKey());
    }

    public boolean deleteUserByUUID(UUID uuid) {
        return this.db.deleteUserByUUID(uuid);
    }

    public boolean loggedIn(ServerPlayerEntity player) {
        return this.loggedIn.contains(player.getUuid());
    }

    public void logout(ServerPlayerEntity player) {
        this.loggedIn.remove(player.getUuid());
        this.updatePos(player);
    }

    public String getHint(ServerPlayerEntity player) {
        return this.hint.get(player.getUuid());
    }

    public void addHint(ServerPlayerEntity player, String hint) {
        this.hint.put(player.getUuid(), hint);
    }

    private void deleteHint(ServerPlayerEntity player) {
        this.hint.remove(player.getUuid());
    }

    public void clear() {
        this.db.close();
        this.loggedIn.clear();
        this.hint.clear();
    }
}