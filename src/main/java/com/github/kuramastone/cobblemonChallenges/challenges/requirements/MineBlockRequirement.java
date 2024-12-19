package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.events.BlockBreakEvent;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.UUID;

public class MineBlockRequirement implements Requirement {
    public static final String ID = "Mine_Block";

    @YamlKey("type")
    private String blockType = "any";
    @YamlKey("amount")
    private int amount = 1;

    public MineBlockRequirement() {
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
        return new MineBlockProgression(profile, this);
    }

    // Static nested Progression class
    public static class MineBlockProgression implements Progression<BlockBreakEvent> {

        private PlayerProfile profile;
        private MineBlockRequirement requirement;
        private int progressAmount;

        public MineBlockProgression(PlayerProfile profile, MineBlockRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<BlockBreakEvent> getType() {
            return BlockBreakEvent.class;
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
        public boolean meetsCriteria(BlockBreakEvent event) {

            String itemName = BuiltInRegistries.BLOCK.wrapAsHolder(event.getBlockState().getBlock()).getRegisteredName();

            if (!StringUtils.doesStringContainCategory(itemName, requirement.blockType)) {
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