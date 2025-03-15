package com.g8e.gameserver.network.dataTransferModels;

import java.util.Objects;

import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.Chunkable;
import com.g8e.gameserver.models.entities.Player;
import com.g8e.gameserver.models.pokemon.Pokemon;

public class DTOPlayer implements Chunkable {

    public int storyProgress;
    public int[] inventory;
    public int[] inventoryAmounts;

    // Entity fields
    public String entityID;
    public int worldX;
    public int worldY;
    public Direction nextTileDirection = null;
    public Direction facingDirection = Direction.DOWN;
    public String name;
    public int currentChunk; // Debugging purposes only
    public DTOPokemon[] party = new DTOPokemon[6];

    public DTOPlayer(Player player) {
        this.entityID = player.entityID;
        this.worldX = player.worldX;
        this.worldY = player.worldY;
        this.nextTileDirection = player.nextTileDirection;
        this.facingDirection = player.facingDirection;
        this.name = player.name;
        this.currentChunk = player.currentChunk;
        for (int i = 0; i < player.party.length; i++) {
            if (player.party[i] != null) {
                Pokemon p = player.party[i];
                this.party[i] = new DTOPokemon(p.getId(), p.getXp(), p.getHp(), p.getMaxHp(), p.getMoves());
            }
        }
        this.inventory = player.inventory;
        this.inventoryAmounts = player.inventoryAmounts;
        this.storyProgress = player.storyProgress;
    }

    @Override
    public int getCurrentChunk() {
        return this.currentChunk;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        DTOPlayer other = (DTOPlayer) obj;

        return Objects.equals(this.entityID, other.entityID) &&
                this.worldX == other.worldX &&
                this.worldY == other.worldY &&
                Objects.equals(this.nextTileDirection, other.nextTileDirection) &&
                Objects.equals(this.facingDirection, other.facingDirection) &&
                Objects.equals(this.name, other.name) &&
                this.currentChunk == other.currentChunk &&
                Objects.equals(this.party, other.party) &&
                Objects.equals(this.inventory, other.inventory) &&
                Objects.equals(this.inventoryAmounts, other.inventoryAmounts) &&
                this.storyProgress == other.storyProgress;
    }

    public String getEntityID() {
        return entityID;
    }

}
