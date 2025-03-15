package com.g8e.gameserver.models.entities;

public class EntityData {
    int npcIndex;
    String name = "";
    int type = 0;

    public EntityData(int npcIndex, String name, String examine, int respawnTime, int[] skills, int type,
            String spriteName) {
        this.npcIndex = npcIndex;
        this.name = name;
        this.type = type;
    }

    public int getNpcIndex() {
        return npcIndex;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

}
