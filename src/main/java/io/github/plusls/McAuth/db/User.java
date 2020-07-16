package io.github.plusls.McAuth.db;

import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.UUID;

public class User {
    public UUID uuid = null;
    public String username = null;
    public String password = null;
    public boolean onlineMode = false;
    public double x = 0f;
    public double y = 0f;
    public double z = 0f;
    public RegistryKey<World> world;

    public User(UUID uuid, String username, String password, boolean onlineMode, double x, double y, double z, RegistryKey<World> world) {
        this.uuid = uuid;
        this.username = username;
        this.password = password;
        this.onlineMode = onlineMode;
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }
}
