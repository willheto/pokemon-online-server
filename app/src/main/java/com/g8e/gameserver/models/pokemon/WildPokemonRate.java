package com.g8e.gameserver.models.pokemon;

public class WildPokemonRate {

    final private String name;
    final private int rate;
    final private int min;
    final private int max;

    public WildPokemonRate(String name, int rate, int min, int max) {
        this.name = name;
        this.rate = rate;
        this.min = min;
        this.max = max;
    }

    public String getName() {
        return name;
    }

    public int getRate() {
        return rate;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

}
