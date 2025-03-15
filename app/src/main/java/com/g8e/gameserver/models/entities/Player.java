package com.g8e.gameserver.models.entities;

import java.sql.SQLException;
import java.util.List;

import com.g8e.db.CommonQueries;
import com.g8e.db.models.DBPlayer;
import com.g8e.gameserver.World;
import com.g8e.gameserver.battle.enums.BattleOption;
import com.g8e.gameserver.battle.trainerBattle.Battle;
import com.g8e.gameserver.battle.trainerBattle.BattleEvent;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.enums.Pokecenter;
import com.g8e.gameserver.models.ChatMessage;
import com.g8e.gameserver.models.events.SoundEvent;
import com.g8e.gameserver.models.events.TalkEvent;
import com.g8e.gameserver.models.objects.Item;
import com.g8e.gameserver.models.pokemon.Pokemon;
import com.g8e.gameserver.models.pokemon.PokemonMove;
import com.g8e.gameserver.network.actions.Action;
import com.g8e.gameserver.network.actions.HealPokemonAction;
import com.g8e.gameserver.network.actions.move.BattleAction;
import com.g8e.gameserver.network.actions.move.ChallengePlayerAction;
import com.g8e.gameserver.network.actions.move.ForceNpcBattlePlayerAction;
import com.g8e.gameserver.network.actions.move.PlayerMove;
import com.g8e.gameserver.network.actions.move.PlayerTakeMoveAction;
import com.g8e.gameserver.network.actions.move.PlayerTalkMoveAction;
import com.g8e.gameserver.network.actions.move.StartBattleAction;
import com.g8e.gameserver.network.actions.story.UpdateStoryProgressAction;
import com.g8e.gameserver.tile.TilePosition;
import com.g8e.util.Logger;
import com.google.gson.Gson;

public class Player extends Entity {

    public int accountID;
    public int playerID;

    public int storyProgress;
    private int lastPokecenter = Pokecenter.NEW_BARK_TOWN; // Where to respawn when player faints
    public Integer pendingStarterPokemonID = null;

    public boolean needsFullChunkUpdate = false;

    public Player(World world, DBPlayer dbPlayer, String uniquePlayerID, String username, int accountID) {
        super(uniquePlayerID, world, dbPlayer.getWorldX(), dbPlayer.getWorldY(), username);
        this.accountID = accountID;
        this.playerID = dbPlayer.getPlayerID();
        this.currentChunk = world.tileManager.getChunkByWorldXandY(dbPlayer.getWorldX(), dbPlayer.getWorldY());
        this.inventory = dbPlayer.getInventory();
        this.inventoryAmounts = dbPlayer.getInventoryAmounts();
        this.storyProgress = dbPlayer.getStoryProgress();
        this.lastPokecenter = dbPlayer.getLastPokecenter();
    }

    @Override
    public int getCurrentChunk() {
        return this.currentChunk;
    }

    @Override
    public void update() {
        this.moveTowardsTarget();

        if (this.targetedEntityID != null) {
            if (goalAction == null) {
                Logger.printError("Goal action is null, but targeted entity is not null!");
                targetedEntityID = null;
                return;
            }

            if (isOneStepAwayFromTarget()) {
                if (goalAction == 1) {
                    Entity entity = this.world.getEntityByID(this.targetedEntityID);
                    if (entity != null && entity instanceof Npc) {
                        this.handleNpcTalk((Npc) entity);
                    }
                }
            }

        }
    }

    private void handleNpcTalk(Npc npc) {
        this.nextTileDirection = null;
        this.targetTile = null;
        this.newTargetTile = null;
        this.targetEntityLastPosition = null;
        this.goalAction = null;
        this.targetedEntityID = null;
        TalkEvent talkEvent = new TalkEvent(this.entityID, npc.entityID, npc.npcIndex);
        this.isForcedToMove = false;

        if (npc.targetedEntityID == null) {
            npc.interactionTargetID = this.entityID;
        }

        int entityX = npc.worldX;
        int entityY = npc.worldY;

        this.facingDirection = this.getDirectionTowardsTile(entityX, entityY);
        this.world.tickTalkEvents.add(talkEvent);

    }

    private boolean isOneStepAwayFromTarget() {
        Entity target = this.world.getEntityByID(this.targetedEntityID);
        if (target == null) {
            Logger.printError("Target not found");
            return false;
        }
        // Players move in 16x16 steps, so check if they are exactly 2 tiles (16px) away

        return (Math.abs(this.worldX - target.worldX) == 2 && this.worldY == target.worldY)
                || (Math.abs(this.worldY - target.worldY) == 2 && this.worldX == target.worldX);
    }

    public void savePosition() {
        try {
            CommonQueries.savePlayerPositionByAccountId(this.accountID, this.worldX, this.worldY);
        } catch (Exception e) {
            Logger.printError("Failed to save player position");
        }
    }

    public void saveCurrentStoryProgress() {
        try {
            CommonQueries.savePlayerStoryProgressByAccountId(this.accountID, this.storyProgress);
        } catch (Exception e) {
            Logger.printError("Failed to save player story progress");
        }
    }

    public void saveLastPokecenter() {
        try {
            CommonQueries.savePlayerLastPokecenterByAccountId(this.accountID, this.lastPokecenter);
        } catch (Exception e) {
            Logger.printError("Failed to save last pokecenter");
        }
    }

    public void setTickActions(List<Action> actions) {
        if (this.isForcedToMove == true) {
            return;
        }
        for (Action action : actions) {

            if (action instanceof PlayerMove playerMove) {
                int targetX = playerMove.getX();
                int targetY = playerMove.getY();

                // Adjust to always target the bottom-left tile of the 16x16 group
                int adjustedX = (targetX / 2) * 2; // Align to leftmost tile in 16x16 grid
                int adjustedY = (targetY / 2) * 2 + 1; // Align to bottom tile in 16x16 grid

                this.newTargetTile = new TilePosition(adjustedX, adjustedY);
            }

            if (action instanceof PlayerTalkMoveAction playerTalkMoveAction) {

                Entity entity = this.world.getEntityByID(playerTalkMoveAction.getEntityID());
                if (entity != null) {
                    this.targetedEntityID = playerTalkMoveAction.getEntityID();
                    this.goalAction = 1;

                    int adjustedX = (entity.worldX / 2) * 2; // Align to leftmost tile in 16x16 grid
                    int adjustedY = (entity.worldY / 2) * 2 + 1; // Align to bottom tile in 16x16 grid
                    this.newTargetTile = new TilePosition(adjustedX, adjustedY);
                }
            }

            if (action instanceof ForceNpcBattlePlayerAction forceNpcAttackPlayerAction) {
                Entity entity = this.world.getEntityByID(forceNpcAttackPlayerAction.getNpcID());
                if (entity != null && entity instanceof Npc) {
                    ((Npc) entity).battleTargetID = this.entityID;
                    this.battleTargetID = entity.entityID;
                    BattleEvent battleEvent = new BattleEvent(this.entityID, entity.entityID);
                    this.world.tickBattleEvents.add(battleEvent);

                    Battle battle = new Battle(this, entity);

                    this.battle = battle;
                    entity.battle = battle;

                }
            }

            if (action instanceof ChallengePlayerAction challengePlayerAction) {
                Player target = (Player) this.world.getEntityByID(challengePlayerAction.getTargetID());

                if (target != null) {
                    // send chat message to target
                    ChatMessage chatMessage = new ChatMessage(target.name,
                            this.name + " has challenged you to a battle!",
                            System.currentTimeMillis(), false, true, this.entityID);

                    this.world.addChatMessage(chatMessage);
                }
            }

            if (action instanceof StartBattleAction startBattleAction) {
                Player target = (Player) this.world.getEntityByID(startBattleAction.getTargetID());

                if (target != null) {
                    BattleEvent battleEvent = new BattleEvent(this.entityID, target.entityID);
                    this.world.tickBattleEvents.add(battleEvent);

                    Battle battle = new Battle(this, target);

                    this.battle = battle;
                    target.battle = battle;

                }
            }

            if (action instanceof BattleAction battleAction) {
                if (this.battle != null) {
                    boolean isPlayerEntity1 = this.battle.getEntity1().entityID.equals(this.entityID);

                    switch (battleAction.getOption()) {
                        case FIGHT -> {

                            PokemonMove move = this.world.pokemonMovesManager
                                    .getPokemonMoveById(battleAction.getMoveId());
                            if (isPlayerEntity1) {
                                this.battle.setEntity1PendingAction(BattleOption.FIGHT, move.getId());
                            } else {
                                this.battle.setEntity2PendingAction(BattleOption.FIGHT, move.getId());
                            }
                        }
                        case ITEM -> {
                            if (isPlayerEntity1) {
                                this.battle.setEntity1PendingAction(BattleOption.ITEM, battleAction.getItemId());
                            } else {
                                this.battle.setEntity2PendingAction(BattleOption.ITEM, battleAction.getItemId());
                            }
                        }
                        case POKEMON -> {
                            if (isPlayerEntity1) {
                                this.battle.setEntity1PendingAction(BattleOption.POKEMON,
                                        battleAction.getPokemonIndex());
                            } else {
                                this.battle.setEntity2PendingAction(BattleOption.POKEMON,
                                        battleAction.getPokemonIndex());
                            }
                        }
                        case RUN -> {
                            if (isPlayerEntity1) {
                                this.battle.setEntity1PendingAction(BattleOption.RUN, null);
                            } else {
                                this.battle.setEntity2PendingAction(BattleOption.RUN, null);
                            }
                        }
                    }
                } else if (this.wildBattle != null) {
                    switch (battleAction.getOption()) {
                        case FIGHT -> {

                            PokemonMove move = this.world.pokemonMovesManager
                                    .getPokemonMoveById(battleAction.getMoveId());
                            this.wildBattle.setEntityPendingAction(BattleOption.FIGHT, move.getId());
                        }
                        case ITEM -> {
                            this.wildBattle.setEntityPendingAction(BattleOption.ITEM, battleAction.getItemId());
                        }
                        case POKEMON -> {
                            this.wildBattle.setEntityPendingAction(BattleOption.POKEMON,
                                    battleAction.getPokemonIndex());
                        }
                        case RUN -> {
                            this.wildBattle.setEntityPendingAction(BattleOption.RUN, null);
                        }
                    }

                }
            }

            if (action instanceof PlayerTakeMoveAction playerTakeMoveAction) {
                this.handlePlayerTakeMove(playerTakeMoveAction.getUniqueItemID());
                this.goalAction = null;
            }

            if (action instanceof UpdateStoryProgressAction updateStoryProgressAction) {
                this.storyProgress = updateStoryProgressAction.getProgress();
                if (this.storyProgress == 3 && this.pendingStarterPokemonID != null) {
                    Pokemon starter = new Pokemon(this.pendingStarterPokemonID, 5);
                    starter.saveUsersNewPokemon(playerID);
                    this.addPokemonToParty(starter);
                }
                this.saveCurrentStoryProgress();
            }

            if (action instanceof HealPokemonAction healPokemonAction) {
                this.healAllPokemon();
                int npcIndex = healPokemonAction.getNpcIndex();

                switch (npcIndex) {
                    case 11 -> this.lastPokecenter = Pokecenter.CHERRYGROVE_CITY;
                }

                this.saveLastPokecenter();
            }

        }
    }

    private boolean isItemOneStepAwayFromTarget() {
        Item target = this.world.itemsManager.getItemByUniqueItemID(this.targetItemID);
        if (target == null) {
            Logger.printError("Target not found");
            return false;
        }
        // Players move in 16x16 steps, so check if they are exactly 2 tiles (16px) away

        return (Math.abs(this.worldX - target.getWorldX()) == 2 && this.worldY == target.getWorldY())
                || (Math.abs(this.worldY - target.getWorldY()) == 2 && this.worldX == target.getWorldX());
    }

    private void handlePlayerTakeMove(String uniqueItemID) {
        Item item = this.world.itemsManager.getItemByUniqueItemID(uniqueItemID);
        if (item == null) {
            this.world.chatMessages
                    .add(new ChatMessage(this.name, "Too late, it's gone!", System.currentTimeMillis(), false));
            return;
        }

        if (item.getWorldX() == this.worldX && item.getWorldY() == this.worldY) {
            this.takeItem(uniqueItemID);
            return;
        }

        boolean isItemOnUnreachableTile = this.world.tileManager.getCollisionByXandY(item.getWorldX(),
                item.getWorldY());

        if (isItemOneStepAwayFromTarget() && isItemOnUnreachableTile) {
            this.takeItem(uniqueItemID);
            return;
        }

        this.targetItemID = uniqueItemID;
        if (isItemOnUnreachableTile) {
            TilePosition closestTile = this.world.tileManager.getClosestWalkableTile(item.getWorldX(),
                    item.getWorldY());
            Logger.printError("Closest tile: " + closestTile.getX() + ", " + closestTile.getY());
            this.newTargetTile = new TilePosition(closestTile.getX(), closestTile.getY());
        } else {
            this.newTargetTile = new TilePosition(item.getWorldX(), item.getWorldY());
        }

    }

    public void takeItem(String uniqueItemID) {
        Item item = this.world.itemsManager.getItemByUniqueItemID(uniqueItemID);

        if (item == null) {
            Logger.printError("Item not found");
            this.world.chatMessages
                    .add(new ChatMessage(this.name, "Too late, it's gone!", System.currentTimeMillis(), false));
            return;
        }

        this.facingDirection = this.getDirectionTowardsTile(item.getWorldX(), item.getWorldY());

        if (item.getItemID() == 1 || item.getItemID() == 2 || item.getItemID() == 3) { // Starter pokemons
            if (this.storyProgress >= 3) {
                this.world.chatMessages
                        .add(new ChatMessage(this.name, "I shouldn't pick more than one.", System.currentTimeMillis(),
                                false));
                return;
            }

            if (this.storyProgress < 2) {
                this.world.chatMessages
                        .add(new ChatMessage(this.name, "I shouldn't touch things that aren't mine.",
                                System.currentTimeMillis(),
                                false));
                return;
            }

            int pokemonID = item.getItemID() == 1 ? 155 : item.getItemID() == 2 ? 158 : 152;
            this.pendingStarterPokemonID = pokemonID;
            Npc profElm = this.world.npcs.stream().filter(npc -> npc.npcIndex == 5).findFirst().get();

            if (profElm == null) {
                Logger.printError("Professor Elm not found");
                return;
            }

            TalkEvent talkEvent = new TalkEvent(this.entityID, profElm.entityID, 5, pokemonID);
            this.world.tickTalkEvents.add(talkEvent);
            this.targetItemID = null;

            return;
        }

        addItemToInventory(item.getItemID(), item.getAmount());
        this.world.itemsManager.removeItem(item.getUniqueID());

        SoundEvent soundEvent = new SoundEvent("pick_up.ogg", true, false, this.entityID);
        this.world.tickSoundEvents.add(soundEvent);

    }

    private int getEmptyInventorySlot() {
        for (int i = 0; i < this.inventory.length; i++) {
            if (this.inventory[i] == 0) {
                return i;
            }
        }

        return -1;
    }

    private void addItemToInventory(int itemID, int quantity) {
        Item item = this.world.itemsManager.getItemByID(itemID);
        if (item == null) {
            Logger.printError("Item not found");
            return;
        }

        if (item.isStackable() == true) {
            boolean isItemAlreadyInInventory = false;
            for (int i = 0; i < this.inventory.length; i++) {
                if (this.inventory[i] == itemID) {
                    if ((long) this.inventoryAmounts[i] + quantity > Integer.MAX_VALUE) {
                        Logger.printError("Quantity exceeds maximum limit for item stack.");
                        world.chatMessages.add(new ChatMessage(this.name,
                                "You already have a full stack of this item. The item is dropped on the ground.",
                                System.currentTimeMillis(), false));

                        world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY, itemID, 200, quantity);
                        return;
                    }
                    this.inventoryAmounts[i] += quantity;
                    isItemAlreadyInInventory = true;
                    break;
                }
            }

            if (isItemAlreadyInInventory == false) {
                int emptySlot = getEmptyInventorySlot();

                if (emptySlot == -1) {
                    Logger.printError("No empty inventory slots, dropping item");
                    world.chatMessages.add(new ChatMessage(this.name,
                            "You don't have enough space in your inventory. The item is dropped on the ground.",
                            System.currentTimeMillis(), false));
                    world.itemsManager.spawnItemWithAmount(this.worldX, this.worldY, itemID, 200, quantity);
                    return;
                }

                this.inventory[emptySlot] = itemID;
                this.inventoryAmounts[emptySlot] = quantity;
            }
        } else {
            if (!item.isStackable() && quantity > 1) {
                Logger.printError("Cannot add multiple non-stackable items.");
                return;
            }
            int emptySlot = getEmptyInventorySlot();

            if (emptySlot == -1) {
                world.chatMessages.add(new ChatMessage(this.name,
                        "You don't have enough space in your inventory. The item is dropped on the ground.",
                        System.currentTimeMillis(), false));
                world.itemsManager.spawnItem(this.worldX, this.worldY, itemID, 200);
                return;
            }

            this.inventory[emptySlot] = itemID;
        }

        saveInventory();
    }

    public void saveInventory() {
        Gson gson = new Gson();
        String inventoryString = gson.toJson(this.inventory);
        String inventoryAmountsString = gson.toJson(this.inventoryAmounts);
        try {
            CommonQueries.savePlayerInventoryByAccountId(this.accountID, inventoryString, inventoryAmountsString);
        } catch (SQLException e) {
            Logger.printError("Failed to save inventory");
        }
    }

    public void faint() {
        ChatMessage faintedMessage = new ChatMessage(this.name,
                "All of your Pokémon have fainted! You blacked out...", System.currentTimeMillis(), false);

        this.world.addChatMessage(faintedMessage);
        TilePosition lastPokecenterTile = getLastPokecenterWorldTilePosition();
        this.worldX = lastPokecenterTile.getX();
        this.worldY = lastPokecenterTile.getY();
        this.facingDirection = Direction.DOWN;
        this.healAllPokemon();
    }

    public TilePosition getLastPokecenterWorldTilePosition() {
        return switch (lastPokecenter) {
            case Pokecenter.NEW_BARK_TOWN -> new TilePosition(160, 321);
            case Pokecenter.CHERRYGROVE_CITY -> new TilePosition(80, 189);
            default -> new TilePosition(160, 321);
        };
    }

    public void healAllPokemon() {
        for (Pokemon party1 : this.party) {
            if (party1 != null) {
                party1.heal();
            }
        }
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
