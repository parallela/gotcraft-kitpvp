package me.lubomirstankov.gotCraftKitPvp.zones;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

public class ZoneManager {

    private final ZoneSelection zoneSelection = new ZoneSelection();
    private final GotCraftKitPvp plugin;
    private final Map<String, Zone> zones = new HashMap<>();
    private final Map<UUID, Zone> playerZones = new HashMap<>();
    private Location arenaSpawn;

    public ZoneManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        loadZones();
        loadArenaSpawn();
        startHealingTask();
        startEffectTask();
    }

    public void loadZones() {
        zones.clear();

        ConfigurationSection zonesSection = plugin.getConfigManager().getZonesConfig().getConfigurationSection("zones");
        if (zonesSection != null) {
            for (String zoneId : zonesSection.getKeys(false)) {
                ConfigurationSection zoneSection = zonesSection.getConfigurationSection(zoneId);

                String typeName = zoneSection.getString("type", "PVP");
                Zone.ZoneType type = Zone.ZoneType.valueOf(typeName);

                String worldName = zoneSection.getString("world");
                World world = Bukkit.getWorld(worldName);

                if (world == null) {
                    plugin.getLogger().warning("World " + worldName + " not found for zone " + zoneId);
                    continue;
                }

                ConfigurationSection minSection = zoneSection.getConfigurationSection("min");
                ConfigurationSection maxSection = zoneSection.getConfigurationSection("max");

                Location min = new Location(
                        world,
                        minSection.getDouble("x"),
                        minSection.getDouble("y"),
                        minSection.getDouble("z")
                );

                Location max = new Location(
                        world,
                        maxSection.getDouble("x"),
                        maxSection.getDouble("y"),
                        maxSection.getDouble("z")
                );

                Zone zone = new Zone(zoneId, type, world, min, max);
                zones.put(zoneId, zone);

                plugin.getLogger().info("Loaded zone: " + zoneId + " (" + type + ")");
            }
        }

        plugin.getLogger().info("Loaded " + zones.size() + " zones!");
    }

    public void loadArenaSpawn() {
        ConfigurationSection spawnSection = plugin.getConfig().getConfigurationSection("arena.spawn");
        if (spawnSection != null) {
            String worldName = spawnSection.getString("world", "world");
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                arenaSpawn = new Location(
                        world,
                        spawnSection.getDouble("x", 0),
                        spawnSection.getDouble("y", 100),
                        spawnSection.getDouble("z", 0),
                        (float) spawnSection.getDouble("yaw", 0),
                        (float) spawnSection.getDouble("pitch", 0)
                );
            }
        }
    }

    public void reload() {
        loadZones();
        loadArenaSpawn();
    }

    public void saveZone(Zone zone) {
        zones.put(zone.getId(), zone);

        ConfigurationSection zonesSection = plugin.getConfigManager().getZonesConfig().getConfigurationSection("zones");
        if (zonesSection == null) {
            zonesSection = plugin.getConfigManager().getZonesConfig().createSection("zones");
        }

        ConfigurationSection zoneSection = zonesSection.createSection(zone.getId());
        zoneSection.set("type", zone.getType().name());
        zoneSection.set("world", zone.getWorld().getName());
        zoneSection.set("min.x", zone.getMin().getX());
        zoneSection.set("min.y", zone.getMin().getY());
        zoneSection.set("min.z", zone.getMin().getZ());
        zoneSection.set("max.x", zone.getMax().getX());
        zoneSection.set("max.y", zone.getMax().getY());
        zoneSection.set("max.z", zone.getMax().getZ());

        plugin.getConfigManager().saveZonesConfig();
    }

    public void deleteZone(String zoneId) {
        zones.remove(zoneId);

        ConfigurationSection zonesSection = plugin.getConfigManager().getZonesConfig().getConfigurationSection("zones");
        if (zonesSection != null) {
            zonesSection.set(zoneId, null);
            plugin.getConfigManager().saveZonesConfig();
        }
    }

    public Zone getZoneAt(Location location) {
        for (Zone zone : zones.values()) {
            if (zone.isInZone(location)) {
                return zone;
            }
        }
        return null;
    }

    public boolean isInSafeZone(Location location) {
        Zone zone = getZoneAt(location);
        return zone != null && zone.getType() == Zone.ZoneType.SAFE;
    }

    public boolean isInPvPZone(Location location) {
        Zone zone = getZoneAt(location);
        return zone != null && zone.getType() == Zone.ZoneType.PVP;
    }

    public boolean isInDoubleDamageZone(Location location) {
        Zone zone = getZoneAt(location);
        return zone != null && zone.getType() == Zone.ZoneType.DOUBLE_DAMAGE;
    }

    public boolean isInGravityZone(Location location) {
        Zone zone = getZoneAt(location);
        return zone != null && zone.getType() == Zone.ZoneType.GRAVITY;
    }

    public boolean isInLevitationZone(Location location) {
        Zone zone = getZoneAt(location);
        return zone != null && zone.getType() == Zone.ZoneType.LEVITATION;
    }

    public boolean isInNauseaZone(Location location) {
        Zone zone = getZoneAt(location);
        return zone != null && zone.getType() == Zone.ZoneType.NAUSEA;
    }

    public void updatePlayerZone(Player player) {
        Zone previousZone = playerZones.get(player.getUniqueId());
        Zone currentZone = getZoneAt(player.getLocation());

        if (previousZone != currentZone) {
            if (previousZone != null) {
                handleZoneExit(player, previousZone);
            }

            if (currentZone != null) {
                handleZoneEnter(player, currentZone);
            }

            playerZones.put(player.getUniqueId(), currentZone);
        }
    }

    private void handleZoneEnter(Player player, Zone zone) {
        switch (zone.getType()) {
            case SAFE:
                plugin.getMessageManager().sendMessage(player, "safe-zone-enter");
                break;
            case PVP:
                plugin.getMessageManager().sendMessage(player, "pvp-zone-enter");
                break;
            case DOUBLE_DAMAGE:
                player.sendMessage("§c§l⚔ §cYou entered a §4DOUBLE DAMAGE §czone!");
                break;
            case GRAVITY:
                player.sendMessage("§b§l✦ §bYou entered a §3GRAVITY §bzone! You will float!");
                break;
            case LEVITATION:
                player.sendMessage("§e§l↑ §eYou entered a §6LEVITATION §ezone!");
                break;
            case NAUSEA:
                player.sendMessage("§5§l✹ §5You entered a §dNAUSEA §5zone!");
                break;
        }
    }

    private void handleZoneExit(Player player, Zone zone) {
        switch (zone.getType()) {
            case SAFE:
                plugin.getMessageManager().sendMessage(player, "safe-zone-exit");
                break;
            case PVP:
                plugin.getMessageManager().sendMessage(player, "pvp-zone-exit");
                break;
            case DOUBLE_DAMAGE:
                player.sendMessage("§c§l⚔ §7You left the §4DOUBLE DAMAGE §7zone.");
                break;
            case GRAVITY:
                player.sendMessage("§b§l✦ §7You left the §3GRAVITY §7zone.");
                break;
            case LEVITATION:
                player.sendMessage("§e§l↑ §7You left the §6LEVITATION §7zone.");
                break;
            case NAUSEA:
                player.sendMessage("§5§l✹ §7You left the §dNAUSEA §7zone.");
                break;
        }
    }

    public void setArenaSpawn(Location location) {
        this.arenaSpawn = location;

        plugin.getConfig().set("arena.spawn.world", location.getWorld().getName());
        plugin.getConfig().set("arena.spawn.x", location.getX());
        plugin.getConfig().set("arena.spawn.y", location.getY());
        plugin.getConfig().set("arena.spawn.z", location.getZ());
        plugin.getConfig().set("arena.spawn.yaw", location.getYaw());
        plugin.getConfig().set("arena.spawn.pitch", location.getPitch());
        plugin.saveConfig();
    }

    public Location getArenaSpawn() {
        return arenaSpawn;
    }

    public void teleportToSpawn(Player player) {
        if (arenaSpawn != null) {
            player.teleport(arenaSpawn);
        }
    }

    private void startHealingTask() {
        if (!plugin.getConfig().getBoolean("safe-zones.healing", true)) {
            return;
        }

        int interval = plugin.getConfig().getInt("safe-zones.healing-interval", 40);
        double amount = plugin.getConfig().getDouble("safe-zones.healing-amount", 1.0);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (isInSafeZone(player.getLocation())) {
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getMaxHealth();

                    if (currentHealth < maxHealth) {
                        player.setHealth(Math.min(maxHealth, currentHealth + amount));
                    }
                }
            }
        }, interval, interval);
    }

    private void startEffectTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                Location loc = player.getLocation();

                // Apply Gravity effect (levitation with low amplitude to make player float)
                if (isInGravityZone(loc)) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.LEVITATION,
                            60,
                            0,
                            false,
                            false,
                            false
                    ));
                }

                // Apply Levitation effect (15 seconds duration when entering)
                if (isInLevitationZone(loc)) {
                    // Check if player doesn't already have levitation or has low duration
                    org.bukkit.potion.PotionEffect currentLevitation = player.getPotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION);
                    if (currentLevitation == null || currentLevitation.getDuration() < 40) {
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.LEVITATION,
                                300, // 15 seconds
                                1,
                                false,
                                false,
                                false
                        ));
                    }
                }

                // Apply Nausea effect
                if (isInNauseaZone(loc)) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.NAUSEA,
                            300, // 15 seconds
                            0,
                            false,
                            false,
                            false
                    ));
                }
            }
        }, 20L, 20L); // Run every second
    }

    public Collection<Zone> getAllZones() {
        return zones.values();
    }

    public void clearPlayerZone(UUID uuid) {
        playerZones.remove(uuid);
    }

    public ZoneSelection getZoneSelection() {
        return zoneSelection;
    }
}

