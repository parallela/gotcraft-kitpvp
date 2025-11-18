package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles custom health regeneration system
 * Configurable rate, delay after damage, and max health
 */
public class HealthRegenerationListener implements Listener {

    private final GotCraftKitPvp plugin;
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private int taskId = -1;

    public HealthRegenerationListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        startRegenerationTask();
    }

    private void startRegenerationTask() {
        if (!plugin.getConfigManager().isHealthRegenEnabled()) {
            return;
        }

        double regenRate = plugin.getConfigManager().getHealthRegenRate();
        if (regenRate <= 0) {
            return; // Regeneration disabled
        }

        // Run task every second (20 ticks)
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int delaySeconds = plugin.getConfigManager().getHealthRegenDelay();
            long currentTime = System.currentTimeMillis();
            double maxHealth = plugin.getConfigManager().getHealthRegenMaxHealth();

            for (Player player : Bukkit.getOnlinePlayers()) {
                // Check if player is alive
                if (!player.isOnline() || player.isDead()) {
                    continue;
                }

                // Get player's max health attribute
                var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
                if (maxHealthAttr == null) {
                    continue;
                }

                double playerMaxHealth = Math.min(maxHealthAttr.getValue(), maxHealth);
                double currentHealth = player.getHealth();

                // Skip if already at max health
                if (currentHealth >= playerMaxHealth) {
                    continue;
                }

                // Check damage delay
                Long lastDamage = lastDamageTime.get(player.getUniqueId());
                if (lastDamage != null) {
                    long timeSinceDamage = (currentTime - lastDamage) / 1000; // Convert to seconds
                    if (timeSinceDamage < delaySeconds) {
                        continue; // Still in delay period
                    }
                }

                // Regenerate health
                double newHealth = Math.min(currentHealth + regenRate, playerMaxHealth);
                player.setHealth(newHealth);
            }
        }, 20L, 20L).getTaskId(); // Run every 20 ticks (1 second)
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (!plugin.getConfigManager().isHealthRegenEnabled()) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Record damage time for delay calculation
        lastDamageTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onVanillaRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Disable vanilla hunger-based regeneration if configured
        if (plugin.getConfigManager().disableVanillaRegen()) {
            EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();

            // Cancel vanilla regeneration reasons
            if (reason == EntityRegainHealthEvent.RegainReason.SATIATED ||
                reason == EntityRegainHealthEvent.RegainReason.REGEN) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Initialize damage time to allow immediate regen on first join
        lastDamageTime.put(event.getPlayer().getUniqueId(), 0L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Cleanup
        lastDamageTime.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Stop the regeneration task (called on plugin disable)
     */
    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        lastDamageTime.clear();
    }
}

