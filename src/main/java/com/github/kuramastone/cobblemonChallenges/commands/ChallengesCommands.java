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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.fabric.actor.FabricCommandActor;
import revxrsal.commands.fabric.annotation.CommandPermission;

import java.util.concurrent.CompletableFuture;

@Command("challenges")
public class ChallengesCommands {

    private final CobbleChallengeAPI api;

    public ChallengesCommands() {
        api = CobbleChallengeMod.instance.getAPI();
    }

    @Subcommand("restart")
    @CommandPermission("challenges.commands.restart")
    public void handleRestartCommand(FabricCommandActor actor, ServerPlayer player) throws CommandSyntaxException {
        PlayerProfile profile = CobbleChallengeMod.instance.getAPI().getOrCreateProfile(player.getUUID());

        profile.resetChallenges();
        profile.addUnrestrictedChallenges();

        actor.sendRawMessage(FabricAdapter.adapt(api.getMessage("commands.restart")));
    }

    @Subcommand("reload")
    @CommandPermission("challenges.commands.reload")
    public void handleReloadCommand(FabricCommandActor actor) {
        api.reloadConfig();
        actor.sendRawMessage(FabricAdapter.adapt(api.getMessage("commands.reload")));
    }

    @CommandPlaceholder
    @CommandPermission("challenges.commands.challenge")
    public void handleChallengeListCommand(FabricCommandActor actor, @Optional ChallengeList challengeList) {
        try {
            ServerPlayer player = actor.requirePlayer();
            if(challengeList == null) {

                ChallengeMenuGUI gui = new ChallengeMenuGUI(api, api.getOrCreateProfile(player.getUUID()));
                gui.open();
                if (!player.hasContainerOpen())
                    player.displayClientMessage(FabricAdapter.adapt(api.getMessage("commands.opening-base-gui")), false);
            }
            else {
                new ChallengeListGUI(api, api.getOrCreateProfile(player.getUUID()), challengeList, api.getConfigOptions().getChallengeGuiConfig(challengeList.getName())).open();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
