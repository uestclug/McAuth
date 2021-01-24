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
import net.minecraft.util.Util;


final public class LoginCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("login")
                .requires(LoginCommand::requires)
                .then(
                        CommandManager.argument("password", StringArgumentType.word())
                                .executes(LoginCommand::execute)
                ));
    }

    private static boolean requires(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            return McAuthMod.auth.userExists(player) && !McAuthMod.auth.loggedIn(player);
        } catch (CommandSyntaxException e) {
            return false;
        }
    }

    private static int execute(CommandContext<ServerCommandSource> commandContext) {
        Entity sourceEntity = commandContext.getSource().getEntity();
        if (!(sourceEntity instanceof ServerPlayerEntity)) {
            return -1;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) sourceEntity;
        String password = StringArgumentType.getString(commandContext, "password");
        if (!McAuthMod.auth.login(player, password)) {
            player.sendSystemMessage(new LiteralText(Translator.tr("mc_auth_mod.login.wrong_password")), Util.NIL_UUID);
            return -1;
        }
        player.sendSystemMessage(new LiteralText(Translator.tr("mc_auth_mod.login.success")), Util.NIL_UUID);
        return 0;
    }
}
