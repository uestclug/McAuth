package io.github.plusls.McAuth;

import io.github.plusls.McAuth.db.Database;
import io.github.plusls.McAuth.db.User;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Auth {
    public ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    private Set<UUID> loggedIn = new HashSet<UUID>();
    private Map<UUID, String> hint = new HashMap<UUID, String>();
    private Database db = null;

    public Auth() throws SQLException {
        Path path = Paths.get(FabricLoader.getInstance().getConfigDirectory().getAbsolutePath(), "McAuth.db");
        this.db = new Database(path);
        this.scheduler.setRemoveOnCancelPolicy(true);
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
                this.addHint(player, "§aReconnect to server will auto login!!");
            } else {
                this.addHint(player, "Login with §7/login <password>");
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean userExists(ServerPlayerEntity player) {
        return this.db.userExistsByUUID(player.getUuid());
    }

    public User getUser(ServerPlayerEntity player) {
        return this.db.getUserByUUID(player.getUuid());
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
        this.scheduler.shutdownNow();
        this.loggedIn.clear();
        this.hint.clear();
    }
}