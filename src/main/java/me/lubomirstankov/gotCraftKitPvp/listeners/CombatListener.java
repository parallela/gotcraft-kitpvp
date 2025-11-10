package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {

    private final GotCraftKitPvp plugin;
    private final Map<UUID, Long> lastHitTime = new HashMap<>();

    public CombatListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }

        // Check if in safe zone
        if (plugin.getZoneManager().isInSafeZone(victim.getLocation()) ||
            plugin.getZoneManager().isInSafeZone(attacker.getLocation())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(attacker, "no-pvp-in-safe-zone");
            return;
        }

        // Legacy combat (1.8 hit delay and mechanics)
        if (plugin.getConfigManager().isLegacyCombat()) {
            UUID attackerId = attacker.getUniqueId();
            long currentTime = System.currentTimeMillis();

            if (lastHitTime.containsKey(attackerId)) {
                long timeSinceLastHit = currentTime - lastHitTime.get(attackerId);
                int hitDelay = plugin.getConfigManager().getHitDelay() * 50; // Convert ticks to ms

                if (timeSinceLastHit < hitDelay) {
                    event.setCancelled(true);
                    return;
                }
            }

            lastHitTime.put(attackerId, currentTime);

            // Disable sprinting when attacking
            if (attacker.isSprinting()) {
                attacker.setSprinting(false);
            }

            // Apply 1.8-style damage multiplier if configured
            if (plugin.getConfig().getBoolean("combat.legacy-damage-multiplier", true)) {
                event.setDamage(event.getDamage() * 1.05);
            }
        }

        // Hit sounds
        if (plugin.getConfigManager().isHitSounds()) {
            String soundName = plugin.getConfig().getString("combat.hit-sound", "ENTITY_PLAYER_HURT");
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                attacker.playSound(attacker.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid hit sound: " + soundName);
            }
        }

        // Damage indicators
        if (plugin.getConfigManager().isDamageIndicators()) {
            double damage = event.getFinalDamage();
            plugin.getMessageManager().sendRawMessage(victim, "<red>-" + String.format("%.1f", damage) + " ❤");
        }
    }
}

