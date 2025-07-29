package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.cobblemon.mod.common.api.events.pokemon.interaction.ExperienceCandyUseEvent;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UseRareCandyRequirement implements Requirement {
    public static final String ID = "use_rare_candy";

    @YamlKey("pokename")
    private String pokename = "any"; // Default to "any" if not specified
    @YamlKey("amount")
    private int amount = 1;
    @YamlKey("required-tags")
    private String requiredLabels = "any";

    public UseRareCandyRequirement() {
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Requirement load(YamlConfig section) {
        return YamlConfig.loadFromYaml(this, section);
    }

    @Override
    public Progression<?> buildProgression(PlayerProfile profile) {
        return new UseRareCandyOnProgression(profile, this);
    }

    // Progression class to track the use of Rare Candies
    public static class UseRareCandyOnProgression implements Progression<ExperienceCandyUseEvent> {

        private PlayerProfile profile;
        private UseRareCandyRequirement requirement;
        private int progressAmount;

        public UseRareCandyOnProgression(PlayerProfile profile, UseRareCandyRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<ExperienceCandyUseEvent> getType() {
            return ExperienceCandyUseEvent.class;
        }

        @Override
        public boolean meetsCriteria(ExperienceCandyUseEvent event) {
            // Get the Pokémon name being used with the Rare Candy
            String pokemonName = event.getPokemon().getSpecies().getName();

            if(!StringUtils.doesListMeetRequiredList(requirement.requiredLabels.split("/"), event.getPokemon().getForm().getLabels())) {
                return false;
            }

            // Check if the Pokémon name meets the requirement
            return StringUtils.doesStringContainCategory(requirement.pokename.split("/"), pokemonName);
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
                    progressAmount = Math.min(progressAmount, this.requirement.amount);
                }
            }
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
