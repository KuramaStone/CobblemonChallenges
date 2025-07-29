package com.github.kuramastone.cobblemonChallenges;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.bUtilities.SimpleAPI;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.CompletedChallenge;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Progression;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.ConfigOptions;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.*;

public class CobbleChallengeAPI implements SimpleAPI {

    private Map<String, Challenge> allChallengesByName;
    private Map<String, ChallengeList> challengeListMap;
    private ConfigOptions configOptions;

    private Map<UUID, PlayerProfile> profileMap;

    public CobbleChallengeAPI() {
        profileMap = Collections.synchronizedMap(new HashMap<>());
        allChallengesByName = Collections.synchronizedMap(new HashMap<>());
    }

    public void init() {
        loadConfigs();
        loadProfiles();
    }

    public void loadProfiles() {
        YamlConfig data = new YamlConfig(CobbleChallengeMod.defaultDataFolder(), "player-data.yml");

        for (String strUUID : data.getKeys("", false)) {
            try {
                UUID uuid = UUID.fromString(strUUID);
                PlayerProfile profile = getOrCreateProfile(uuid, false);

                YamlConfig section = data.getSection(strUUID);
                // legacy from when they were saved as a list
                List<CompletedChallenge> completedChallenges = Collections.synchronizedList(new ArrayList<>());
                profile.setCompletedChallenges(completedChallenges);

                if (section.containsKey("completed-map")) {
                    YamlConfig completeSection = section.getSection("completed-map");

                    // due to an old bug, it can sometimes be double layered
                    if (completeSection.containsKey("completed-map")) {
                        completeSection = completeSection.getSection("completed-map");
                    }

                    for (String anyID : completeSection.getKeys("", false)) {
                        String challengeListID = completeSection.getString("%s.challengeListID".formatted(anyID));
                        String challengeID = completeSection.getString("%s.challengeID".formatted(anyID));
                        long lastTimeCompleted = completeSection.getLong("%s.timeCompleted".formatted(anyID));
                        completedChallenges.add(new CompletedChallenge(challengeListID, challengeID, lastTimeCompleted));
                    }
                }
                if (section.containsKey("progression")) {
                    YamlConfig progressionSection = section.getSection("progression");
                    // iterate over each challenge list
                    for (String strList : progressionSection.getKeys("", false)) {
                        YamlConfig listSection = progressionSection.getSection(strList);
                        ChallengeList list = getChallengeList(strList);

                        if(list != null) {
                            // iterate over each challenge
                            for (String strChallenge : listSection.getKeys("", false)) {
                                Challenge challenge = list.getChallenge(strChallenge);
                                // removed challenges are no longer loaded, ignore null challenges
                                if (challenge != null) {
                                    ChallengeProgress progress = list.buildNewProgressForQuest(challenge, profile);
                                    YamlConfig challengeSection = listSection.getSection(strChallenge);

                                    progress.setStartTime(challengeSection.containsKey("startTime") ?
                                            challengeSection.getLong("startTime") : System.currentTimeMillis());

                                    // iterate over each requirement for challenge
                                    int index = 0;
                                    for (Pair<String, Progression<?>> progSet : progress.getProgressionMap()) {
                                        YamlConfig progSection = challengeSection.getSection(index++ + "." + progSet.getKey());

                                        // if requirements change, this section may be null. ignore it.
                                        if (progSection != null)
                                            progSet.getValue().loadFrom(uuid, progSection);
                                    }

                                    profile.addActiveChallenge(progress);
                                }
                            }
                        }
                    }
                }

                // only add unrestricted challenges after adding saved challenges
                profile.addUnrestrictedChallenges();
            } catch (Exception e) {
                CobbleChallengeMod.logger.error("Failed to load cobblemonchallenges player profile: {}", strUUID);
                e.printStackTrace();
            }

        }

    }

    public synchronized void saveProfiles() {
        YamlConfig data = new YamlConfig(CobbleChallengeMod.defaultDataFolder(), "player-data.yml");
        data.clear();

        for (PlayerProfile profile : getProfiles()) {
            try {
                YamlConfig profileEntry = data.getOrCreateSection(profile.getUUID().toString());

                YamlConfig completedSection = data.getOrCreateSection("%s.completed-map".formatted(profile.getUUID()));
                for (CompletedChallenge completedChallenge : profile.getCompletedChallenges()) {
                    String challengeID = completedChallenge.challengeID();
                    completedSection.set("%s.challengeListID".formatted(challengeID), completedChallenge.challengeListID());
                    completedSection.set("%s.challengeID".formatted(challengeID), completedChallenge.challengeID());
                    completedSection.set("%s.timeCompleted".formatted(challengeID), completedChallenge.timeCompleted());
                }

                for (Map.Entry<String, List<ChallengeProgress>> set : profile.getActiveChallengesMap().entrySet()) {
                    for (ChallengeProgress cp : set.getValue()) {
                        YamlConfig challengeSection = profileEntry.getOrCreateSection(
                                "progression.%s.%s".formatted(set.getKey(), cp.getActiveChallenge().getName()));
                        challengeSection.set("startTime", cp.getStartTime());
                        int index = 0;
                        for (Pair<String, Progression<?>> progSet : cp.getProgressionMap()) {
                            YamlConfig progSection = challengeSection.getOrCreateSection(
                                    "%s.%s"
                                            .formatted(index++, progSet.getKey()));
                            progSet.getValue().writeTo(progSection);
                        }
                    }
                }
            } catch (Exception e) {
                CobbleChallengeMod.logger.error("Error saving player profile: {}", profile.getUUID());
                e.printStackTrace();
            }
        }

        data.save();
    }

    public void loadConfigs() {
        loadConfigOptions(); // load messages
        RequirementLoader.init(); // use messages for requirements loader
        loadChallenges(); // load challenges using the customized requirements
    }

    private void loadConfigOptions() {
        configOptions = new ConfigOptions();
        configOptions.load();
    }

    private void loadChallenges() {

        File parentFolder = new File(CobbleChallengeMod.defaultDataFolder(), "challenges");
        if (parentFolder.listFiles() == null || parentFolder.listFiles().length == 0) {
            parentFolder.delete();
        }

        challengeListMap = new HashMap<>();
        allChallengesByName = new HashMap<>();

        for (File yamlFile : YamlConfig.getYamlFiles(parentFolder)) {
            YamlConfig config = new YamlConfig(yamlFile.getParentFile(), null, null, yamlFile.getName(), getClass());

            // remove ending
            String name = yamlFile.getName();
            if (name.contains(".")) {
                name = name.substring(0, name.lastIndexOf('.')); // remove extension if possible
            }

            if (challengeListMap.containsKey(name)) {
                CobbleChallengeMod.logger.error("Unable to load duplicate ChallengeList '{}'. Try giving it a unique name.", name);
                continue;
            }

            ChallengeList cl = ChallengeList.load(this, name, config.getSection("challenge-list"));
            challengeListMap.put(name, cl);
            CobbleChallengeMod.logger.info("Loading {} config with {} challenges...", name, cl.getChallengeMap().size());
        }
    }

    /**
     * @param challenge
     * @return Returns false if a challenge is already registered under that name
     */
    public boolean registerChallenge(Challenge challenge) {
        if (this.allChallengesByName.containsKey(challenge.getName())) {
            return false;
        }

        this.allChallengesByName.put(challenge.getName(), challenge);
        return true;
    }

    public ConfigOptions getConfigOptions() {
        return configOptions;
    }

    public ChallengeList getChallengeList(String id) {
        return this.challengeListMap.get(id);
    }

    public CobbleChallengeMod getMod() {
        return CobbleChallengeMod.instance;
    }

    public PlayerProfile getOrCreateProfile(UUID uuid, boolean addChallenges) {
        PlayerProfile pp = profileMap.computeIfAbsent(uuid, id -> new PlayerProfile(this, id));
        if (addChallenges)
            pp.addUnrestrictedChallenges(); // add challenges that dont require selection
        return pp;
    }

    public PlayerProfile getOrCreateProfile(UUID uuid) {
        return getOrCreateProfile(uuid, true);
    }

    @Override
    public ComponentEditor getMessage(String key, Object... replacements) {
        ComponentEditor edit = configOptions.messages.getOrDefault(key, new ComponentEditor(key)).copy();

        // used to prevent infinite loops
        if (!key.equals("prefix")) {
            edit = edit.replace("{prefix}", getMessage("prefix").getText());
        }

        if (replacements.length % 2 != 0) {
            throw new IllegalArgumentException("Key was not provided with a replacement");
        }

        if (replacements.length > 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                edit = edit.replace(replacements[i].toString(), replacements[i + 1].toString());
            }
        }

        return edit;
    }

    public Collection<ChallengeList> getChallengeLists() {
        return new HashSet<>(challengeListMap.values());
    }

    public void reloadConfig() {
        saveProfiles();
        loadConfigOptions();
        loadChallenges();
        loadProfiles();
    }

    public List<PlayerProfile> getProfiles() {
        return new ArrayList<>(profileMap.values());
    }
}
