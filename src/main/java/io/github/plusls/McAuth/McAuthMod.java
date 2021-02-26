package io.github.plusls.McAuth;

import io.github.plusls.McAuth.command.*;
import io.github.plusls.McAuth.util.Translator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;

public class McAuthMod implements ModInitializer {
    public static final String MODID = "mc_auth_mod";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static Auth auth = null;
    public static boolean isCarpetLoaded;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(LoginCommand::register);
        CommandRegistrationCallback.EVENT.register(SetOnlineLoginCommand::register);
        CommandRegistrationCallback.EVENT.register(RegisterCommand::register);
        CommandRegistrationCallback.EVENT.register(PasswdCommand::register);
        CommandRegistrationCallback.EVENT.register(DeleteAccountCommand::register);
        CommandRegistrationCallback.EVENT.register(OfflineWhitelistCommand::register);
        CommandRegistrationCallback.EVENT.register(UpdateWhitelistCommand::register);
        Translator.reloadLanguage();
        isCarpetLoaded = FabricLoader.getInstance().isModLoaded("carpet");

    }

    public static void init() {
        try {
            McAuthMod.auth = new Auth();
            ServerPlayConnectionEvents.DISCONNECT.register(Auth::onDisconnect);
            ServerPlayConnectionEvents.JOIN.register(Auth::onJoin);
        } catch (SQLException e) {
            // log error and make game crash.
            throw new RuntimeException("Initialization failed for McAuth.", e);
        }
    }

    public static void shutdown() {
        if (McAuthMod.auth != null) {
            McAuthMod.auth.clear();
            McAuthMod.auth = null;
        }
    }
}