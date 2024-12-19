package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.pokemon.PokemonSeenEvent;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;

import java.util.UUID;

public class PokemonSeenRequirement implements Requirement {

    public static final String ID = "pokemon_seen";

    @YamlKey("amount")
    private int amount = 1; // Number of Pokédex entries to complete
    @YamlKey(value = "pokename", required = false)
    private String pokename = "any"; // Number of Pokédex entries to complete

    public PokemonSeenRequirement() {
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
        /*
        Do not ask the player to scan more pokemon than possible
         */
        PokemonSeenProgression ccp = new PokemonSeenProgression(profile, this);
        int currentAmount = Cobblemon.INSTANCE.getPlayerDataManager().getPokedexData(profile.getUUID()).getSpeciesRecords().size(); // 99
        int maxPokedexEntries = PokemonSpecies.INSTANCE.count(); // 100
        int maxPossibleToGain = Math.max(0, maxPokedexEntries - currentAmount); // 1

        // cant ask them to gain more than the max
        ccp.progressAmount = this.amount - Math.min(this.amount, maxPossibleToGain);

        return ccp;
    }

    // Progression class to track progress
    public static class PokemonSeenProgression implements Progression<PokemonSeenEvent> {

        private PlayerProfile profile;
        private PokemonSeenRequirement requirement;
        public int progressAmount;

        public PokemonSeenProgression(PlayerProfile profile, PokemonSeenRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<PokemonSeenEvent> getType() {
            return PokemonSeenEvent.class;
        }

        @Override
        public boolean meetsCriteria(PokemonSeenEvent event) {

            if (!StringUtils.doesStringContainCategory(event.getPokemon().getSpecies().getName(), requirement.pokename)) {
                return false;
            }

            return true;
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
