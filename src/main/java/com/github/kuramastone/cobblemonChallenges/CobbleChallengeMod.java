package com.github.kuramastone.cobblemonChallenges;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.github.kuramastone.cobblemonChallenges.commands.CommandHandler;
import com.github.kuramastone.cobblemonChallenges.events.BlockBreakEvent;
import com.github.kuramastone.cobblemonChallenges.events.BlockPlaceEvent;
import com.github.kuramastone.cobblemonChallenges.events.PlayTimeScheduler;
import com.github.kuramastone.cobblemonChallenges.events.Played30SecondsEvent;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.listeners.TickScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class CobbleChallengeMod implements ModInitializer {

    public static String MODID = "cobblemonchallenges";

    public static CobbleChallengeMod instance;
    public static final Logger logger = LogManager.getLogger(MODID);
    private static MinecraftServer minecraftServer;
    private CobbleChallengeAPI api;

    @Override
    public void onInitialize() {
        instance = this;
        api = new CobbleChallengeAPI();
        api.init();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> minecraftServer = server); // capture minecraftserver
        CommandHandler.register(); // register commands
        registerTrackedEvents();
    }

    private void registerTrackedEvents() {
        ServerTickEvents.START_SERVER_TICK.register(TickScheduler::onServerTick);

        ChallengeListener.register();
        BlockBreakEvent.register();
        BlockPlaceEvent.register();
        ServerTickEvents.START_SERVER_TICK.register(PlayTimeScheduler::onServerTick);
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.HIGHEST, ChallengeListener::onPokemonCaptured);
        CobblemonEvents.POKEMON_SCANNED.subscribe(Priority.HIGHEST, ChallengeListener::onPokemonPokedexScanned);
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGHEST, ChallengeListener::onBattleVictory);
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(Priority.HIGHEST, ChallengeListener::onEvolution);
        CobblemonEvents.APRICORN_HARVESTED.subscribe(Priority.HIGHEST, ChallengeListener::onApricornHarvest);
        CobblemonEvents.BERRY_HARVEST.subscribe(Priority.HIGHEST, ChallengeListener::onBerryHarvest);
        CobblemonEvents.POKEMON_SEEN.subscribe(Priority.HIGHEST, ChallengeListener::onPokemonPokedexSeen);
        CobblemonEvents.EXPERIENCE_CANDY_USE_POST.subscribe(Priority.HIGHEST, ChallengeListener::onRareCandyUsed);
        CobblemonEvents.HATCH_EGG_POST.subscribe(Priority.HIGHEST, ChallengeListener::onEggHatch);
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_POST.subscribe(Priority.HIGHEST, ChallengeListener::onExpGained);
        CobblemonEvents.LEVEL_UP_EVENT.subscribe(Priority.HIGHEST, ChallengeListener::onLevelUp);
        CobblemonEvents.TRADE_COMPLETED.subscribe(Priority.HIGHEST, ChallengeListener::onTradeCompleted);
        CobblemonEvents.FOSSIL_REVIVED.subscribe(Priority.HIGHEST, ChallengeListener::onFossilRevived);
    }

    public CobbleChallengeAPI getAPI() {
        return api;
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public static File defaultDataFolder() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), MODID);
    }
}