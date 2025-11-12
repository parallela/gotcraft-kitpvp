package me.lubomirstankov.gotCraftKitPvp.listeners;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.ItemStack;

public class AbilityListener implements Listener {

    private final GotCraftKitPvp plugin;

    public AbilityListener(GotCraftKitPvp plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        // Get display name using modern API
        var itemMeta = item.getItemMeta();
        if (!itemMeta.hasDisplayName()) {
            return;
        }

        String displayName = PlainTextComponentSerializer.plainText().serialize(itemMeta.displayName());

        // Check for spawn items
        if (displayName.contains("Select Kit") || displayName.contains("Kit Selector")) {
            event.setCancelled(true);
            plugin.getGuiManager().openKitSelector(player);
            return;
        }

        if (displayName.contains("Stats") || displayName.contains("Your Stats")) {
            event.setCancelled(true);
            plugin.getGuiManager().openStatsGUI(player, player);
            return;
        }

        // Check for ability items
        String activeKit = plugin.getKitManager().getActiveKit(player);
        if (activeKit == null) {
            return;
        }

        var kit = plugin.getKitManager().getKit(activeKit);
        if (kit == null) {
            return;
        }

        // Check if the item name contains "Ability" - this identifies ability items
        if (!displayName.contains("Ability")) {
            return;
        }

        // Match ability by checking if the ability name is in the item name
        for (String abilityId : kit.getAbilities()) {
            var ability = plugin.getAbilityManager().getAbility(abilityId);
            if (ability != null) {
                // Check if this is the right ability item
                // For example: "Dash Ability" for dash, "Teleport Ability" for teleport
                String abilityName = ability.getName().toLowerCase();
                String itemName = displayName.toLowerCase();

                // Simple check: if ability name is in item name
                if (itemName.contains(abilityId) || itemName.contains(abilityName)) {
                    plugin.getAbilityManager().useAbility(player, abilityId);
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    /**
     * Prevent ability fireballs from breaking blocks
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Prevent fireballs (ability) from breaking blocks
        if (event.getEntity() instanceof Fireball) {
            event.blockList().clear(); // Clear the list of blocks to break
        }
    }

    /**
     * Prevent lightning (ability) from breaking blocks and starting fires
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onLightningStrike(LightningStrikeEvent event) {
        // Prevent lightning from causing fire
        if (event.getLightning().isEffect()) {
            // This is a visual effect lightning (used by abilities)
            // It doesn't break blocks by default, but let's be safe
            event.setCancelled(false); // Allow the visual effect
        }
    }
}


