package io.github.plusls.McAuth.command;

import com.mojang.authlib.Agent;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.util.MyProfileLookupCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Util;

public class SetOnlineLoginCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("setOnlineLogin")
                .requires(SetOnlineLoginCommand::requires)
                .then(CommandManager.argument("username", StringArgumentType.word())
                        .executes(SetOnlineLoginCommand::execute)));
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
        ServerPlayerEntity player = (ServerPlayerEntity) commandContext.getSource().getEntity();
        String username = StringArgumentType.getString(commandContext, "username");
        if (player.getName().getString().equals(username)) {
            MyProfileLookupCallback myProfileLookupCallback = new MyProfileLookupCallback();
            player.server.getGameProfileRepo().findProfilesByNames(new String[]{username}, Agent.MINECRAFT, myProfileLookupCallback);
            if (myProfileLookupCallback.gameProfile == null) {
                McAuthMod.LOGGER.info(String.format("Get %s gameProfile failed", username));
                player.sendSystemMessage(new LiteralText("§cGet online gameProfile failed."), Util.NIL_UUID);
                return -1;
            } else {
                McAuthMod.LOGGER.info(String.format("Get %s gameProfile success.", username));
            }
            if (McAuthMod.auth.register(player, null, true,
                    myProfileLookupCallback.gameProfile.getId())) {
                player.sendSystemMessage(new LiteralText("§aSetOnlineLogin success!!"), Util.NIL_UUID);
                return 0;
            } else {
                player.sendSystemMessage(new LiteralText("§cRegister failed."), Util.NIL_UUID);
                return -1;
            }
        } else {
            player.sendSystemMessage(new LiteralText("§cUsername does not match!"), Util.NIL_UUID);
            return -1;
        }
    }
}
