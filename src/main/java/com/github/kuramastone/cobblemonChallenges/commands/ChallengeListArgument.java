package com.github.kuramastone.cobblemonChallenges.commands;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.exception.CommandErrorException;
import revxrsal.commands.fabric.actor.FabricCommandActor;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.parameter.ParameterType;
import revxrsal.commands.stream.MutableStringStream;

public class ChallengeListArgument implements ParameterType<FabricCommandActor, ChallengeList> {

    private CobbleChallengeAPI api;

    public ChallengeListArgument(CobbleChallengeAPI api) {
        this.api = api;
    }


    @Override
    public ChallengeList parse(@NotNull MutableStringStream input, @NotNull ExecutionContext<@NotNull FabricCommandActor> context) {
        String inText = input.readString();

        ChallengeList list = api.getChallengeList(inText);
        if(list == null) {
            throw new CommandErrorException("Unknown list: " + inText);
        }

        return list;
    }

    @Override
    public @NotNull SuggestionProvider<@NotNull FabricCommandActor> defaultSuggestions() {
        return it -> {
            return api.getChallengeLists().stream().map(ChallengeList::getName).toList();
        };
    }
}
