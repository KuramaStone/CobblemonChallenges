package com.github.kuramastone.cobblemonChallenges;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.commands.ChallengeListArgument;
import com.github.kuramastone.cobblemonChallenges.commands.ChallengesCommands;
import com.github.kuramastone.cobblemonChallenges.commands.OldCommandHandler;
import com.github.kuramastone.cobblemonChallenges.events.*;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.listeners.TickScheduler;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import revxrsal.commands.Lamp;
import revxrsal.commands.fabric.FabricLamp;
import revxrsal.commands.fabric.FabricLampConfig;
import revxrsal.commands.fabric.actor.FabricCommandActor;

import java.io.File;
import java.util.concurrent.CompletableFuture;

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

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted); // capture minecraftserver
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> onStopped());
        startSaveScheduler();
        startRepeatableScheduler();
//        OldCommandHandler.register();
        registerCommands();
        registerTrackedEvents();
    }

    private void registerCommands() {
        Lamp<FabricCommandActor> lamp = FabricLamp.builder(FabricLampConfig.createDefault())
                .parameterTypes( it -> {
                    it.addParameterType(ChallengeList.class, new ChallengeListArgument(api));
                })
                .build();
        lamp.register(new ChallengesCommands());
    }

    private void onServerStarted(MinecraftServer server) {
        minecraftServer = server;
        api.init();
    }


    private void startSaveScheduler() {
        TickScheduler.scheduleRepeating(20 * 60 * 30, () -> {
            CompletableFuture.runAsync(() -> api.saveProfiles());
            return true;
        });
    }

    private void startRepeatableScheduler() {
        TickScheduler.scheduleRepeating(20, () -> {

            for (PlayerProfile profile : api.getProfiles()) {
                profile.refreshRepeatableChallenges();
            }

            return true;
        });
    }

    private void onStopped() {
        api.saveProfiles();
    }

    private void registerTrackedEvents() {
        ServerTickEvents.START_SERVER_TICK.register(TickScheduler::onServerTick);

        ChallengeListener.register();
        BlockBreakEvent.register();
        BlockPlaceEvent.register();
        PlayerJoinEvent.register();
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
