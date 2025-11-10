package me.lubomirstankov.gotCraftKitPvp.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Utility class for text formatting with MiniMessage
 * MiniMessage only - no legacy color code support
 */
public class TextFormatter {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

    /**
     * Parse MiniMessage text to Component
     * Example: <gradient:red:blue>Text</gradient>
     * Example: <rainbow>Rainbow text</rainbow>
     * Example: <gold><bold>Bold gold text</bold></gold>
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        return miniMessage.deserialize(text);
    }

    /**
     * Convert Component to legacy string (for scoreboards, etc.)
     */
    public static String toLegacy(Component component) {
        return legacySerializer.serialize(component);
    }

    /**
     * Parse MiniMessage text and convert to legacy string
     */
    public static String parseLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return toLegacy(parse(text));
    }

    /**
     * Create a gradient text
     * Example: gradient("Text", "#ff0000", "#0000ff") -> red to blue gradient
     */
    public static Component gradient(String text, String startColor, String endColor) {
        String miniMessageText = String.format("<gradient:%s:%s>%s</gradient>", startColor, endColor, text);
        return miniMessage.deserialize(miniMessageText);
    }

    /**
     * Create rainbow text
     */
    public static Component rainbow(String text) {
        return miniMessage.deserialize("<rainbow>" + text + "</rainbow>");
    }

    /**
     * Create rainbow text with phase
     */
    public static Component rainbow(String text, int phase) {
        return miniMessage.deserialize("<rainbow:" + phase + ">" + text + "</rainbow>");
    }
}

