package io.github.plusls.McAuth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.db.User;
import io.github.plusls.McAuth.util.Translator;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
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
        String username = StringArgumentType.getString(commandContext, "username");
        User user = McAuthMod.auth.getUserByUsername(username);
        if (user != null) {
            if (McAuthMod.auth.deleteUserByUUID(user.uuid)) {
                commandContext.getSource().sendFeedback(
                        new LiteralText(String.format(
                                Translator.tr("mc_auth_mod.delete_account.success"),
                                username)), true);
                return 0;
            } else {
                commandContext.getSource().sendFeedback(
                        new LiteralText(String.format(
                                Translator.tr("mc_auth_mod.delete_account.failed"),
                                username)), true);
                return -1;
            }
        } else {
            commandContext.getSource().sendFeedback(
                    new LiteralText(String.format(
                            Translator.tr("mc_auth_mod.delete_account.no_account"),
                            username)), false);
            return -1;
        }
    }
}
