package com.g8e.gameserver.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.g8e.gameserver.World;
import com.g8e.gameserver.models.entities.Player;
import com.g8e.gameserver.models.pokemon.Pokemon;
import com.g8e.gameserver.models.pokemon.WildPokemonArea;
import com.g8e.gameserver.models.pokemon.WildPokemonRate;
import com.google.gson.Gson;

public class WildPokemonAreasManager {
    private World world;
    final private WildPokemonArea[] pokemonData = new WildPokemonArea[5];

    public WildPokemonAreasManager(World world) {
        this.world = world;
        loadPokemonData();
    }

    private void loadPokemonData() {
        URL pokemonDataUrl = getClass().getResource("/data/scripts/wildPokemonAreas.json");

        if (pokemonDataUrl == null) {
            throw new IllegalArgumentException("Resource not found: /data/wildPokemonAreas.json");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pokemonDataUrl.openStream()))) {
            Gson gson = new Gson();
            WildPokemonArea[] loadedPokemonDatas = gson.fromJson(reader, WildPokemonArea[].class);
            System.arraycopy(loadedPokemonDatas, 0, pokemonData, 0, loadedPokemonDatas.length);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Pokemon getRandomEncounterByEntityID(String entityID) {
        Player player = this.world.players.stream().filter(p -> p.entityID.equals(entityID)).findFirst().orElse(null);
        if (player == null) {
            return null;
        }

        int playerX = player.worldX;
        int playerY = player.worldY;

        for (WildPokemonArea area : pokemonData) {
            if (playerX >= area.getTopLeftTile().getX() && playerX <= area.getBottomRightTile().getX()
                    && playerY >= area.getTopLeftTile().getY() && playerY <= area.getBottomRightTile().getY()) {
                WildPokemonRate rate = area.rollEncounter();
                int encounterId = this.world.pokemonsManager.getIdByName(rate.getName());
                int randomLevel = (int) (Math.random() * (rate.getMax() - rate.getMin())) + rate.getMin();
                return new Pokemon(encounterId, randomLevel);

            }
        }

        return null;

    }

}
