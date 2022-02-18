package io.github.plusls.McAuth.command;

import com.google.common.base.Charsets;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.util.MyProfileLookupCallback;
import io.github.plusls.McAuth.util.Translator;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;

import java.util.Collections;
import java.util.UUID;

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

    private static int execute(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        ServerCommandSource source = commandContext.getSource();
        Entity sourceEntity = source.getEntity();
        if (!(sourceEntity instanceof ServerPlayerEntity)) {
            return -1;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) sourceEntity;
        String username = StringArgumentType.getString(commandContext, "username");
        if (player.getName().getString().equals(username)) {
            MyProfileLookupCallback myProfileLookupCallback = new MyProfileLookupCallback();

            player.server.getGameProfileRepo().findProfilesByNames(
                    new String[]{username}, Agent.MINECRAFT, myProfileLookupCallback);

            if (myProfileLookupCallback.gameProfile == null) {
                McAuthMod.LOGGER.info(String.format("Get %s gameProfile failed", username));
                source.sendFeedback(
                        new LiteralText(Translator.tr("mc_auth_mod.set_online_login.get_online_game_profile_failed")),
                        true);
                return -1;
            } else {
                McAuthMod.LOGGER.info(String.format("Get %s gameProfile success.", username));
            }
            if (McAuthMod.auth.register(player, null, true,
                    myProfileLookupCallback.gameProfile.getId())) {
                UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(Charsets.UTF_8));
                try {
                    if (source.getServer().getPlayerManager().isWhitelistEnabled() &&
                            WhitelistCommand.executeRemove(commandContext.getSource(),
                                    Collections.singleton(new GameProfile(offlineUuid, username))) != 0) {

                        source.sendFeedback(
                                new LiteralText(Translator.tr("mc_auth_mod.set_online_login.remove_whitelist_success")),
                                true);

                        // Add whitelist
                        if (WhitelistCommand.executeAdd(commandContext.getSource(),
                                Collections.singleton(myProfileLookupCallback.gameProfile)) != 0) {

                            source.sendFeedback(
                                    new LiteralText(Translator.tr("mc_auth_mod.set_online_login.add_whitelist_success")),
                                    true);
                        } else {

                            source.sendFeedback(
                                    new LiteralText(Translator.tr("mc_auth_mod.set_online_login.add_whitelist_failed")),
                                    true);
                        }
                    } else {
                        source.sendFeedback(
                                new LiteralText(Translator.tr("mc_auth_mod.set_online_login.remove_whitelist_failed")),
                                true);
                    }
                } finally {
                    source.sendFeedback(
                            new LiteralText(Translator.tr("mc_auth_mod.set_online_login.success")), true);
                    source.sendFeedback(
                            new LiteralText(Translator.tr(McAuthMod.auth.getHint(player))), true);
                }

                return 0;
            } else {
                source.sendFeedback(new LiteralText(Translator.tr("mc_auth_mod.set_online_login.failed")), true);
                return -1;
            }
        } else {
            source.sendFeedback(new LiteralText(Translator.tr("mc_auth_mod.set_online_login.username_not_match")), true);
            return -1;
        }
    }
}
