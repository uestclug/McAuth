package io.github.plusls.McAuth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.db.User;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class DeleteAccountCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("deleteAccount")
                .requires(DeleteAccountCommand::requires)
                .then(CommandManager.argument("username", StringArgumentType.word())
                        .executes(DeleteAccountCommand::execute)
                ));
    }

    private static boolean requires(ServerCommandSource source) {
        return source.hasPermissionLevel(4);
    }

    private static int execute(CommandContext<ServerCommandSource> commandContext) {
        ServerPlayerEntity player = (ServerPlayerEntity) commandContext.getSource().getEntity();
        String username = StringArgumentType.getString(commandContext, "username");
        User user = McAuthMod.auth.getUserByUsername(username);
        if (user != null) {
            if (McAuthMod.auth.deleteUserByUUID(user.uuid)) {
                commandContext.getSource().sendFeedback(
                        new LiteralText(String.format("Delete account %s success!", username)), true);
                return 0;
            } else {
                commandContext.getSource().sendFeedback(
                        new LiteralText(String.format("Delete account %s failed.", username)), true);
                return -1;
            }
        } else {
            commandContext.getSource().sendFeedback(
                    new LiteralText(String.format("Can't find account %s", username)), false);
            return -1;
        }
    }
}
