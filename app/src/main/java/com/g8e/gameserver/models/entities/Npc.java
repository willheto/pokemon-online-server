package com.g8e.gameserver.models.entities;

import com.g8e.gameserver.World;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.pokemon.Pokemon;
import com.g8e.gameserver.tile.TilePosition;

public class Npc extends Entity {

    public int npcIndex; // Index of the entity in the world's entity list

    public transient int originalWorldX;
    public transient int originalWorldY;

    public int wanderRange = 5;
    public int interactionRange = 1;
    public String interactionTargetID = null;

    public Npc(World world, int npcIndex, int worldX, int worldY, String name) {
        super("npc" + (int) (Math.random() * 1000000), world, worldX, worldY, name);
        this.npcIndex = npcIndex;
        this.originalWorldX = worldX;
        this.originalWorldY = worldY;
    }

    @Override
    public void update() {
        this.updateCounters();

        if (interactionTargetID != null) {
            Entity entity = this.world.getEntityByID(interactionTargetID);
            if (entity == null) {
                interactionTargetID = null;
                return;
            }

            int entityX = entity.worldX;
            int entityY = entity.worldY;

            int interactionRange = this.interactionRange * 2; // Adjust for 16px movement

            // Ensure interaction range aligns with 16x16 movement
            if (entityX < this.worldX - interactionRange || entityX > this.worldX + interactionRange
                    || entityY < this.worldY - interactionRange || entityY > this.worldY + interactionRange) {
                interactionTargetID = null;
                return;
            }

            // Face the target based on 16x16 movement grid
            if (entityX < this.worldX) {
                this.facingDirection = Direction.LEFT;
            } else if (entityX > this.worldX) {
                this.facingDirection = Direction.RIGHT;
            } else if (entityY < this.worldY) {
                this.facingDirection = Direction.UP;
            } else if (entityY > this.worldY) {
                this.facingDirection = Direction.DOWN;
            }

            this.targetTile = null;
            this.newTargetTile = null;
            this.nextTileDirection = null;
        }

        if (this.wanderRange != 0) {
            if (Math.random() < 0.01 && this.interactionTargetID == null) {
                this.setNewTargetTileWithinWanderArea();
            }

            if (this.isTargetTileNotWithinWanderArea()) {
                this.setNewTargetTileWithinWanderArea();
            }

            this.moveTowardsTarget();
        }

    }

    protected void setNewTargetTileWithinWanderArea() {
        int targetX = this.originalWorldX + (int) (Math.random() * (this.wanderRange * 2 + 1)
                - this.wanderRange);

        int targetY = this.originalWorldY + (int) (Math.random() * (this.wanderRange * 2 + 1)
                - this.wanderRange);

        // Adjust to always target the bottom-left tile of the 16x16 group
        int adjustedX = (targetX / 2) * 2; // Align to leftmost tile in 16x16 grid
        int adjustedY = (targetY / 2) * 2 + 1; // Align to bottom tile in 16x16 grid
        // Check if the new target tile is walkable
        boolean collision = this.world.tileManager.getCollisionByXandY(adjustedX, adjustedY);
        if (!collision) {
            this.newTargetTile = new TilePosition(adjustedX, adjustedY);
        }
    }

    protected boolean isTargetTileNotWithinWanderArea() {
        TilePosition targetTile = this.getTarget();
        if (targetTile == null) {
            return false;
        }

        return Math.abs(targetTile.x - this.originalWorldX) > this.wanderRange
                || Math.abs(targetTile.y - this.originalWorldY) > this.wanderRange;
    }

    public void setFacingDirection(Direction direction) {
        this.facingDirection = direction;
    }

    private void updateCounters() {

    }

    public void setWanderRange(int wanderRange) {
        this.wanderRange = wanderRange;
    }

    public void setInteractionRange(int interactionRange) {
        this.interactionRange = interactionRange;
    }

    public void addPokemonToParty(Pokemon pokemon) {

        for (int i = 0; i < this.party.length; i++) {
            if (this.party[i] == null) {
                this.party[i] = pokemon;
                return;
            }
        }
    }

}
