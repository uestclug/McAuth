package io.github.plusls.McAuth.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.plusls.McAuth.McAuthMod;
import io.github.plusls.McAuth.db.User;
import io.github.plusls.McAuth.util.Translator;
import net.minecraft.server.Whitelist;
import net.minecraft.server.WhitelistEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.command.WhitelistCommand;
import net.minecraft.text.LiteralText;

import java.util.*;

public class UpdateWhitelistCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, boolean dedicated) {
        dispatcher.register(CommandManager.literal("updateWhitelist")
                .requires(UpdateWhitelistCommand::requires)
                .executes(UpdateWhitelistCommand::execute));
    }

    private static boolean requires(ServerCommandSource source) {
        return source.hasPermissionLevel(4);
    }

    private static int execute(CommandContext<ServerCommandSource> commandContext) throws CommandSyntaxException {
        ServerCommandSource source = commandContext.getSource();
        Whitelist whitelist = source.getMinecraftServer().getPlayerManager().getWhitelist();
        List<GameProfile> newUserList = new ArrayList<>();
        List<User> authUserList = McAuthMod.auth.getUserList();
        Set<UUID> authUserUUIDSet = new HashSet<>();
        for (User user : authUserList) {
            GameProfile gameProfile = new GameProfile(user.uuid, user.username);
            if (!whitelist.isAllowed(new GameProfile(user.uuid, user.username))) {
                newUserList.add(gameProfile);
            }
            authUserUUIDSet.add(user.uuid);
        }
        if (newUserList.size() > 0) {
            WhitelistCommand.executeAdd(source, newUserList);
        }

        Map<String, Set<UUID>> userUUidMap = new HashMap<>();

        for (WhitelistEntry whitelistEntry : whitelist.values()) {
            GameProfile gameProfile = Objects.requireNonNull(whitelistEntry.getKey());
            if (!userUUidMap.containsKey(gameProfile.getName())) {
                userUUidMap.put(gameProfile.getName(), new HashSet<>());
            }
            userUUidMap.get(gameProfile.getName()).add(gameProfile.getId());
        }

        // 删除掉在白名单中但是不在数据库中的用户
        List<GameProfile> removeUserList = new ArrayList<>();
        for (Map.Entry<String, Set<UUID>> entry : userUUidMap.entrySet()) {
            // if (entry.getValue().size() > 1) {
            for (UUID uuid : entry.getValue()) {
                if (!authUserUUIDSet.contains(uuid)) {
                    removeUserList.add(new GameProfile(uuid, entry.getKey()));
                }
            }
            //}
        }
        if (removeUserList.size() > 0) {
            WhitelistCommand.executeRemove(source, removeUserList);
        }
        source.sendFeedback(new LiteralText(
                String.format(Translator.tr("mc_auth_mod.updateWhitelist.success"),
                        newUserList.size(),
                        removeUserList.size())), true);
        return 0;
    }
}
