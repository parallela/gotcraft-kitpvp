package me.lubomirstankov.gotCraftKitPvp.zones;

import org.bukkit.Location;
import org.bukkit.World;

public class Zone {

    private final String id;
    private final ZoneType type;
    private final World world;
    private final Location min;
    private final Location max;

    public Zone(String id, ZoneType type, World world, Location min, Location max) {
        this.id = id;
        this.type = type;
        this.world = world;
        this.min = min;
        this.max = max;
    }

    public boolean isInZone(Location location) {
        if (!location.getWorld().equals(world)) {
            return false;
        }

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        return x >= Math.min(min.getX(), max.getX()) && x <= Math.max(min.getX(), max.getX())
                && y >= Math.min(min.getY(), max.getY()) && y <= Math.max(min.getY(), max.getY())
                && z >= Math.min(min.getZ(), max.getZ()) && z <= Math.max(min.getZ(), max.getZ());
    }

    // Getters
    public String getId() {
        return id;
    }

    public ZoneType getType() {
        return type;
    }

    public World getWorld() {
        return world;
    }

    public Location getMin() {
        return min;
    }

    public Location getMax() {
        return max;
    }

    public enum ZoneType {
        SAFE,
        PVP,
        DOUBLE_DAMAGE,
        GRAVITY,
        LEVITATION,
        NAUSEA
    }
}

