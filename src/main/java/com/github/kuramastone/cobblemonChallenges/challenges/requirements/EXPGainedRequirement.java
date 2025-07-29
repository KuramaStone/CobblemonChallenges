package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedPostEvent;
import com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedPostEvent;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.PixelmonUtils;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class EXPGainedRequirement implements Requirement {
    public static final String ID = "exp_gained";

    @YamlKey("pokename")
    private String pokename = "any";
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
    @YamlKey("is_legendary")
    private boolean is_legendary = false;
    @YamlKey("is_ultra_beast")
    private boolean is_ultra_beast = false;
    @YamlKey("required-tags")
    private String requiredLabels = "any";

    public EXPGainedRequirement() {
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
        return new ExpGainedProgression(profile, this);
    }

    // Static nested Progression class
    public static class ExpGainedProgression implements Progression<ExperienceGainedPostEvent> {

        private PlayerProfile profile;
        private EXPGainedRequirement requirement;
        private int progressAmount;

        public ExpGainedProgression(PlayerProfile profile, EXPGainedRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<ExperienceGainedPostEvent> getType() {
            return ExperienceGainedPostEvent.class;
        }

        @Override
        public boolean isCompleted() {
            return progressAmount >= requirement.amount;
        }

        @Override
        public void progress(Object obj) {
            if (matchesMethod(obj)) {
                if (meetsCriteria(getType().cast(obj))) {
                    ExperienceGainedPostEvent event = getType().cast(obj);
                    progressAmount += event.getExperience();
                }
            }
        }

        @Override
        public boolean meetsCriteria(ExperienceGainedPostEvent event) {

            Pokemon pokemon = event.getPokemon();
            String pokename = pokemon.getSpecies().getName();
            boolean shiny = pokemon.getShiny();
            List<ElementalType> types = StreamSupport.stream(pokemon.getTypes().spliterator(), false).collect(Collectors.toUnmodifiableList());
            String ballName = event.getPokemon().getCaughtBall().getName().toString();
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

            if (!requirement.pokemon_type.toLowerCase().startsWith("any") &&
                    types.stream().map(ElementalType::toString).noneMatch(requirement.pokemon_type::equalsIgnoreCase)) {
                return false;
            }

            if (!requirement.ball.toLowerCase().startsWith("any") &&
                    !ballName.toLowerCase().contains(requirement.ball.toLowerCase())) {
                return false;
            }

            if (!requirement.time_of_day.toLowerCase().startsWith("any") &&
                    !PixelmonUtils.doesDaytimeMatch(time_of_day, requirement.time_of_day)) {
                return false;
            }

            if (requirement.is_legendary && !is_legendary) {
                return false;
            }

            if (requirement.is_ultra_beast && !is_ultra_beast) {
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

        @Override
        public String getProgressString() {
            return CobbleChallengeMod.instance.getAPI().getMessage("challenges.progression-string",
                    "{current}", String.valueOf(this.progressAmount),
                    "{target}", String.valueOf(this.requirement.amount)).getText();
        }
    }
}