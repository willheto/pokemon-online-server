package com.g8e.gameserver.network.actions;

public class HealPokemonAction extends Action {

    final private int npcIndex;

    public HealPokemonAction(String playerID, int pokemonIndex, int npcIndex) {
        this.action = "healPokemon";
        this.playerID = playerID;
        this.npcIndex = pokemonIndex;
    }

    public int getNpcIndex() {
        return npcIndex;
    }

}
