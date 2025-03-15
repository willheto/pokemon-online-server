package com.g8e.gameserver;

import java.util.ArrayList;
import java.util.List;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import com.g8e.gameserver.battle.trainerBattle.BattleEvent;
import com.g8e.gameserver.battle.trainerBattle.BattleTurnEvent;
import com.g8e.gameserver.battle.wildBattle.WildBattleTurnEvent;
import com.g8e.gameserver.battle.wildBattle.WildPokemonEvent;
import com.g8e.gameserver.constants.NpcConstants;
import com.g8e.gameserver.enums.Direction;
import com.g8e.gameserver.managers.EntitiesManager;
import com.g8e.gameserver.managers.ItemsManager;
import com.g8e.gameserver.managers.PokemonMovesManager;
import com.g8e.gameserver.managers.PokemonsManager;
import com.g8e.gameserver.managers.ShopsManager;
import com.g8e.gameserver.managers.TeleportsManager;
import com.g8e.gameserver.managers.WildPokemonAreasManager;
import com.g8e.gameserver.models.ChatMessage;
import com.g8e.gameserver.models.entities.Entity;
import com.g8e.gameserver.models.entities.EntityData;
import com.g8e.gameserver.models.entities.Npc;
import com.g8e.gameserver.models.entities.Player;
import com.g8e.gameserver.models.events.SoundEvent;
import com.g8e.gameserver.models.events.TalkEvent;
import com.g8e.gameserver.models.objects.Item;
import com.g8e.gameserver.models.pokemon.Pokemon;
import com.g8e.gameserver.network.GameState;
import com.g8e.gameserver.network.GameStateComparator;
import com.g8e.gameserver.network.WebSocketEventsHandler;
import com.g8e.gameserver.network.actions.Action;
import com.g8e.gameserver.network.compressing.Compress;
import com.g8e.gameserver.network.dataTransferModels.DTONpc;
import com.g8e.gameserver.network.dataTransferModels.DTOPlayer;
import com.g8e.gameserver.pathfinding.AStar;
import com.g8e.gameserver.tile.TileManager;
import com.g8e.util.Logger;
import com.google.gson.Gson;

public class World {

    Gson gson = new Gson(); // Moved outside the loop

    private static final int TICK_RATE = 200;
    public final int maxWorldCol = 400; // Width of the world in tiles
    public final int maxWorldRow = 400; // Height of the world in tiles
    public final int maxPlayers = 10; // TODO: Sync with login server

    public AStar pathFinder;

    public WebSocketEventsHandler webSocketEventsHandler;
    public TileManager tileManager = new TileManager(this);
    public ItemsManager itemsManager = new ItemsManager(this);
    public ShopsManager shopsManager = new ShopsManager();
    public EntitiesManager entitiesManager = new EntitiesManager();
    public PokemonMovesManager pokemonMovesManager = new PokemonMovesManager();
    public TeleportsManager teleportsManager = new TeleportsManager();
    public WildPokemonAreasManager wildPokemonAreasManager = new WildPokemonAreasManager(this);
    public PokemonsManager pokemonsManager = new PokemonsManager();

    public List<Player> players = new ArrayList<>();
    public List<Npc> npcs = new ArrayList<>();

    public List<Item> items = new ArrayList<>();
    public List<ChatMessage> chatMessages = new ArrayList<>();
    public List<Action> actionQueue = new ArrayList<>();
    public List<TalkEvent> tickTalkEvents = new ArrayList<>();
    public List<SoundEvent> tickSoundEvents = new ArrayList<>();
    public List<BattleEvent> tickBattleEvents = new ArrayList<>();
    public List<BattleTurnEvent> tickBattleTurnEvents = new ArrayList<>();
    public List<WildPokemonEvent> tickWildPokemonEvents = new ArrayList<>();
    public List<WildBattleTurnEvent> tickWildBattleTurnEvents = new ArrayList<>();

    public GameState previousGameState;
    public WebSocket[] connections = new WebSocket[maxPlayers];
    public List<String> onlinePlayers = new ArrayList<>();

    public World() {
        this.pathFinder = new AStar(this.tileManager);

        this.setInitialNpcs();
        this.setInitialItems();
    }

    public WebSocket[] getConnections() {
        return connections;
    }

    public List<String> getOnlinePlayers() {
        return onlinePlayers;
    }

    public void addConnection(WebSocket conn) {
        for (int i = 0; i < maxPlayers; i++) {
            if (connections[i] == null) {
                connections[i] = conn;
                onlinePlayers.add(conn.toString());
                break;
            }
        }
    }

    public void removeConnection(WebSocket conn) {
        for (int i = 0; i < maxPlayers; i++) {
            if (connections[i] == conn) {
                connections[i] = null;
                onlinePlayers.remove(conn.toString());
                break;
            }
        }
    }

    public void start() {

        while (true) {
            try {
                Thread.sleep(TICK_RATE);
                gameTick();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void addChatMessage(ChatMessage chatMessage) {
        this.chatMessages.add(chatMessage);
    }

    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }

    private void gameTick() {
        try {
            this.players.forEach(player -> {
                List<Action> playerActions = this.actionQueue.stream()
                        .filter(action -> action.getPlayerID().equals(player.entityID))
                        .toList();

                player.setTickActions(playerActions);
                player.update();
            });

            // Remove actions after processing all players to avoid concurrent modification
            // exception
            List<Action> actionsToRemove = this.players.stream()
                    .flatMap(player -> this.actionQueue.stream()
                            .filter(action -> action.getPlayerID().equals(player.entityID)))
                    .toList();
            this.actionQueue.removeAll(actionsToRemove);
            this.npcs.forEach(npc -> npc.update());
            itemsManager.updateDespawnTimers();
            sentGameStateToConnections();
        } catch (Exception e) {
            Logger.printError("Error in gameTick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sentGameStateToConnections() {
        GameState newGameState = getChangedGameState();

        for (WebSocket conn : connections) {
            if (conn != null) {
                Player player = this.players.stream().filter(p -> p.entityID.equals(conn.toString())).findFirst()
                        .orElse(null);

                if (player != null) {
                    List<DTONpc> npcs = this.npcs.stream().map(npc -> new DTONpc(npc)).toList();
                    List<DTOPlayer> players = this.players.stream().map(p -> new DTOPlayer(p)).toList();
                    List<TalkEvent> talkEvents = this.tickTalkEvents.stream()
                            .filter(t -> t.talkerID.equals(player.entityID) || t.targetID.equals(player.entityID))
                            .toList();
                    List<BattleEvent> battleEvents = this.tickBattleEvents.stream()
                            .filter(b -> b.entity1ID.equals(player.entityID) || b.entity2ID.equals(player.entityID))
                            .toList();
                    List<WildPokemonEvent> wildPokemonEvents = this.tickWildPokemonEvents.stream()
                            .filter(w -> w.entityID.equals(player.entityID)).toList();
                    List<WildBattleTurnEvent> wildBattleTurnEvents = this.tickWildBattleTurnEvents.stream()
                            .filter(w -> w.playerID.equals(player.entityID)).toList();
                    List<BattleTurnEvent> battleTurnEvents = this.tickBattleTurnEvents.stream()
                            .filter(b -> b.playerID.equals(player.entityID) || b.opponentID.equals(player.entityID))
                            .toList();

                    newGameState.setNpcs(npcs);
                    newGameState.setPlayers(players);
                    newGameState.setTickTalkEvents(talkEvents);
                    newGameState.setTickBattleEvents(battleEvents);
                    newGameState.setTickWildPokemonEvents(wildPokemonEvents);
                    newGameState.setTickWildBattleTurnEvents(wildBattleTurnEvents);
                    newGameState.setTickBattleTurnEvents(battleTurnEvents);

                    // add current player if not in the list
                    if (players.stream().noneMatch(p -> p.getEntityID().equals(player.entityID))) {
                        players.add(new DTOPlayer(player));
                    }

                    String gameStateJson = gson.toJson(newGameState);
                    byte[] compressedData = Compress.compress(gameStateJson);

                    try {
                        conn.send(compressedData);
                    } catch (WebsocketNotConnectedException e) {
                        Logger.printInfo("Connection " + conn
                                + " is not connected, probably in combat and waiting to be logged out");
                    }

                }
            }
        }

        this.tickTalkEvents.clear();
        this.tickSoundEvents.clear();
        this.tickBattleEvents.clear();
        this.tickBattleTurnEvents.clear();
        this.tickWildPokemonEvents.clear();
        this.tickWildBattleTurnEvents.clear();

    }

    private GameState getChangedGameState() {
        List<DTOPlayer> dtoPlayers = this.players.stream().map(player -> new DTOPlayer(player)).toList();
        List<DTONpc> dtoNpcs = this.npcs.stream().map(npc -> new DTONpc(npc)).toList();

        GameState newGameState = new GameState(this.tickTalkEvents,
                this.tickSoundEvents,
                this.tickBattleEvents,
                this.tickBattleTurnEvents,
                this.tickWildPokemonEvents,
                this.tickWildBattleTurnEvents,
                dtoPlayers, dtoNpcs,
                this.chatMessages,
                this.items, null, this.onlinePlayers, this.shopsManager.getShops());
        if (previousGameState == null) {
            previousGameState = newGameState;
            return newGameState;
        } else {
            GameState changedGameState = GameStateComparator.getChangedGameState(previousGameState, newGameState);
            previousGameState = newGameState;
            return changedGameState;
        }

    }

    public List<Item> getItems() {
        return items;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public List<Npc> getNpcs() {
        return npcs;
    }

    public void enqueueAction(Action action) {
        this.actionQueue.add(action);
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public Entity getEntityByID(String entityID) {
        for (Player player : players) {
            if (entityID.equals(player.entityID)) {
                return player;
            }
        }

        for (Npc npc : npcs) {
            if (entityID.equals(npc.entityID)) {
                return npc;
            }
        }

        return null;
    }

    public Item getItemByID(String itemUniqueID) {
        Item item = null;
        for (Item i : items) {
            if (i != null && i.getUniqueID().equals(itemUniqueID)) {
                item = i;
                break;
            }
        }
        return item;
    }

    public void removePlayer(WebSocket conn) {
        String playerID = conn.toString();
        Player player = this.players.stream().filter(p -> p.entityID.equals(playerID)).findFirst().orElse(null);
        if (player != null) {
            this.players.remove(player);
            removeConnection(conn);
        }
    }

    private void setInitialNpcs() {
        addNpc(NpcConstants.RIVAL, 228, 185, 0, Direction.RIGHT);
        addNpc(NpcConstants.MOM, 128, 321, 0, Direction.LEFT);
        addNpc(NpcConstants.WOMAN, 234, 197, 0, Direction.LEFT);
        addNpc(NpcConstants.MAN, 243, 197, 10, Direction.DOWN);
        addNpc(NpcConstants.ELM, 200, 319, 0, Direction.DOWN);
        addNpc(NpcConstants.BOY1, 204, 205, 0, Direction.UP);
        addNpc(NpcConstants.BOY2, 156, 211, 5, Direction.LEFT);
        addNpc(NpcConstants.MAN2, 152, 189, 5, Direction.DOWN);
        addNpc(NpcConstants.OFFICER1, 310, 322, 0, Direction.RIGHT);
        addNpc(NpcConstants.BOY3, 321, 321, 5, Direction.UP);
        addNpc(NpcConstants.CHERRYGROVE_POKECENTER_LADY, 236, 367, 0, Direction.DOWN);
        addNpc(NpcConstants.CHERRYGROVE_POKECENTER_GUY, 234, 367, 0, Direction.DOWN);
        addNpc(NpcConstants.CHERRYGROVE_POKECENTER_WOMAN, 232, 373, 0, Direction.RIGHT);
        addNpc(NpcConstants.CHERRYGROVE_POKECENTER_GENTLEMAN, 246, 373, 0, Direction.LEFT);
        addNpc(NpcConstants.CHERRYGROVE_BOY1, 68, 195, 5, Direction.LEFT);
        addNpc(NpcConstants.CHERRYGROVE_OLD_MAN, 86, 193, 0, Direction.DOWN);
        addNpc(NpcConstants.CHERRYGROVE_WOMAN1, 75, 205, 5, Direction.DOWN);
        addNpc(NpcConstants.CHERRYGROVE_BOY2, 194, 371, 0, Direction.RIGHT);
        addNpc(NpcConstants.CHERRYGROVE_GIRL, 196, 371, 0, Direction.LEFT);

        Npc will = addNpc(NpcConstants.ELITE_FOUR_WILL, 243, 197, 0, Direction.DOWN);
        Pokemon xatu40 = new Pokemon(178, 40);
        Pokemon jynx = new Pokemon(124, 41);
        Pokemon exeggutor = new Pokemon(103, 42);
        Pokemon slowbro = new Pokemon(80, 43);
        Pokemon xatu42 = new Pokemon(178, 42);
        will.addPokemonToParty(xatu40);
        will.addPokemonToParty(jynx);
        will.addPokemonToParty(exeggutor);
        will.addPokemonToParty(slowbro);
        will.addPokemonToParty(xatu42);

    }

    private Npc addNpc(int index, int x, int y, int wanderRange, Direction facingDirection) {
        EntityData entityData = entitiesManager.getEntityDataByIndex(index);
        Npc npc = new Npc(this, index, x, y, entityData.getName());
        this.npcs.add(npc);
        npc.setWanderRange(wanderRange);
        npc.setFacingDirection(facingDirection);
        return npc;
    }

    private void setInitialItems() {
        this.itemsManager.spawnItem(202, 321, 1);
        this.itemsManager.spawnItem(204, 321, 2);
        this.itemsManager.spawnItem(206, 321, 3);
    }
}
