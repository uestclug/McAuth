package io.github.plusls.McAuth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.plusls.McAuth.McAuthMod;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;

public class PasswdCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("passwd")
                .requires(PasswdCommand::requires)
                .then(CommandManager.argument("password", StringArgumentType.word())
                        .then(CommandManager.argument("verify", StringArgumentType.word())
                                .executes(PasswdCommand::execute)
                        )));
    }

    private static boolean requires(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            return McAuthMod.auth.loggedIn(player);
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int execute(CommandContext<ServerCommandSource> commandContext) {
        ServerPlayerEntity player = (ServerPlayerEntity) commandContext.getSource().getEntity();
        String password = StringArgumentType.getString(commandContext, "password");
        if (password.equals(StringArgumentType.getString(commandContext, "verify"))) {
            if (McAuthMod.auth.changePassword(player, password)) {
                player.sendSystemMessage(new LiteralText("§aChange password success!"), Util.NIL_UUID);
                return 0;
            } else {
                player.sendSystemMessage(new LiteralText("§cChange password failed!"), Util.NIL_UUID);
                return -1;
            }
        } else {
            player.sendSystemMessage(new LiteralText("§cThe password is not the same as the second one!"), Util.NIL_UUID);
            return -1;
        }
    }
}
