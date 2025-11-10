package me.lubomirstankov.gotCraftKitPvp.config;

import me.lubomirstankov.gotCraftKitPvp.GotCraftKitPvp;
import me.lubomirstankov.gotCraftKitPvp.utils.TextFormatter;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageManager {

    private final GotCraftKitPvp plugin;
    private FileConfiguration messages;
    private final Map<String, Component> cachedMessages = new HashMap<>();
    private final Map<String, String> cachedLegacyMessages = new HashMap<>();

    public MessageManager(GotCraftKitPvp plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
        cacheMessages();
    }

    private void cacheMessages() {
        cachedMessages.clear();
        cachedLegacyMessages.clear();

        for (String key : messages.getKeys(true)) {
            if (messages.isString(key)) {
                String message = messages.getString(key);
                if (message != null) {
                    Component component = TextFormatter.parse(message);
                    cachedMessages.put(key, component);
                    // Cache legacy version for scoreboards
                    cachedLegacyMessages.put(key, TextFormatter.toLegacy(component));
                }
            }
        }
    }

    public void reload() {
        loadMessages();
    }

    public Component getMessageComponent(String path) {
        return cachedMessages.getOrDefault(path, Component.text(path));
    }

    public Component getMessageComponent(String path, Map<String, String> placeholders) {
        String message = messages.getString(path, path);

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        return TextFormatter.parse(message);
    }

    public String getMessage(String path) {
        // Return legacy formatted string for scoreboard compatibility
        return cachedLegacyMessages.getOrDefault(path, path);
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        Component component = getMessageComponent(path, placeholders);
        return TextFormatter.toLegacy(component);
    }

    public List<Component> getMessageListComponent(String path) {
        List<String> list = messages.getStringList(path);

        if (list == null || list.isEmpty()) {
            plugin.getLogger().warning("Message list '" + path + "' not found or empty!");
            return new ArrayList<>();
        }

        return list.stream()
                .map(TextFormatter::parse)
                .collect(Collectors.toList());
    }

    public List<String> getMessageList(String path) {
        List<String> list = messages.getStringList(path);

        if (list == null || list.isEmpty()) {
            plugin.getLogger().warning("Message list '" + path + "' not found or empty!");
            return new ArrayList<>();
        }

        return list.stream()
                .map(TextFormatter::parseLegacy)
                .collect(Collectors.toList());
    }

    public void sendMessage(Player player, String path) {
        Component message = getMessageComponent(path);
        player.sendMessage(message);
    }

    public void sendMessage(Player player, String path, Map<String, String> placeholders) {
        Component message = getMessageComponent(path, placeholders);
        player.sendMessage(message);
    }

    public void sendRawMessage(Player player, String text) {
        player.sendMessage(TextFormatter.parse(text));
    }

    /**
     * Parse MiniMessage text and return legacy string (for item meta, scoreboards, etc.)
     */
    public String parseLegacy(String text) {
        return TextFormatter.parseLegacy(text);
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    // Common messages
    public Component getNoPermission() {
        return getMessageComponent("no-permission");
    }

    public Component getPlayerOnly() {
        return getMessageComponent("player-only");
    }

    public Component getPlayerNotFound() {
        return getMessageComponent("player-not-found");
    }

    public Component getReloadSuccess() {
        return getMessageComponent("reload-success");
    }
}

