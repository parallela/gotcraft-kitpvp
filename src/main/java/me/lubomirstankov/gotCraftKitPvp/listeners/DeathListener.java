package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.stats.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FALLBACK DEATH HANDLER
 * This listener handles PlayerDeathEvent as a fallback.
 * The primary death handling is now done in DamageListener using a fake death system
 * to completely skip the death screen. This only triggers if somehow a real death occurs.
 */
public class DeathListener implements Listener {

    private final GotCraftKitPvp plugin;
    private final Map<UUID, Long> antiCleanupTimers = new HashMap<>();

    public DeathListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = victim.getKiller();

        // Keep inventory and experience
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);

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

            // Disable death message
            event.setDeathMessage(null);

            // Anti-cleanup
            int antiCleanupDuration = plugin.getConfigManager().getAntiCleanupDuration();
            if (antiCleanupDuration > 0) {
                antiCleanupTimers.put(killer.getUniqueId(), System.currentTimeMillis() + (antiCleanupDuration * 1000L));
            }
        } else {
            // Disable death message
            event.setDeathMessage(null);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Teleport to spawn
        if (plugin.getZoneManager().getArenaSpawn() != null) {
            event.setRespawnLocation(plugin.getZoneManager().getArenaSpawn());
        }

        // Clear respawn title screen immediately and repeatedly
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Clear using Adventure API (modern way)
            player.clearTitle();
            player.showTitle(net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.empty(),
                net.kyori.adventure.text.Component.empty(),
                net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ZERO,
                    java.time.Duration.ZERO,
                    java.time.Duration.ZERO
                )
            ));
        });

        // Clear again after a few ticks to ensure it's gone
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.clearTitle();
            player.showTitle(net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.empty(),
                net.kyori.adventure.text.Component.empty(),
                net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ZERO,
                    java.time.Duration.ZERO,
                    java.time.Duration.ZERO
                )
            ));
        }, 5L);

        // Reapply 1.8 combat mechanics
        if (plugin.getConfig().getBoolean("combat.legacy-combat", true)) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                try {
                    var attackSpeed = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED);
                    if (attackSpeed != null) {
                        attackSpeed.setBaseValue(16.0);
                    }
                } catch (Exception e) {
                    // Ignore if attribute doesn't exist
                }
            }, 1L);
        }

        // Give back kit after delay
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            String activeKit = plugin.getKitManager().getActiveKit(player);
            if (activeKit != null) {
                var kit = plugin.getKitManager().getKit(activeKit);
                if (kit != null) {
                    plugin.getKitManager().giveKit(player, kit);
                }
            }
        }, plugin.getConfig().getInt("general.respawn-delay", 10));
    }

    public boolean hasAntiCleanup(UUID uuid) {
        Long endTime = antiCleanupTimers.get(uuid);
        if (endTime == null) {
            return false;
        }

        if (System.currentTimeMillis() > endTime) {
            antiCleanupTimers.remove(uuid);
            return false;
        }

        return true;
    }
}

