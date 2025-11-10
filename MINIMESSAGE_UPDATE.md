# GotCraftKitPvp - MiniMessage Only Update

## Summary of Changes

This update converts the entire plugin to use **MiniMessage only** for all text formatting, removing all legacy color code support.

## Files Modified

### Core Utility Classes

#### `TextFormatter.java`
- Removed all legacy color code support (`&` codes)
- Removed `smart()`, `legacy()`, `fromLegacy()`, and `strip()` methods
- Kept only MiniMessage parsing methods:
  - `parse(String)` - Parse MiniMessage to Component
  - `toLegacy(Component)` - Convert Component to legacy string (for scoreboards/item meta)
  - `parseLegacy(String)` - Parse MiniMessage and convert to legacy string (shorthand)
  - `gradient()`, `rainbow()` - Helper methods for common formats

#### `MessageManager.java`
- Removed `use-minimessage` toggle
- All messages are now parsed as MiniMessage by default
- Added `parseLegacy(String)` method for item meta and scoreboards
- Updated common message methods to return Components instead of Strings:
  - `getNoPermission()` → returns Component
  - `getPlayerOnly()` → returns Component
  - `getPlayerNotFound()` → returns Component
  - `getReloadSuccess()` → returns Component
- `getMessage(String)` still returns legacy string for backward compatibility with scoreboards

### Command Classes

#### `KitPvpCommand.java`
- Updated all `colorize()` calls to use `sendRawMessage()` or `parseLegacy()`
- Zone wand item lore now uses `parseLegacy()` for ItemMeta
- Help command now sends Component messages properly

### Listener Classes

#### `CombatListener.java`
- Damage indicators now use `sendRawMessage()` with MiniMessage format

#### `PlayerJoinListener.java`
- Spawn item names and lore now use `parseLegacy()` for ItemMeta
- Updated default item names to use MiniMessage format

### Manager Classes

#### `StatsManager.java`
- Kill streak broadcasts now use `sendRawMessage()` to all online players
- Properly broadcasts MiniMessage formatted messages

#### `ScoreboardManager.java`
- Title parsing now uses `parseLegacy()` for scoreboard compatibility
- Line formatting now uses `parseLegacy()` for scoreboard compatibility

#### `GUIManager.java`
- All GUI titles now use `parseLegacy()` for inventory titles
- Kit selector lore now uses `parseLegacy()` with MiniMessage format
- Stats GUI titles now use `parseLegacy()` with MiniMessage format
- Leaderboard GUI titles now use `parseLegacy()` with MiniMessage format

### Configuration Files

#### `messages.yml`
- Removed `use-minimessage: true` toggle
- Updated header documentation to specify MiniMessage-only
- All messages remain in MiniMessage format

#### `config.yml`
- Updated kill streak messages to use MiniMessage format:
  - Changed `&e` to `<yellow>`
  - Changed `&7` to `<gray>`
  - Changed `&c` to `<red>`
  - Changed `&4` to `<dark_red>`
- Updated level reward messages to use MiniMessage format
- Updated ability cooldown message to use MiniMessage format
- Updated safe zone messages to use MiniMessage format
- Spawn item names and lore now use MiniMessage format

#### `README.md`
- Updated to reflect MiniMessage-only support
- Removed legacy color code examples
- Added comprehensive MiniMessage examples including:
  - Common colors
  - Formatting tags
  - Gradients and rainbow
  - Interactive elements (click, hover)

### Database

#### `DatabaseManager.java`
- Fixed compilation errors by recreating the file properly
- Removed duplicate code blocks
- All async database operations work correctly

## Migration Guide

If you're upgrading from a version that used legacy color codes:

1. **Messages**: Replace all `&` color codes with MiniMessage tags:
   - `&c` → `<red>`
   - `&a` → `<green>`
   - `&e` → `<yellow>`
   - `&6` → `<gold>`
   - `&b` → `<aqua>`
   - `&7` → `<gray>`
   - `&f` → `<white>`
   - `&l` → `<bold></bold>`
   - `&o` → `<italic></italic>`
   - `&n` → `<underlined></underlined>`

2. **Config Files**: Update all message strings in `config.yml` to use MiniMessage format

3. **Custom Kit Names**: If you have custom kits, update their display names and lore to use MiniMessage format

## Benefits of MiniMessage-Only

1. **Modern Formatting**: Access to gradients, rainbow text, and advanced formatting
2. **Interactive Text**: Easy click and hover events
3. **Consistency**: Single formatting system across the entire plugin
4. **Better Maintainability**: No need to support two different systems
5. **Future-Proof**: MiniMessage is the modern standard for Paper plugins

## Testing Checklist

- [x] Plugin compiles without errors
- [x] All commands work (`/kitpvp help`, `/kits`, `/stats`, `/leaderboard`)
- [ ] Messages display correctly in chat
- [ ] Scoreboards display correctly
- [ ] GUIs display correctly (kit selector, stats, leaderboard)
- [ ] Item names and lore display correctly
- [ ] Kill streak broadcasts work
- [ ] Level up messages work
- [ ] Zone messages work
- [ ] Damage indicators work

## Breaking Changes

⚠️ **This is a breaking change**: Any custom configurations using legacy color codes (`&`) will need to be updated to MiniMessage format.

## Support

For MiniMessage syntax help, visit: https://docs.advntr.dev/minimessage/format.html

