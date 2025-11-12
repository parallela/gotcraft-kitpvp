package me.lubomirstankov.gotCraftKitPvp.kits;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Kit {

    private final String id;
    private String name;
    private String description;
    private KitIcon icon;
    private String permission;
    private double price;
    private boolean free;
    private int cooldown;
    private List<KitItem> items;
    private Map<String, KitItem> armor;
    private List<String> effects;
    private List<String> abilities;

    public Kit(String id) {
        this.id = id;
        this.items = new ArrayList<>();
        this.armor = new HashMap<>();
        this.effects = new ArrayList<>();
        this.abilities = new ArrayList<>();
    }

    public void loadFromConfig(ConfigurationSection config) {
        this.name = config.getString("name", id);
        this.description = config.getString("description", "");

        // Load icon
        if (config.contains("icon")) {
            ConfigurationSection iconSection = config.getConfigurationSection("icon");
            this.icon = new KitIcon(
                    Material.valueOf(iconSection.getString("material", "CHEST")),
                    iconSection.getString("name", name),
                    iconSection.getStringList("lore")
            );
        } else {
            this.icon = new KitIcon(Material.CHEST, name, new ArrayList<>());
        }

        this.permission = config.getString("permission", "");
        this.price = config.getDouble("price", 0);
        this.free = config.getBoolean("free", false);
        this.cooldown = config.getInt("cooldown", 0);

        // Load items
        if (config.contains("items")) {
            for (Map<?, ?> itemMap : config.getMapList("items")) {
                items.add(parseKitItem(itemMap));
            }
        }

        // Load armor
        if (config.contains("armor")) {
            ConfigurationSection armorSection = config.getConfigurationSection("armor");
            if (armorSection.contains("helmet")) {
                armor.put("helmet", parseArmorItem(armorSection.getConfigurationSection("helmet")));
            }
            if (armorSection.contains("chestplate")) {
                armor.put("chestplate", parseArmorItem(armorSection.getConfigurationSection("chestplate")));
            }
            if (armorSection.contains("leggings")) {
                armor.put("leggings", parseArmorItem(armorSection.getConfigurationSection("leggings")));
            }
            if (armorSection.contains("boots")) {
                armor.put("boots", parseArmorItem(armorSection.getConfigurationSection("boots")));
            }
        }

        // Load effects
        if (config.contains("effects")) {
            this.effects = config.getStringList("effects");
        }

        // Load abilities
        if (config.contains("abilities")) {
            this.abilities = config.getStringList("abilities");
        }
    }

    private KitItem parseKitItem(Map<?, ?> map) {
        Object slotObj = map.containsKey("slot") ? map.get("slot") : 0;
        int slot = slotObj instanceof Number ? ((Number) slotObj).intValue() : 0;

        Object materialObj = map.containsKey("material") ? map.get("material") : "STONE";
        Material material = Material.valueOf(String.valueOf(materialObj));

        Object amountObj = map.containsKey("amount") ? map.get("amount") : 1;
        int amount = amountObj instanceof Number ? ((Number) amountObj).intValue() : 1;

        String name = (String) map.get("name");
        @SuppressWarnings("unchecked")
        List<String> lore = (List<String>) map.get("lore");
        @SuppressWarnings("unchecked")
        List<String> enchantments = (List<String>) map.get("enchantments");

        return new KitItem(slot, material, amount, name, lore, enchantments);
    }

    private KitItem parseArmorItem(ConfigurationSection section) {
        Material material = Material.valueOf(section.getString("material", "AIR"));
        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");
        List<String> enchantments = section.getStringList("enchantments");

        return new KitItem(0, material, 1, name, lore, enchantments);
    }

    public ItemStack[] getArmorContents() {
        ItemStack[] armorContents = new ItemStack[4];
        armorContents[3] = armor.containsKey("helmet") ? armor.get("helmet").toItemStack() : null;
        armorContents[2] = armor.containsKey("chestplate") ? armor.get("chestplate").toItemStack() : null;
        armorContents[1] = armor.containsKey("leggings") ? armor.get("leggings").toItemStack() : null;
        armorContents[0] = armor.containsKey("boots") ? armor.get("boots").toItemStack() : null;
        return armorContents;
    }

    public ItemStack[] getInventoryContents() {
        ItemStack[] contents = new ItemStack[36];
        for (KitItem item : items) {
            if (item.getSlot() >= 0 && item.getSlot() < 36) {
                contents[item.getSlot()] = item.toItemStack();
            }
        }
        return contents;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public KitIcon getIcon() {
        return icon;
    }

    public String getPermission() {
        return permission;
    }

    public double getPrice() {
        return price;
    }

    public boolean isFree() {
        return free;
    }

    public int getCooldown() {
        return cooldown;
    }

    public List<KitItem> getItems() {
        return items;
    }

    public Map<String, KitItem> getArmor() {
        return armor;
    }

    public List<String> getEffects() {
        return effects;
    }

    public List<String> getAbilities() {
        return abilities;
    }

    // Setters for kit editor
    public void setArmorContents(ItemStack[] armorContents) {
        armor.clear();
        if (armorContents[3] != null) { // Helmet
            armor.put("helmet", itemStackToKitItem(armorContents[3], 0));
        }
        if (armorContents[2] != null) { // Chestplate
            armor.put("chestplate", itemStackToKitItem(armorContents[2], 0));
        }
        if (armorContents[1] != null) { // Leggings
            armor.put("leggings", itemStackToKitItem(armorContents[1], 0));
        }
        if (armorContents[0] != null) { // Boots
            armor.put("boots", itemStackToKitItem(armorContents[0], 0));
        }
    }

    public void setInventoryContents(ItemStack[] contents) {
        items.clear();
        for (int i = 0; i < contents.length && i < 36; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                items.add(itemStackToKitItem(contents[i], i));
            }
        }
    }

    private KitItem itemStackToKitItem(ItemStack item, int slot) {
        List<String> enchantments = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            // Use the modern key instead of deprecated getName()
            String enchantKey = entry.getKey().getKey().getKey().toUpperCase();
            enchantments.add(enchantKey + ":" + entry.getValue());
        }

        String name = null;
        List<String> lore = null;
        if (item.hasItemMeta() && item.getItemMeta() != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                name = meta.getDisplayName();
            }
            if (meta.hasLore()) {
                lore = meta.getLore();
            }
        }

        return new KitItem(slot, item.getType(), item.getAmount(), name, lore, enchantments);
    }

    public void saveToConfig(org.bukkit.configuration.file.FileConfiguration config) {
        config.set("name", name);
        config.set("description", description);
        config.set("permission", permission);
        config.set("price", price);
        config.set("free", free);
        config.set("cooldown", cooldown);

        // Save icon
        config.set("icon.material", icon.getMaterial().name());
        config.set("icon.name", icon.getName());
        config.set("icon.lore", icon.getLore());

        // Save items
        List<Map<String, Object>> itemsList = new ArrayList<>();
        for (KitItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("slot", item.getSlot());
            itemMap.put("material", item.getMaterial().name());
            itemMap.put("amount", item.getAmount());
            if (item.getName() != null) {
                itemMap.put("name", item.getName());
            }
            if (item.getLore() != null && !item.getLore().isEmpty()) {
                itemMap.put("lore", item.getLore());
            }
            if (item.getEnchantments() != null && !item.getEnchantments().isEmpty()) {
                itemMap.put("enchantments", item.getEnchantments());
            }
            itemsList.add(itemMap);
        }
        config.set("items", itemsList);

        // Save armor
        for (Map.Entry<String, KitItem> entry : armor.entrySet()) {
            String armorType = entry.getKey();
            KitItem item = entry.getValue();
            config.set("armor." + armorType + ".material", item.getMaterial().name());
            if (item.getName() != null) {
                config.set("armor." + armorType + ".name", item.getName());
            }
            if (item.getLore() != null && !item.getLore().isEmpty()) {
                config.set("armor." + armorType + ".lore", item.getLore());
            }
            if (item.getEnchantments() != null && !item.getEnchantments().isEmpty()) {
                config.set("armor." + armorType + ".enchantments", item.getEnchantments());
            }
        }

        // Save effects and abilities
        config.set("effects", effects);
        config.set("abilities", abilities);
    }

    // Inner classes
    public static class KitIcon {
        private final Material material;
        private final String name;
        private final List<String> lore;

        public KitIcon(Material material, String name, List<String> lore) {
            this.material = material;
            this.name = name;
            this.lore = lore;
        }

        public ItemStack toItemStack() {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (name != null) {
                    meta.setDisplayName(parseColorFormat(name));
                }
                if (lore != null && !lore.isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(parseColorFormat(line));
                    }
                    meta.setLore(coloredLore);
                }
                item.setItemMeta(meta);
            }
            return item;
        }

        /**
         * Parses text supporting both legacy color codes (&c) and MiniMessage (<red>)
         * Tries MiniMessage first, then falls back to legacy codes
         */
        private String parseColorFormat(String text) {
            try {
                // First, translate legacy color codes (&) to section symbols (§)
                String withLegacy = org.bukkit.ChatColor.translateAlternateColorCodes('&', text);

                // Then try to parse as MiniMessage (handles tags like <red>, <gradient>, etc.)
                // MiniMessage will ignore § codes and pass them through
                net.kyori.adventure.text.Component component =
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(withLegacy);

                // Convert back to legacy format for ItemMeta (supports § color codes)
                return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .serialize(component);
            } catch (Exception e) {
                // Fallback: just use legacy color code translation
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
            }
        }

        public Material getMaterial() {
            return material;
        }

        public String getName() {
            return name;
        }

        public List<String> getLore() {
            return lore;
        }
    }

    public static class KitItem {
        private final int slot;
        private final Material material;
        private final int amount;
        private final String name;
        private final List<String> lore;
        private final List<String> enchantments;

        public KitItem(int slot, Material material, int amount, String name, List<String> lore, List<String> enchantments) {
            this.slot = slot;
            this.material = material;
            this.amount = amount;
            this.name = name;
            this.lore = lore;
            this.enchantments = enchantments;
        }

        public ItemStack toItemStack() {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                if (name != null) {
                    // Support both legacy color codes (&c) and MiniMessage (<red>)
                    String parsed = parseColorFormat(name);
                    meta.setDisplayName(parsed);
                }

                if (lore != null && !lore.isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        // Support both legacy color codes (&c) and MiniMessage (<red>)
                        String parsed = parseColorFormat(line);
                        coloredLore.add(parsed);
                    }
                    meta.setLore(coloredLore);
                }

                item.setItemMeta(meta);
            }

            // Add enchantments
            if (enchantments != null) {
                for (String enchantmentString : enchantments) {
                    try {
                        String[] parts = enchantmentString.split(":");
                        String enchantKey = parts[0].toLowerCase();
                        int level = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                        // Try modern key first (e.g., "sharpness", "protection")
                        Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantKey));

                        // Fallback to deprecated getName() for backwards compatibility
                        if (enchantment == null) {
                            enchantment = Enchantment.getByName(parts[0]);
                        }

                        if (enchantment != null) {
                            item.addUnsafeEnchantment(enchantment, level);
                        }
                    } catch (Exception e) {
                        // Skip invalid enchantment
                    }
                }
            }

            return item;
        }

        /**
         * Parses text supporting both legacy color codes (&c) and MiniMessage (<red>)
         * Tries MiniMessage first, then falls back to legacy codes
         */
        private String parseColorFormat(String text) {
            try {
                // First, translate legacy color codes (&) to section symbols (§)
                String withLegacy = org.bukkit.ChatColor.translateAlternateColorCodes('&', text);

                // Then try to parse as MiniMessage (handles tags like <red>, <gradient>, etc.)
                // MiniMessage will ignore § codes and pass them through
                net.kyori.adventure.text.Component component =
                    net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(withLegacy);

                // Convert back to legacy format for ItemMeta (supports § color codes)
                return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .serialize(component);
            } catch (Exception e) {
                // Fallback: just use legacy color code translation
                return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
            }
        }

        public int getSlot() {
            return slot;
        }

        public Material getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }

        public String getName() {
            return name;
        }

        public List<String> getLore() {
            return lore;
        }

        public List<String> getEnchantments() {
            return enchantments;
        }
    }
}

