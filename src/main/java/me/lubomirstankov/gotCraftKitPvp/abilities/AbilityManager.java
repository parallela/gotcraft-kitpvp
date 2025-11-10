package me.lubomirstankov.gotCraftKitPvp.abilities;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityManager {

    private final GotCraftKitPvp plugin;
    private final Map<String, Ability> abilities = new HashMap<>();
    private final Map<UUID, Map<String, Long>> abilityCooldowns = new ConcurrentHashMap<>();

    public AbilityManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        loadAbilities();
    }

    public void loadAbilities() {
        abilities.clear();

        ConfigurationSection abilitiesSection = plugin.getConfigManager().getAbilitiesConfig().getConfigurationSection("abilities");
        if (abilitiesSection != null) {
            for (String abilityId : abilitiesSection.getKeys(false)) {
                ConfigurationSection abilitySection = abilitiesSection.getConfigurationSection(abilityId);

                Ability ability = new Ability(abilityId);
                ability.setName(abilitySection.getString("name", abilityId));
                ability.setDescription(abilitySection.getString("description", ""));
                ability.setCooldown(abilitySection.getInt("cooldown", 10));
                ability.setPermission(abilitySection.getString("permission", ""));
                ability.setPower(abilitySection.getDouble("power", 1.0));
                ability.setAmount(abilitySection.getDouble("amount", 1.0));
                ability.setRadius(abilitySection.getDouble("radius", 5.0));
                ability.setDuration(abilitySection.getInt("duration", 5));
                ability.setRange(abilitySection.getDouble("range", 10.0));
                ability.setDamage(abilitySection.getDouble("damage", 5.0));
                ability.setAmplifier(abilitySection.getInt("amplifier", 1));

                // Determine type from ID
                try {
                    ability.setType(Ability.AbilityType.valueOf(abilityId.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    ability.setType(Ability.AbilityType.CUSTOM);
                }

                abilities.put(abilityId, ability);
            }
        }

        plugin.getLogger().info("Loaded " + abilities.size() + " abilities!");
    }

    public void reload() {
        loadAbilities();
    }

    public Ability getAbility(String id) {
        return abilities.get(id);
    }

    public boolean useAbility(Player player, String abilityId) {
        if (!plugin.getConfigManager().isAbilitiesEnabled()) {
            return false;
        }

        Ability ability = abilities.get(abilityId);
        if (ability == null) {
            return false;
        }

        // Check permission
        if (!ability.getPermission().isEmpty() && !player.hasPermission(ability.getPermission())) {
            return false;
        }

        // Check cooldown
        if (hasCooldown(player, abilityId)) {
            long remaining = getCooldownRemaining(player, abilityId);
            Map<String, String> placeholders = Map.of("%time%", formatTime(remaining));
            plugin.getMessageManager().sendMessage(player, "ability-cooldown", placeholders);
            return false;
        }

        // Execute ability
        boolean success = executeAbility(player, ability);

        if (success) {
            // Set cooldown
            setCooldown(player, abilityId, ability.getCooldown());

            // Send message
            Map<String, String> placeholders = Map.of("%ability%", ability.getName());
            plugin.getMessageManager().sendMessage(player, "ability-activated", placeholders);
        }

        return success;
    }

    private boolean executeAbility(Player player, Ability ability) {
        switch (ability.getType()) {
            case DASH:
                return executeDash(player, ability);
            case HEAL:
                return executeHeal(player, ability);
            case FIREBALL:
                return executeFireball(player, ability);
            case FREEZE:
                return executeFreeze(player, ability);
            case LIGHTNING:
                return executeLightning(player, ability);
            case TELEPORT:
                return executeTeleport(player, ability);
            case INVISIBILITY:
                return executeInvisibility(player, ability);
            case STRENGTH:
                return executeStrength(player, ability);
            default:
                return false;
        }
    }

    private boolean executeDash(Player player, Ability ability) {
        Vector direction = player.getLocation().getDirection().normalize();
        direction.multiply(ability.getPower());
        direction.setY(0.5);
        player.setVelocity(direction);

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);

        return true;
    }

    private boolean executeHeal(Player player, Ability ability) {
        double currentHealth = player.getHealth();
        double maxHealth = player.getMaxHealth();
        double newHealth = Math.min(maxHealth, currentHealth + ability.getAmount());

        player.setHealth(newHealth);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);

        return true;
    }

    private boolean executeFireball(Player player, Ability ability) {
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setYield(1.0f);
        fireball.setShooter(player);

        player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);

        return true;
    }

    private boolean executeFreeze(Player player, Ability ability) {
        Location loc = player.getLocation();

        for (Entity entity : player.getWorld().getNearbyEntities(loc, ability.getRadius(), ability.getRadius(), ability.getRadius())) {
            if (entity instanceof Player target && !target.equals(player)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ability.getDuration() * 20, 4));
                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ability.getDuration() * 20, 2));
                target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, ability.getDuration() * 20, 200)); // Negative jump = can't jump

                target.playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
                target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0);
            }
        }

        player.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.add(0, 1, 0), 100, ability.getRadius(), 1, ability.getRadius(), 0);

        return true;
    }

    private boolean executeLightning(Player player, Ability ability) {
        Entity target = getTargetEntity(player, ability.getRange());

        if (target instanceof LivingEntity livingTarget) {
            target.getWorld().strikeLightningEffect(target.getLocation());
            livingTarget.damage(ability.getDamage(), player);

            player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

            return true;
        }

        plugin.getMessageManager().sendMessage(player, "ability-no-target");
        return false;
    }

    private boolean executeTeleport(Player player, Ability ability) {
        Entity target = getTargetEntity(player, ability.getRange());

        if (target != null) {
            Location targetLoc = target.getLocation();
            Vector direction = targetLoc.getDirection().multiply(-2);
            Location teleportLoc = targetLoc.add(direction);
            teleportLoc.setDirection(targetLoc.getDirection().multiply(-1));

            player.teleport(teleportLoc);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 50, 0.5, 1, 0.5, 0.1);

            return true;
        }

        plugin.getMessageManager().sendMessage(player, "ability-no-target");
        return false;
    }

    private boolean executeInvisibility(Player player, Ability ability) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, ability.getDuration() * 20, 0));

        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.05);

        return true;
    }

    private boolean executeStrength(Player player, Ability ability) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ability.getDuration() * 20, ability.getAmplifier()));

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 1, 0.5, 0);

        return true;
    }

    private Entity getTargetEntity(Player player, double range) {
        return player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                range,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        ) != null ? player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                range,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        ).getHitEntity() : null;
    }

    public void setCooldown(Player player, String abilityId, int seconds) {
        Map<String, Long> cooldowns = abilityCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
        cooldowns.put(abilityId, System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean hasCooldown(Player player, String abilityId) {
        Map<String, Long> cooldowns = abilityCooldowns.get(player.getUniqueId());
        if (cooldowns == null) {
            return false;
        }

        Long cooldownEnd = cooldowns.get(abilityId);
        if (cooldownEnd == null) {
            return false;
        }

        return System.currentTimeMillis() < cooldownEnd;
    }

    public long getCooldownRemaining(Player player, String abilityId) {
        Map<String, Long> cooldowns = abilityCooldowns.get(player.getUniqueId());
        if (cooldowns == null) {
            return 0;
        }

        Long cooldownEnd = cooldowns.get(abilityId);
        if (cooldownEnd == null) {
            return 0;
        }

        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }

    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        }
    }
}

