package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;

public class DamageListener implements Listener {

    private final GotCraftKitPvp plugin;

    public DamageListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // Skip if already cancelled
        if (event.isCancelled()) {
            return;
        }

        // Check if in safe zone
        if (plugin.getZoneManager().isInSafeZone(player.getLocation())) {
            if (plugin.getConfig().getBoolean("safe-zones.prevent-damage", true)) {
                event.setCancelled(true);
                return;
            }
        }

        // Fall damage in PvP zones
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            if (!plugin.getConfig().getBoolean("combat.fall-damage", false)) {
                event.setCancelled(true);
                return;
            }
        }

        // Void damage - teleport to spawn
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            if (plugin.getConfig().getBoolean("combat.void-teleport", true)) {
                event.setCancelled(true);
                plugin.getZoneManager().teleportToSpawn(player);
                // Damage player slightly
                player.damage(4.0);
                return;
            }
        }

        // FAKE DEATH SYSTEM - Skip death screen completely
        // Check if this damage would kill the player
        double finalHealth = player.getHealth() - event.getFinalDamage();

        if (finalHealth <= 0.0 && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            // Cancel the actual death
            event.setCancelled(true);

            // Handle fake death
            handleFakeDeath(player, event);
        }
    }

    private void handleFakeDeath(Player victim, EntityDamageEvent event) {
        // Get the killer if this was a player attack
        Player killer = null;
        if (event instanceof EntityDamageByEntityEvent damageByEntityEvent) {
            if (damageByEntityEvent.getDamager() instanceof Player) {
                killer = (Player) damageByEntityEvent.getDamager();
            }
        }

        // Handle stats
        if (killer != null && !killer.equals(victim)) {
            plugin.getStatsManager().handleKill(killer, victim);

            // Check for killstreak ending
            PlayerStats victimStats = plugin.getStatsManager().getStats(victim);
            if (victimStats != null && victimStats.getCurrentStreak() > 3) {
                Map<String, String> placeholders = Map.of(
                        "%killer%", killer.getName(),
                        "%victim%", victim.getName(),
                        "%streak%", String.valueOf(victimStats.getCurrentStreak())
                );
                String message = plugin.getMessageManager().getMessage("killstreak-ended", placeholders);
                plugin.getServer().broadcastMessage(message);
            }
        }

        // Clear inventory
        victim.getInventory().clear();
        victim.getInventory().setArmorContents(null);

        // Teleport to spawn immediately (no death screen!)
        if (plugin.getZoneManager().getArenaSpawn() != null) {
            victim.teleport(plugin.getZoneManager().getArenaSpawn());
        }

        // Reset health and hunger
        victim.setHealth(victim.getMaxHealth());
        victim.setFoodLevel(20);
        victim.setSaturation(20.0f);
        victim.setFireTicks(0);

        // Clear potion effects
        victim.getActivePotionEffects().forEach(effect ->
            victim.removePotionEffect(effect.getType())
        );

        // Play death effect (optional)
        victim.playEffect(org.bukkit.EntityEffect.HURT);

        // Reapply 1.8 combat mechanics
        if (plugin.getConfig().getBoolean("combat.legacy-combat", true)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    var attackSpeed = victim.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED);
                    if (attackSpeed != null) {
                        attackSpeed.setBaseValue(16.0);
                    }
                } catch (Exception e) {
                    // Ignore if attribute doesn't exist
                }
            });
        }

        // Give back kit after delay
        int respawnDelay = plugin.getConfig().getInt("general.respawn-delay", 10);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            String activeKit = plugin.getKitManager().getActiveKit(victim);
            if (activeKit != null) {
                var kit = plugin.getKitManager().getKit(activeKit);
                if (kit != null) {
                    plugin.getKitManager().giveKit(victim, kit);
                }
            }
        }, respawnDelay);

        // Update scoreboard
        final Player finalKiller = killer;
        if (plugin.getScoreboardManager() != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getScoreboardManager().updateScoreboard(victim);
                if (finalKiller != null) {
                    plugin.getScoreboardManager().updateScoreboard(finalKiller);
                }
            });
        }

        // Update health displays for both players
        if (plugin.getHealthTagListener() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getHealthTagListener().updateHealthDisplay(victim);
                if (finalKiller != null) {
                    plugin.getHealthTagListener().updateHealthDisplay(finalKiller);
                }
            }, 2L);
        }
    }
}

