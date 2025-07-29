package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class EvolvePokemonRequirement implements Requirement {
    public static final String ID = "Evolve_Pokemon";

    @YamlKey("pokename")
    public String pokename = "any";
    @YamlKey("amount")
    private int amount = 1;

    @YamlKey("shiny")
    private boolean shiny = false;
    @YamlKey("pokemon_type")
    private String pokemon_type = "any";
    @YamlKey("ball")
    private String ball = "any";
    @YamlKey("time_of_day")
    private String time_of_day = "any";
    @YamlKey("required-tags")
    private String requiredLabels = "any";

    public EvolvePokemonRequirement() {
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
        return new EvolvePokemonProgression(profile, this);
    }

    // Static nested Progression class
    public static class EvolvePokemonProgression implements Progression<EvolutionCompleteEvent> {

        private PlayerProfile profile;
        public EvolvePokemonRequirement requirement;
        private int progressAmount;

        public EvolvePokemonProgression(PlayerProfile profile, EvolvePokemonRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<EvolutionCompleteEvent> getType() {
            return EvolutionCompleteEvent.class;
        }

        @Override
        public String getProgressString() {
            return CobbleChallengeMod.instance.getAPI().getMessage("challenges.progression-string",
                    "{current}", String.valueOf(this.progressAmount),
                    "{target}", String.valueOf(this.requirement.amount)).getText();
        }

        @Override
        public boolean isCompleted() {
            return progressAmount >= requirement.amount;
        }

        @Override
        public void progress(Object obj) {
            if (matchesMethod(obj)) {
                if (meetsCriteria((EvolutionCompleteEvent) obj)) {
                    progressAmount++;
                }
            }
        }

        @Override
        public boolean meetsCriteria(EvolutionCompleteEvent event) {

            Pokemon pokemon = event.getPokemon();
            String pokename = pokemon.getSpecies().getName();
            boolean shiny = pokemon.getShiny();
            List<ElementalType> types = StreamSupport.stream(pokemon.getTypes().spliterator(), false).collect(Collectors.toUnmodifiableList());
            long time_of_day = event.getPokemon().getOwnerEntity().level().getDayTime();
            boolean is_legendary = pokemon.isLegendary();
            boolean is_ultra_beast = pokemon.isUltraBeast();

            if (!StringUtils.doesStringContainCategory(requirement.pokename.split("/"), pokename)) {
                return false;
            }

            if(!StringUtils.doesListMeetRequiredList(requirement.requiredLabels.split("/"), pokemon.getForm().getLabels())) {
                return false;
            }

            if (requirement.shiny && !shiny) {
                return false;
            }

            if (types.stream().noneMatch(it -> StringUtils.doesStringContainCategory(requirement.pokemon_type.split("/"), it.getName()))) {
                return false;
            }

            if (!requirement.time_of_day.toLowerCase().startsWith("any") &&
                    doesDaytimeMatch(time_of_day, requirement.time_of_day)) {
                return false;
            }

            return true;
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