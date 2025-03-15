package com.g8e.gameserver.battle.trainerBattle;

import com.g8e.gameserver.battle.enums.BattleOption;

public class BattleTurnEvent {
    public String playerID;
    public String opponentID;
    public int moveId;
    public int newHp;
    public boolean isBattleOver = false;
    public boolean isAllPokemonsFainted = false;
    public boolean isPlayerWinner = false;
    public boolean isRunSuccessful = false; // Flag for successful run attempts
    public int itemUsedId = -1; // For item usage tracking
    public int switchedPokemonIndex = -1; // For Pokémon switch tracking
    public int effect = 0; // For move effects
    public BattleOption actionType;
    public boolean isCaught = false; // Flag for successful Pokémon catches

    // Constructor for fight moves
    public BattleTurnEvent(String playerID, String opponentID, int moveId) {
        this.playerID = playerID;
        this.opponentID = opponentID;
        this.moveId = moveId;
        this.actionType = BattleOption.FIGHT;
    }

    // Generic constructor for ITEM and POKEMON actions
    public BattleTurnEvent(String playerID, String opponentID, int actionId,
            BattleOption actionType) {
        this.playerID = playerID;
        this.opponentID = opponentID;

        // Handle item usage or Pokémon switch based on the action type
        if (actionType == BattleOption.ITEM) {
            this.itemUsedId = actionId; // actionId is itemId for ITEM action
        } else if (actionType == BattleOption.POKEMON) {
            this.switchedPokemonIndex = actionId; // actionId is pokemonIndex for POKEMON action
        }
        this.actionType = actionType;
    }

    // Constructor for run attempts
    public BattleTurnEvent(String playerID, boolean isRunSuccessful) {
        this.playerID = playerID;
        this.isRunSuccessful = isRunSuccessful;
        this.actionType = BattleOption.RUN;
    }

    public void setBattleOver(boolean isPlayerWinner) {
        this.isBattleOver = true;
        this.isPlayerWinner = isPlayerWinner;
    }

    public boolean isBattleOver() {
        return isBattleOver;
    }

    public String getPlayerID() {
        return playerID;
    }

    public void setHp(int newHp) {
        this.newHp = newHp;
    }

    public void setEffect(int effect) {
        this.effect = effect;
    }
}
