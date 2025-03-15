package com.g8e.gameserver.battle.wildBattle;

import com.g8e.gameserver.World;
import com.g8e.gameserver.models.pokemon.Pokemon;
import com.g8e.gameserver.network.dataTransferModels.DTOPokemon;

public class WildPokemonEvent {

    final transient private World world;
    public String entityID;
    public DTOPokemon wildPokemon;

    public WildPokemonEvent(World world, String entityID) {
        this.world = world;
        this.entityID = entityID;
    }

    public Pokemon rollRandomPokemon() {
        Pokemon encounter = this.world.wildPokemonAreasManager.getRandomEncounterByEntityID(entityID);
        this.wildPokemon = new DTOPokemon(
                encounter.getId(),
                encounter.getXp(),
                encounter.getHp(),
                encounter.getHp(),
                encounter.getMoves());

        return encounter;
    }
}
