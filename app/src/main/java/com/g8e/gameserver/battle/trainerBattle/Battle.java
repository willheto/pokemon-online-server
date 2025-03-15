package com.g8e.gameserver.battle.trainerBattle;

import java.util.ArrayList;
import java.util.List;

import com.g8e.gameserver.battle.enums.BattleOption;
import com.g8e.gameserver.models.entities.Entity;
import com.g8e.gameserver.models.entities.Npc;
import com.g8e.gameserver.models.entities.Player;
import com.g8e.gameserver.models.pokemon.Pokemon;
import com.g8e.gameserver.models.pokemon.PokemonMove;
import com.g8e.util.Logger;

public class Battle {
    private final Player entity1;
    private final Entity entity2;
    private Pokemon entity1ActivePokemon;
    private Pokemon entity2ActivePokemon;

    private PokemonMove entity1PendingMove;
    private PokemonMove entity2PendingMove;

    private BattleOption entity1PendingAction;
    private BattleOption entity2PendingAction;

    private Integer entity1PendingItemId;
    private Integer entity2PendingItemId;

    private Integer entity1PendingPokemonIndex;
    private Integer entity2PendingPokemonIndex;

    final private List<Pokemon> entity1Participants = new ArrayList<>();
    final private List<Pokemon> entity2Participants = new ArrayList<>();

    private boolean entity1PokemonJustFainted = false;
    private boolean entity2PokemonJustFainted = false;

    public Battle(Player entity1, Entity entity2) {
        this.entity1 = entity1;
        this.entity2 = entity2;

        // Initialize active Pokémon for each player
        this.entity1ActivePokemon = getFirstHealthyPokemon(entity1);
        this.entity2ActivePokemon = getFirstHealthyPokemon(entity2);

        entity1Participants.add(entity1ActivePokemon);
        entity2Participants.add(entity2ActivePokemon);
    }

    private Pokemon getFirstHealthyPokemon(Entity player) {
        for (Pokemon pokemon : player.party) {
            if (pokemon != null && pokemon.getHp() > 0) {
                return pokemon;
            }
        }
        return null; // No healthy Pokémon found
    }

    public void setEntity1PendingAction(BattleOption action, Integer value) {
        this.entity1PendingAction = action;

        switch (action) {
            case FIGHT -> {
                if (value != null) {
                    this.entity1PendingMove = entity1.world.pokemonMovesManager.getPokemonMoveById(value);
                }
            }
            case ITEM -> this.entity1PendingItemId = entity1.inventory[value];
            case POKEMON -> this.entity1PendingPokemonIndex = value;
            case RUN -> Logger.printDebug("Attempt to run from battle, not implemented");
        }

        // if entity 2 is typeof npc, then npc will automatically choose random move
        if (entity2 instanceof Npc) {
            int randomMoveId = entity2ActivePokemon.getRandomMove();
            this.setEntity2PendingAction(BattleOption.FIGHT, randomMoveId);
        }
        if (entity2PendingAction != null || entity1PokemonJustFainted) {
            executeTurn();
        }
    }

    public void setEntity2PendingAction(BattleOption action, Integer value) {
        this.entity2PendingAction = action;

        switch (action) {
            case FIGHT -> {
                if (value != null) {
                    this.entity2PendingMove = entity1.world.pokemonMovesManager.getPokemonMoveById(value);
                }
            }
            case ITEM -> this.entity2PendingItemId = entity2.inventory[value];
            case POKEMON -> this.entity2PendingPokemonIndex = value;
            case RUN -> Logger.printDebug("Attempt to run from battle, not implemented");
        }

        if (entity1PendingAction != null || entity2PokemonJustFainted) {
            executeTurn();
        }
    }

    private void executeTurn() {
        if (entity1ActivePokemon == null || entity2ActivePokemon == null) {
            return;
        }

        this.entity1PokemonJustFainted = false;
        this.entity2PokemonJustFainted = false;

        // Handle actions for both players
        if (entity1PendingAction == BattleOption.ITEM) {
            useItem(true); // Player 1 uses item
        }
        if (entity2PendingAction == BattleOption.ITEM) {
            useItem(false); // Player 2 uses item
        }
        if (entity1PendingAction == BattleOption.POKEMON) {
            switchPokemon(true); // Player 1 switches Pokémon
        }
        if (entity2PendingAction == BattleOption.POKEMON) {
            switchPokemon(false); // Player 2 switches Pokémon
        }
        // Handle fighting turns
        executeFightTurn();

        resetPendingActions();
    }

    private void executeFightTurn() {
        // Determine which player goes first based on speed
        boolean entity1GoesFirst = entity1ActivePokemon.getSpeed() > entity2ActivePokemon.getSpeed();

        Pokemon firstPokemon = entity1GoesFirst ? entity1ActivePokemon : entity2ActivePokemon;
        Pokemon secondPokemon = entity1GoesFirst ? entity2ActivePokemon : entity1ActivePokemon;

        PokemonMove firstMove = entity1GoesFirst ? entity1PendingMove : entity2PendingMove;
        PokemonMove secondMove = entity1GoesFirst ? entity2PendingMove : entity1PendingMove;

        // First Pokémon moves (if it has a move)
        if (firstMove != null) {
            processMove(entity1GoesFirst, firstMove, firstPokemon, secondPokemon);
        }

        // Check if the second Pokémon is still alive & has a move before executing its
        // turn
        if (secondPokemon.getHp() > 0 && secondMove != null) {
            if (entity1PokemonJustFainted || entity2PokemonJustFainted) {
                return;
            }

            processMove(!entity1GoesFirst, secondMove, secondPokemon, firstPokemon);
        }
    }

    private void processMove(boolean isEntity1Move, PokemonMove move, Pokemon attackerPokemon, Pokemon targetPokemon) {
        // Generate battle event
        BattleTurnEvent event = new BattleTurnEvent(
                isEntity1Move ? entity1.entityID : entity2.entityID,
                isEntity1Move ? entity2.entityID : entity1.entityID,
                move.getId());

        int effect = attackerPokemon.useMove(move.getId(), targetPokemon);

        // Determine the effect of the move
        switch (effect) {
            case 0 -> event.setEffect(0);
            case 1 -> event.setEffect(1);
            case -1 -> event.setEffect(-1);
            default -> {
            }
        }

        // Update the target Pokémon's HP
        event.setHp(targetPokemon.getHp());

        // Check if target Pokémon fainted
        if (targetPokemon.getHp() == 0) {
            event.setBattleOver(isEntity1Move);

            boolean opponentHasHealthyPokemon = hasHealthyPokemon(isEntity1Move ? entity2 : entity1);
            boolean attackerHasHealthyPokemon = hasHealthyPokemon(isEntity1Move ? entity1 : entity2);

            // The Pokémon that just fainted belongs to the opponent of the attacking entity

            // If the opponent has no healthy Pokémon left, they lose the battle
            // If the opponent has no healthy Pokémon left, they lose the battle
            if (!opponentHasHealthyPokemon) {
                event.isAllPokemonsFainted = true;
                entity1.world.tickBattleTurnEvents.add(event);

                if ((isEntity1Move ? entity2 : entity1) instanceof Player) {
                    ((Player) (isEntity1Move ? entity2 : entity1)).faint();
                }

                entity1.battle = null;
                entity2.battle = null;
                return;
            }

            // If the attacker has no healthy Pokémon left, they lose the battle
            if (!attackerHasHealthyPokemon) {
                event.isAllPokemonsFainted = true;
                entity1.world.tickBattleTurnEvents.add(event);

                if ((isEntity1Move ? entity2 : entity1) instanceof Player) {
                    ((Player) (isEntity1Move ? entity2 : entity1)).faint();
                }

                entity1.battle = null;
                entity2.battle = null;
                return;
            }

            // Track which side's Pokémon just fainted
            this.entity1PokemonJustFainted = !isEntity1Move;
            this.entity2PokemonJustFainted = isEntity1Move;

            // If battle is still ongoing, distribute XP to the winning side
            List<Pokemon> winningParticipants = isEntity1Move ? entity1Participants : entity2Participants;
            for (Pokemon participant : winningParticipants) {
                participant.addXp(targetPokemon, false, winningParticipants.size());
            }
            // if entity 2 is npc, then npc will automatically switch next pokemon
            if (entity2 instanceof Npc) {
                this.entity2ActivePokemon = getFirstHealthyPokemon(entity2);
            }

        }

        // Add event to world event list
        entity1.world.tickBattleTurnEvents.add(event);
    }

    private boolean hasHealthyPokemon(Entity player) {
        for (Pokemon pokemon : player.party) {
            if (pokemon != null && pokemon.getHp() > 0) {
                return true;
            }
        }
        return false;
    }

    private void useItem(boolean isEntity1) {
        // Item usage event
        Integer pendingItemId = isEntity1 ? entity1PendingItemId : entity2PendingItemId;
        BattleTurnEvent event = new BattleTurnEvent(
                isEntity1 ? entity1.entityID : entity2.entityID,
                isEntity1 ? entity2.entityID : entity1.entityID,
                pendingItemId,
                BattleOption.ITEM);

        // Process item usage here...
        entity1.world.tickBattleTurnEvents.add(event);
    }

    private void switchPokemon(boolean isEntity1) {
        // Pokémon switch event
        Integer pendingPokemonIndex = isEntity1 ? entity1PendingPokemonIndex : entity2PendingPokemonIndex;
        BattleTurnEvent event = new BattleTurnEvent(
                isEntity1 ? entity1.entityID : entity2.entityID,
                isEntity1 ? entity2.entityID : entity1.entityID,
                pendingPokemonIndex,
                BattleOption.POKEMON);

        Pokemon newActivePokemon = isEntity1 ? entity1.party[pendingPokemonIndex] : entity2.party[pendingPokemonIndex];
        if (isEntity1) {
            entity1ActivePokemon = newActivePokemon;
        } else {
            entity2ActivePokemon = newActivePokemon;
        }

        entity1.world.tickBattleTurnEvents.add(event);

    }

    private void resetPendingActions() {
        entity1PendingMove = null;
        entity2PendingMove = null;
        entity1PendingAction = null;
        entity2PendingAction = null;
        entity1PendingItemId = null;
        entity2PendingItemId = null;
        entity1PendingPokemonIndex = null;
        entity2PendingPokemonIndex = null;
    }

    public Entity getEntity1() {
        return entity1; // Returns Player 1
    }

    public Entity getEntity2() {
        return entity2; // Returns Player 2
    }

    public void setEntityActivePokemon(Pokemon pokemon, boolean isEntity1) {
        if (isEntity1) {
            entity1ActivePokemon = pokemon;
        } else {
            entity2ActivePokemon = pokemon;
        }
    }
}
