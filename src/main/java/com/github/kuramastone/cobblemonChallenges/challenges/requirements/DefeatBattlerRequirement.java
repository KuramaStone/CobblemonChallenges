package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.cobblemon.mod.common.api.battles.model.actor.AIBattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DefeatBattlerRequirement implements Requirement {
    public static final String ID = "Defeat_Battler";

    @YamlKey("pokename")
    public String pokename = "any";
    @YamlKey("amount")
    private int amount = 1;

    @YamlKey("shiny")
    private boolean shiny = false;
    @YamlKey("pokemon_type")
    public String pokemon_type = "any";
    @YamlKey("ball")
    private String ball = "any";
    @YamlKey("time_of_day")
    private String time_of_day = "any";
    @YamlKey("is_legendary")
    private boolean is_legendary = false;
    @YamlKey("is_ultra_beast")
    private boolean is_ultra_beast = false;
    @YamlKey("effectiveness")
    private String effectiveness = "any";
    @YamlKey("npc-player-gymleader-wild")
    private String enemyType = "any";
    @YamlKey("required-tags")
    private String requiredLabels = "any";

    public DefeatBattlerRequirement() {
    }

    public Requirement load(YamlConfig section) {
        return YamlConfig.loadFromYaml(this, section);
    }

    // The requirement name now returns the ID used to recognize it
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Progression<?> buildProgression(PlayerProfile profile) {
        return new DefeatPokemonProgression(profile, this);
    }

    // Static nested Progression class
    public static class DefeatPokemonProgression implements Progression<BattleVictoryEvent> {

        private PlayerProfile profile;
        public DefeatBattlerRequirement requirement;
        private int progressAmount;

        public DefeatPokemonProgression(PlayerProfile profile, DefeatBattlerRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<BattleVictoryEvent> getType() {
            return BattleVictoryEvent.class;
        }


        @Override
        public boolean isCompleted() {
            return progressAmount >= requirement.amount;
        }

        @Override
        public void progress(Object obj) {
            if (matchesMethod(obj)) {
                if (meetsCriteria(getType().cast(obj))) {
                    progressAmount++;
                }
            }
        }

        @Override
        public boolean meetsCriteria(BattleVictoryEvent event) {
            BattleActor player = event.getBattle().getActor(profile.getUUID());

            // make sure battle uses this player
            if(player == null)
                return false;

            if(event.getWasWildCapture())
                return false;

            // return if the player didnt win
            if(!event.getWinners().contains(player))
                return false;

            // check npc/player/gymleader/wild
            StringBuilder enemyType = new StringBuilder();
            List<BattlePokemon> enemyPokemon = new ArrayList<>();
            for (BattleActor participant : event.getLosers()) {
                enemyPokemon.addAll(participant.getPokemonList());
                if (participant instanceof EntityBackedBattleActor<?> entityBackedBattleActor) {
                    if (entityBackedBattleActor.getEntity() instanceof Player)
                        enemyType.append("player/");
                    else
                        enemyType.append("wild/");
                }
                else if (participant instanceof AIBattleActor aiBattleActor) {
                    enemyType.append("npc/");
                }
                else {
                    enemyType.append("wild/");
                }
            }

            // check if acceptable requirements contains any of the enemytypes we inserted
            if (!StringUtils.doesStringContainCategory(requirement.enemyType.split("/"), enemyType.toString())) {
                return false;
            }

            for (BattlePokemon battlePokemon : enemyPokemon) {
                Pokemon pokemon = battlePokemon.getOriginalPokemon();
                String pokename = pokemon.getSpecies().getName();
                boolean shiny = pokemon.getShiny();
                List<ElementalType> types = StreamSupport.stream(pokemon.getTypes().spliterator(), false).collect(Collectors.toUnmodifiableList());
                long time_of_day = CobbleChallengeMod.getMinecraftServer().getPlayerList().getPlayer(player.getUuid()).level().getDayTime();
                boolean is_legendary = pokemon.isLegendary();
                boolean is_ultra_beast = pokemon.isUltraBeast();

                if (!StringUtils.doesStringContainCategory(requirement.pokename.split("/"), pokename)) {
                    continue;
                }

                if(!StringUtils.doesListMeetRequiredList(requirement.requiredLabels.split("/"), pokemon.getForm().getLabels())) {
                    return false;
                }

                if (requirement.shiny && !shiny) {
                    continue;
                }

                if (types.stream().noneMatch(it -> StringUtils.doesStringContainCategory(requirement.pokemon_type.split("/"), it.getName()))) {
                    continue;
                }


                if (!requirement.time_of_day.toLowerCase().startsWith("any") &&
                        doesDaytimeMatch(time_of_day, requirement.time_of_day)) {
                    continue;
                }

                if (requirement.is_legendary && !is_legendary) {
                    continue;
                }

                if (requirement.is_ultra_beast && !is_ultra_beast) {
                    continue;
                }
                return true;
            }


            return false;
        }

        @Override
        public boolean matchesMethod(Object obj) {
            return getType().isInstance(obj);
        }

        @Override
        public double getPercentageComplete() {
            return (double) progressAmount / requirement.amount;
        }

        @Override
        public Progression loadFrom(UUID uuid, YamlConfig configurationSection) {
            this.progressAmount = configurationSection.getInt("progressAmount");
            return this;
        }

        @Override
        public void writeTo(YamlConfig configurationSection) {
            configurationSection.set("progressAmount", progressAmount);
        }

        @Override
        public String getProgressString() {
            return CobbleChallengeMod.instance.getAPI().getMessage("challenges.progression-string",
                    "{current}", String.valueOf(this.progressAmount),
                    "{target}", String.valueOf(this.requirement.amount)).getText();
        }
    }

    private static boolean doesDaytimeMatch(long time, String phrase) {
        long normalizedTime = time % 24000;

        // Convert the phrase to lowercase to handle case-insensitive comparison
        phrase = phrase.toLowerCase();

        switch (phrase) {
            case "any":
                return true;  // If 'any', always return true
            case "dawn":
                return normalizedTime >= 0 && normalizedTime < 1000;
            case "day":
                return normalizedTime >= 1000 && normalizedTime < 12000;
            case "dusk":
                return normalizedTime >= 12000 && normalizedTime < 13000;
            case "night":
                return normalizedTime >= 13000 && normalizedTime < 24000;
            default:
                // If the phrase doesn't match any expected value, return true anyways to be safe.
                return true;
        }

    }
}