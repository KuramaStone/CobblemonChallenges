package com.github.kuramastone.cobblemonChallenges.commands;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.guis.ChallengeListGUI;
import com.github.kuramastone.cobblemonChallenges.guis.ChallengeMenuGUI;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import com.github.kuramastone.cobblemonChallenges.utils.PermissionUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.CompletableFuture;

public class OldCommandHandler {

    private static CobbleChallengeAPI api;

    public static void register() {
        api = CobbleChallengeMod.instance.getAPI();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("challenges")
                    .requires(source -> hasPermission(source, "challenges.commands.challenge"))
                    .executes(OldCommandHandler::handleChallengeBaseCommand)
                    .then(Commands.argument("list", StringArgumentType.word())
                            .suggests(OldCommandHandler::handleListSuggestions)
                            .executes(OldCommandHandler::handleChallengeListCommand)
                    )
                    .then(Commands.literal("reload")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.reload"))
                            .executes(OldCommandHandler::handleReloadCommand))
                    .then(Commands.literal("reset")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.restart"))
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(OldCommandHandler::handleRestartCommand)))
            );
        });

    }

    private static int handleRestartCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = EntityArgument.getPlayer(context, "player");
        PlayerProfile profile = CobbleChallengeMod.instance.getAPI().getOrCreateProfile(player.getUUID());

        profile.resetChallenges();
        profile.addUnrestrictedChallenges();

        context.getSource().sendSystemMessage(FabricAdapter.adapt(api.getMessage("commands.restart")));
        return 1;
    }


    private static boolean hasPermission(CommandSourceStack source, String perm) {
        return source.hasPermission(2) || (source.isPlayer() && PermissionUtils.hasPermission(source.getPlayer(), perm));
    }

    private static CompletableFuture<Suggestions> handleListSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        // Define available options for completion
        for (ChallengeList cl : api.getChallengeLists()) {
            // Add each option to the suggestions
            builder.suggest(cl.getName());
        }

        return builder.buildFuture();
    }

    private static int handleReloadCommand(CommandContext<CommandSourceStack> context) {
        api.reloadConfig();
        context.getSource().sendSystemMessage(FabricAdapter.adapt(api.getMessage("commands.reload")));
        return 1;
    }

    private static int handleChallengeBaseCommand(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            if (!source.isPlayer()) {
                source.sendSystemMessage(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
                return 1;
            }

            ServerPlayer player = (ServerPlayer) source.getEntity();

            ChallengeMenuGUI gui = new ChallengeMenuGUI(api, api.getOrCreateProfile(player.getUUID()));
            gui.open();
            if (!player.hasContainerOpen())
                player.displayClientMessage(FabricAdapter.adapt(api.getMessage("commands.opening-base-gui")), false);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static int handleChallengeListCommand(CommandContext<CommandSourceStack> context) {
        try {
            String listName = StringArgumentType.getString(context, "list");

            CommandSourceStack source = context.getSource();

            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
                return 1;
            }

            ServerPlayer player = source.getPlayer();
            ChallengeList challengeList = api.getChallengeList(listName);

            if (challengeList == null) {
                source.sendFailure(FabricAdapter.adapt(api.getMessage("issues.unknown_challenge_list", "{challenge_list}", listName)));
                return 1;
            }

            new ChallengeListGUI(api, api.getOrCreateProfile(player.getUUID()), challengeList, api.getConfigOptions().getChallengeGuiConfig(challengeList.getName())).open();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}