package io.github.plusls.McAuth.command;

import com.google.common.base.Charsets;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.WhitelistCommand;

import java.util.Collections;
import java.util.UUID;

public class OfflineWhitelistCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("offlineWhitelist")
                .requires(OfflineWhitelistCommand::requires)
                .then(CommandManager.argument("username", StringArgumentType.word())
                        .executes(OfflineWhitelistCommand::execute)
                ));
    }

    private static boolean requires(ServerCommandSource source) {
        return source.hasPermissionLevel(4);
    }

    private static int execute(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        String username = StringArgumentType.getString(commandContext, "username");

        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
        return WhitelistCommand.executeAdd(commandContext.getSource(),
                Collections.singleton(new GameProfile(offlineUuid, username)));
    }
}
