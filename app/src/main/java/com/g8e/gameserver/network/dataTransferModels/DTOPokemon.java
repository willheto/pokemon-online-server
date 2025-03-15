package com.g8e.gameserver.network.dataTransferModels;

public class DTOPokemon {

    final private int id; // pokemon id
    final private int xp;
    final private int hp;
    final private int maxHp;
    final private int[] moves;

    public DTOPokemon(int id, int xp, int hp, int maxHp, int[] moves) {
        this.id = id;
        this.xp = xp;
        this.hp = hp;
        this.maxHp = maxHp;
        this.moves = moves;
    }

    public int getId() {
        return id;
    }

    public int getXp() {
        return xp;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int[] getMoves() {
        return moves;
    }

}
