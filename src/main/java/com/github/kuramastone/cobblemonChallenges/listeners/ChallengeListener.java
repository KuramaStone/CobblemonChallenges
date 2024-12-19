package com.github.kuramastone.cobblemonChallenges.listeners;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.berry.BerryHarvestEvent;
import com.cobblemon.mod.common.api.events.farming.ApricornHarvestEvent;
import com.cobblemon.mod.common.api.events.pokedex.scanning.PokemonScannedEvent;
import com.cobblemon.mod.common.api.events.pokemon.*;
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent;
import com.cobblemon.mod.common.api.events.pokemon.interaction.ExperienceCandyUseEvent;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.events.BlockBreakEvent;
import com.github.kuramastone.cobblemonChallenges.events.BlockPlaceEvent;
import com.github.kuramastone.cobblemonChallenges.events.ChallengeCompletedEvent;
import com.github.kuramastone.cobblemonChallenges.events.Played30SecondsEvent;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import kotlin.Unit;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class ChallengeListener {

    private static CobbleChallengeAPI api;

    public static void register() {
        api = CobbleChallengeMod.instance.getAPI();
    }

    public static void passEvent(Object event, UUID player) {

        PlayerProfile profile = api.getOrCreateProfile(player);
        for (ChallengeProgress activeChallenge : profile.getActiveChallenges()) {
            activeChallenge.progress(event);
        }

    }

    public static void passEvent(Object event, Player player) {
        passEvent(event, player.getUUID());
    }

    public static void onBlockBreak(BlockBreakEvent event) {
        passEvent(event, event.getPlayer());
    }

    public static void onChallengeCompleted(ChallengeCompletedEvent event) {
        passEvent(event, event.getPlayerProfile().getUUID());
    }

    public static void onBlockPlace(BlockPlaceEvent event) {
        passEvent(event, event.getPlayer());
    }

    public static Unit onPokemonCaptured(PokemonCapturedEvent event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onPokemonPokedexScanned(PokemonScannedEvent event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onBattleVictory(BattleVictoryEvent event) {
        for (BattleActor winner : event.getWinners()) {
            for (UUID playerUUID : winner.getPlayerUUIDs()) {
                passEvent(event, playerUUID);
            }
        }
        return null;
    }

    public static Unit onEvolution(EvolutionCompleteEvent event) {
        passEvent(event, event.getPokemon().getOwnerPlayer());
        return null;
    }

    public static Unit onApricornHarvest(ApricornHarvestEvent event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onBerryHarvest(BerryHarvestEvent event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onPokemonPokedexSeen(PokemonSeenEvent event) {
        passEvent(event, event.getPlayerId());
        return null;
    }

    public static Unit onRareCandyUsed(ExperienceCandyUseEvent.Post event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static void on30SecondsPlayed(Played30SecondsEvent event) {
        passEvent(event, event.getPlayerProfile().getPlayerEntity());
    }

    public static Unit onEggHatch(HatchEggEvent.Post event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onExpGained(ExperienceGainedPostEvent event) {
        passEvent(event, event.getPokemon().getOwnerPlayer());
        return null;
    }

    public static Unit onLevelUp(LevelUpEvent event) {
        passEvent(event, event.getPokemon().getOwnerPlayer());
        return null;
    }

    public static Unit onTradeCompleted(TradeCompletedEvent event) {
        passEvent(event, event.getTradeParticipant1().getUuid());
        return null;
    }

    public static Unit onFossilRevived(FossilRevivedEvent event) {
        passEvent(event, event.getPlayer());
        return null;
    }
}
