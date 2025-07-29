package com.github.kuramastone.cobblemonChallenges;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.*;
import com.github.kuramastone.cobblemonChallenges.events.RegisterRequirementsEvent;

import java.util.HashMap;
import java.util.Map;

public class RequirementLoader {

    private static Map<String, Class<? extends Requirement>> requirementMap;

    public static void init() {
        requirementMap = new HashMap<>();

        register(CatchPokemonRequirement.class);
        register(CompleteChallengeRequirement.class);
        register(PokemonScannedRequirement.class);
        register(DefeatBattlerRequirement.class);
        register(EvolvePokemonRequirement.class);
        register(HarvestApricornRequirement.class);
        register(HarvestBerryRequirement.class);
        register(MilestoneTimePlayedRequirement.class);
        register(MineBlockRequirement.class);
        register(PlaceBlockRequirement.class);
        register(UseRareCandyRequirement.class);
        register(PokemonSeenRequirement.class);

        register(HatchEggRequirement.class);
        register(EXPGainedRequirement.class);
        register(LevelUpToRequirement.class);
        register(IncreaseLevelRequirement.class);
        register(TradeCompletedRequirement.class);
        register(FossilRevivedRequirement.class);

        register(LoginRequirement.class);

        // register
        RegisterRequirementsEvent.EVENT.invoker().onRegistration();

    }

    public static void register(Class<? extends Requirement> clazz) {
        try {
            String id = (String) clazz.getDeclaredField("ID").get(null);
            id = id.toLowerCase();
            //PixelChallengeMod.logger.info(String.format("Registering class '%s' under id '%s'.", clazz.getSimpleName(), id));
            requirementMap.put(id, clazz);
        }
        catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Cannot find 'public static final String ID' in this requirement. This is required in order to associate it in the config.", e);
        }
    }


    public static Requirement load(String challengeID, String requirementType, YamlConfig section) {
        Class<? extends Requirement> clazz = requirementMap.get(requirementType);

        if(clazz == null) {
            CobbleChallengeMod.logger.error("Cannot load requirement for a requirement '{}' for challenge '{}'. This requirement does not exist.", requirementType, challengeID);
            return null;
        }

        try {
            return clazz.newInstance().load(section);
        }
        catch (Exception e) {
            CobbleChallengeMod.logger.error(String.format("Cannot load requirement for a requirement '%s' for challenge %s. Unknown cause.", requirementType, challengeID));
            e.printStackTrace();
            return null;
        }
    }

}
