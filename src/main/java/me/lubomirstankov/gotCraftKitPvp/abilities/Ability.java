package me.lubomirstankov.gotCraftKitPvp.abilities;

import java.util.List;

public class Ability {

    private final String id;
    private String name;
    private String description;
    private int cooldown;
    private String permission;
    private AbilityType type;
    private double power;
    private double amount;
    private double radius;
    private int duration;
    private double range;
    private double damage;
    private int amplifier;

    public Ability(String id) {
        this.id = id;
    }

    public enum AbilityType {
        DASH,
        HEAL,
        FIREBALL,
        FREEZE,
        LIGHTNING,
        TELEPORT,
        INVISIBILITY,
        STRENGTH,
        CUSTOM
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public AbilityType getType() {
        return type;
    }

    public void setType(AbilityType type) {
        this.type = type;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public void setAmplifier(int amplifier) {
        this.amplifier = amplifier;
    }
}

