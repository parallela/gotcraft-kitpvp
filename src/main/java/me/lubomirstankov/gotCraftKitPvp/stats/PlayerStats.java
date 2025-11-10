package me.lubomirstankov.gotCraftKitPvp.stats;

import java.util.UUID;

public class PlayerStats {

    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int currentStreak;
    private int bestStreak;
    private int level;
    private int xp;
    private String lastKit;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.kills = 0;
        this.deaths = 0;
        this.currentStreak = 0;
        this.bestStreak = 0;
        this.level = 1;
        this.xp = 0;
        this.lastKit = null;
    }

    public PlayerStats(UUID uuid, String name, int kills, int deaths, int currentStreak,
                       int bestStreak, int level, int xp, String lastKit) {
        this.uuid = uuid;
        this.name = name;
        this.kills = kills;
        this.deaths = deaths;
        this.currentStreak = currentStreak;
        this.bestStreak = bestStreak;
        this.level = level;
        this.xp = xp;
        this.lastKit = lastKit;
    }

    public void addKill() {
        kills++;
        currentStreak++;
        if (currentStreak > bestStreak) {
            bestStreak = currentStreak;
        }
    }

    public void addDeath() {
        deaths++;
        currentStreak = 0;
    }

    public void addXP(int amount) {
        xp += amount;
    }

    public void removeXP(int amount) {
        xp = Math.max(0, xp - amount);
    }

    public void levelUp() {
        level++;
        xp = 0;
    }

    public double getKDR() {
        if (deaths == 0) {
            return kills;
        }
        return (double) kills / deaths;
    }

    public String getFormattedKDR() {
        return String.format("%.2f", getKDR());
    }

    // Getters and setters
    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void setBestStreak(int bestStreak) {
        this.bestStreak = bestStreak;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public String getLastKit() {
        return lastKit;
    }

    public void setLastKit(String lastKit) {
        this.lastKit = lastKit;
    }
}

