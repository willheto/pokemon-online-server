package com.g8e.gameserver.models.entities;

import java.util.List;

import com.g8e.gameserver.World;
import com.g8e.gameserver.battle.trainerBattle.Battle;
import com.g8e.gameserver.battle.wildBattle.WildBattle;
import com.g8e.gameserver.battle.wildBattle.WildPokemonEvent;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.models.ChatMessage;
import com.g8e.gameserver.models.Chunkable;
import com.g8e.gameserver.models.events.SoundEvent;
import com.g8e.gameserver.models.events.TalkEvent;
import com.g8e.gameserver.models.pokemon.Pokemon;
import com.g8e.gameserver.models.teleport.TeleportData;
import com.g8e.gameserver.pathfinding.AStar;
import com.g8e.gameserver.pathfinding.PathNode;
import com.g8e.gameserver.tile.Tile;
import com.g8e.gameserver.tile.TilePosition;

public abstract class Entity implements Chunkable {
    public String entityID; // Unique identifier for the entity
    public transient World world;
    public transient AStar pathFinder;
    public String targetedEntityID = null;

    public int worldX;
    public int worldY;

    public TilePosition targetTile = null;
    public TilePosition newTargetTile = null;
    public Direction nextTileDirection = null;

    public String name;

    protected int tickCounter = 0;
    public String targetItemID = null;
    public Direction facingDirection = Direction.DOWN;

    public List<PathNode> currentPath;
    protected TilePosition targetEntityLastPosition;
    protected Integer goalAction = null; // 1 talk, 2 attack, 3 trade

    public int currentChunk;

    public Pokemon[] party = new Pokemon[6];
    public String battleTargetID = null;

    public Battle battle = null;
    public WildBattle wildBattle = null;
    public boolean isForcedToMove = false;

    public int[] inventory = new int[20];
    public int[] inventoryAmounts = new int[20];

    public Entity(String entityID, World world, int worldX, int worldY, String name) {
        this.entityID = entityID;
        this.world = world;

        this.worldX = worldX;
        this.worldY = worldY;
        this.name = name;
        this.pathFinder = world.pathFinder;
        this.party = new Pokemon[6];
    }

    public abstract void update();

    @Override
    public int getCurrentChunk() {
        return this.currentChunk;
    }

    private TilePosition getPositionOneTileAwayFromTarget(TilePosition target) {
        // Create an array of tile offsets around the target
        TilePosition[] tilesAroundTarget = new TilePosition[] {
                new TilePosition(target.x, target.y - 1), // Up
                new TilePosition(target.x + 1, target.y), // Right
                new TilePosition(target.x, target.y + 1), // Down
                new TilePosition(target.x - 1, target.y), // Left
        };

        TilePosition closestTile = null;
        double minDistance = Double.MAX_VALUE;

        // Check each tile and calculate its distance from the current position
        for (TilePosition tile : tilesAroundTarget) {
            Tile currentTile = this.world.tileManager.getTileByXandY(tile.x, tile.y);

            if (currentTile != null && !currentTile.collision) {
                // Calculate Euclidean distance to the current position
                double distance = Math.sqrt(Math.pow(tile.x - this.worldX, 2) + Math.pow(tile.y - this.worldY, 2));

                if (distance < minDistance) {
                    minDistance = distance;
                    closestTile = tile;
                }
                if (distance == minDistance) {
                    // might as well pick a random one if they are the same distance
                    if (Math.random() > 0.5) {

                        minDistance = distance;
                        closestTile = tile;
                    }
                }
            }
        }

        return closestTile;
    }

    protected Direction getDirectionTowardsTile(int entityX, int entityY) {
        if (entityX < this.worldX) {
            return Direction.LEFT;
        } else if (entityX > this.worldX) {
            return Direction.RIGHT;
        } else if (entityY < this.worldY) {
            return Direction.UP;
        } else {
            return Direction.DOWN;
        }

    }

    protected void moveToNextTile() {
        if (this.nextTileDirection == null) {
            this.isForcedToMove = false;
            return;
        }
        switch (this.nextTileDirection) {
            case UP -> move(this.worldX, this.worldY - 1);
            case DOWN -> move(this.worldX, this.worldY + 1);
            case LEFT -> move(this.worldX - 1, this.worldY);
            case RIGHT -> move(this.worldX + 1, this.worldY);
            default -> {
            }
        }

    }

    private void setTalkTargetTile() {
        Entity entity = this.world.getEntityByID(this.targetedEntityID);
        if (entity != null) {

            TilePosition entityTile = new TilePosition(entity.worldX, entity.worldY);

            if (this.targetEntityLastPosition != null &&
                    this.targetEntityLastPosition.getX() == entityTile.getX()
                    && this.targetEntityLastPosition.getY() == entityTile.getY()) {
                return;
            }
            this.targetEntityLastPosition = entityTile;
            this.newTargetTile = getPositionOneTileAwayFromTarget(entityTile);

        } else {
            this.targetedEntityID = null;
        }

    }

    protected void moveTowardsTarget() {

        if (this.nextTileDirection != null) {
            moveToNextTile();
        }

        if (this.targetedEntityID != null) {
            setTalkTargetTile();
        }

        TilePosition target = this.getTarget();

        if (target == null) {
            this.isForcedToMove = false;
            return;
        }

        if (this.newTargetTile != null) {

            currentPath = this.pathFinder.findPath(this.worldX, this.worldY, target.x, target.y);

            if (currentPath.isEmpty()) {
                this.world.chatMessages
                        .add(new ChatMessage(name, "I can't reach that!", System.currentTimeMillis(), false));
            }

            if (currentPath.size() < 2) {
                currentPath = null;
                this.newTargetTile = null;
                this.targetTile = null;
                this.nextTileDirection = null;
                return;
            }
            this.targetTile = newTargetTile;
            this.newTargetTile = null;

            int deltaX = currentPath.get(1).x - this.worldX;
            int deltaY = currentPath.get(1).y - this.worldY;
            Direction nextTileDirection = this.getDirection(deltaX, deltaY);
            this.nextTileDirection = nextTileDirection;
            this.facingDirection = nextTileDirection;

            return;
        }

        if (currentPath == null || currentPath.isEmpty()) {
            this.world.chatMessages
                    .add(new ChatMessage(name, "I can't reach that!", System.currentTimeMillis(), false));
            return;
        }

        // Already at target
        if (currentPath.size() == 1) {
            this.nextTileDirection = null;
            this.targetTile = null;
            this.newTargetTile = null;
            return;
        }

        // Last step
        if (currentPath.size() == 2) {
            PathNode nextStep = currentPath.get(1);
            moveAlongPath(nextStep);
            if (this.targetItemID != null && this instanceof Player) {
                ((Player) this).takeItem(targetItemID);
                this.targetItemID = null;

            }

        } else if (currentPath.size() > 2) {
            PathNode nextStep = currentPath.get(1);
            PathNode nextNextStep = currentPath.get(2);
            moveAlongPath(nextStep, nextNextStep);
        }

    }

    protected void moveAlongPath(PathNode nextStep, PathNode nextNextStep) {
        /* move(nextStep.x, nextStep.y); */

        Direction nextTileDirection = this.getDirection(nextNextStep.x - nextStep.x, nextNextStep.y - nextStep.y);
        this.nextTileDirection = nextTileDirection;
        this.facingDirection = nextTileDirection;
        currentPath.remove(0);
    }

    // Last step
    protected void moveAlongPath(PathNode nextStep) {
        this.nextTileDirection = null;

        /* move(nextStep.x, nextStep.y); */
        this.targetTile = null;
        currentPath = null;
    }

    // Always use move instead of explicitly setting worldX and worldY
    // This will ensure that the chunk is updated correctly
    protected void move(int worldX, int worldY) {
        Tile tile = this.world.tileManager.getTileByXandY(worldX, worldY);

        if (tile.getNumberRepresentation() == 249 && this.worldY < worldY) {
            // if coming from up
            this.worldY += 3;
            this.worldX = worldX;
        } else if (tile.getNumberRepresentation() == 249) {

        } else {
            this.worldX = worldX;
            this.worldY = worldY;
        }
        if (this instanceof Player player) {
            TilePosition newTile = new TilePosition(worldX, worldY);
            TeleportData teleport = this.world.teleportsManager.checkTeleportTriggers(newTile);

            if (teleport != null) {
                if (teleport.isInstant()) {
                    player.worldX = teleport.getDestinationX();
                    player.worldY = teleport.getDestinationY();
                    player.facingDirection = teleport.getFacingDirectionAfterTeleport();
                    player.currentPath = null;
                    player.targetTile = null;
                    player.newTargetTile = null;
                    player.nextTileDirection = null;
                } else if (((Player) this).storyProgress < 3) {
                    this.isForcedToMove = true;
                    player.currentPath = null;
                    player.targetTile = null;
                    player.nextTileDirection = null;
                    player.newTargetTile = null;

                    Npc npc = this.world.npcs.stream().filter(e -> e.npcIndex == 3).findFirst().get();
                    npc.targetedEntityID = player.entityID;
                    npc.facingDirection = Direction.LEFT;
                    TalkEvent talkEvent = new TalkEvent(this.entityID, npc.entityID, npc.npcIndex, 1);
                    this.world.tickTalkEvents.add(talkEvent);
                    SoundEvent soundEvent = new SoundEvent("moms_theme.ogg", false, true, this.entityID);

                    this.world.tickSoundEvents.add(soundEvent);
                    // Set the player's target to the teleporter
                    this.newTargetTile = new TilePosition(teleport.getDestinationX(), teleport.getDestinationY());

                }
            }

            // check if on jumpable cliff

            player.savePosition();

        }
        updateChunk();

        if (this instanceof Player player) {
            boolean canWildPokemonAppearOnCurrentTile = this.world.tileManager.canWildPokemonAppearOnTile(this.worldX,
                    this.worldY);

            // 1/16 roll
            if (canWildPokemonAppearOnCurrentTile && Math.random() < 0.03125) {
                WildPokemonEvent wildPokemonEvent = new WildPokemonEvent(this.world, this.entityID);
                Pokemon encounter = wildPokemonEvent.rollRandomPokemon();
                this.world.tickWildPokemonEvents.add(wildPokemonEvent);
                WildBattle battle = new WildBattle(player, encounter);
                this.wildBattle = battle;
                this.currentPath = null;
                this.targetTile = null;
                this.newTargetTile = null;
                this.nextTileDirection = null;

            }
        }

    }

    private void updateChunk() {
        int chunk = this.world.tileManager.getChunkByWorldXandY(this.worldX, this.worldY);
        if (this.currentChunk != chunk) {
            this.currentChunk = chunk;

            if (this instanceof Player player) {
                player.needsFullChunkUpdate = true;
            }
        }
    }

    protected Direction getDirection(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY == 0) {
            return null;
        }

        if (deltaX == 0 && deltaY == -1) {
            return Direction.UP;
        }

        if (deltaX == 0 && deltaY == 1) {
            return Direction.DOWN;
        }

        if (deltaX == -1 && deltaY == 0) {
            return Direction.LEFT;
        }

        if (deltaX == 1 && deltaY == 0) {
            return Direction.RIGHT;
        }

        return null;
    }

    protected TilePosition getTarget() {
        return this.newTargetTile != null ? this.newTargetTile : this.targetTile;
    }

    public Integer getFreePartySlot() {
        for (int i = 0; i < this.party.length; i++) {
            if (this.party[i] == null) {
                return i;
            }
        }
        return null;
    }

    public void setParty(Pokemon[] party) {
        this.party = party;
    }

    public void faint() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'faint'");
    }

}
