package com.g8e.gameserver.models.pokemon;

import com.g8e.gameserver.tile.TilePosition;

public class WildPokemonArea {

    final private String name;
    final private TilePosition topLeftTile;
    final private TilePosition bottomRightTile;
    final private WildPokemonRate[] pokemon;

    public WildPokemonArea(String name, TilePosition topLeftTile, TilePosition bottomRightTile,
            WildPokemonRate[] pokemon) {
        this.name = name;
        this.topLeftTile = topLeftTile;
        this.bottomRightTile = bottomRightTile;
        this.pokemon = pokemon;
    }

    public String getName() {
        return name;
    }

    public TilePosition getTopLeftTile() {
        return topLeftTile;
    }

    public TilePosition getBottomRightTile() {
        return bottomRightTile;
    }

    public WildPokemonRate[] getPokemon() {
        return pokemon;
    }

    public WildPokemonRate rollEncounter() {
        int random = (int) (Math.random() * 100);
        int totalRate = 0;

        for (WildPokemonRate rate : pokemon) {
            totalRate += rate.getRate();
            if (random < totalRate) {
                return rate;

            }
        }

        return null;
    }

}
