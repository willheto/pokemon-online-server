package com.g8e.gameserver.models.pokemon;

public class ExperienceCurve {

    /**
     * Calculates the total experience required to reach a given level.
     *
     * @param level The target level (must be >= 1)
     * @return The total experience required
     */
    public static int getExperienceForLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        return level * level * level; // EXP = n^3
    }

    /**
     * Calculates the level corresponding to a given experience amount.
     *
     * @param experience The total experience
     * @return The corresponding level
     */
    public static int getLevelByExperience(int experience) {
        if (experience < 0) {
            throw new IllegalArgumentException("Experience cannot be negative");
        }
        return (int) Math.cbrt(experience); // Level = cubic root of EXP
    }

    public static int getExperienceByLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        return level * level * level; // EXP = n^3
    }

}
