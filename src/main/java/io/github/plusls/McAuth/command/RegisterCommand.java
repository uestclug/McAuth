package io.github.plusls.McAuth.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.util.Translator;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

public class RegisterCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("register")
                .requires(RegisterCommand::requires)
                .then(CommandManager.argument("password", StringArgumentType.word())
                        .then(CommandManager.argument("verify", StringArgumentType.word())
                                .executes(RegisterCommand::execute)
                        )
                ));
    }

    private static boolean requires(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            return !McAuthMod.auth.userExists(player);
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int execute(CommandContext<ServerCommandSource> commandContext) {
        ServerCommandSource source = commandContext.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof ServerPlayerEntity)) {
            return -1;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) sourceEntity;
        String password = StringArgumentType.getString(commandContext, "password");

        if (password.equals(StringArgumentType.getString(commandContext, "verify"))) {

            if (McAuthMod.auth.register(player, password, false, null)) {
                source.sendFeedback(new LiteralText(Translator.tr("mc_auth_mod.register.success")), true);
                return 0;
            } else {
                source.sendFeedback(new LiteralText(Translator.tr("mc_auth_mod.register.failed")), true);
                return -1;
            }
        } else {
            source.sendFeedback(new LiteralText(Translator.tr("mc_auth_mod.register.password_not_equal")), true);
            return -1;
        }
    }
}
