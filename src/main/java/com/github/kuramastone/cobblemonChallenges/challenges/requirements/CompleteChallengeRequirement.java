package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.events.ChallengeCompletedEvent;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;

import java.util.UUID;

public class CompleteChallengeRequirement implements Requirement {

    public static final String ID = "complete_challenge";

    @YamlKey("challenge-list")
    private String challengeList = "any"; // The challenge list to track
    @YamlKey("amount")
    private int amount = 1; // Number of challenges to complete

    public CompleteChallengeRequirement() {
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
        CompleteChallengeProgression ccp = new CompleteChallengeProgression(profile, this);
        return ccp;
    }

    // Progression class to track progress
    public static class CompleteChallengeProgression implements Progression<ChallengeCompletedEvent> {

        private PlayerProfile profile;
        private CompleteChallengeRequirement requirement;
        private int progressAmount;

        public CompleteChallengeProgression(PlayerProfile profile, CompleteChallengeRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<ChallengeCompletedEvent> getType() {
            return ChallengeCompletedEvent.class;
        }

        @Override
        public boolean meetsCriteria(ChallengeCompletedEvent event) {

            if(!StringUtils.doesStringContainCategory(requirement.challengeList.split("/"), event.getChallengeList().getName())) {
                return false;
            }

            return true;
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
    }
}