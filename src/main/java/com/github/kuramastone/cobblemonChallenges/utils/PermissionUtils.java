package com.github.kuramastone.cobblemonChallenges.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minecraft.world.entity.player.Player;

public class PermissionUtils {

    public static boolean hasPermission(Player player, String permission) {
        LuckPerms api = LuckPermsProvider.get();

        User user = api.getUserManager().getUser(player.getUUID());
        return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
}
