# GotCraftKitPvp

**Professional KitPvP Plugin for Paper 1.20+**

A feature-rich, highly configurable KitPvP plugin with modern features including:
- 🎨 **MiniMessage Only** - Modern text formatting with gradients, rainbows, and more (no legacy color codes)
- 🎯 **1.8 Combat Mechanics** - Classic PvP feel on modern servers
- 📊 **Advanced Statistics** - Track kills, deaths, KDR, streaks, and levels
- 🎁 **Custom Kits** - Fully customizable kits with abilities
- 🗺️ **Zone Management** - Safe zones and PvP zones
- 🏆 **Leaderboards** - Top players by kills, streaks, and levels
- 💰 **Economy Integration** - Vault support for kit purchases
- 🎮 **Modern GUIs** - Clean, paginated interfaces
- ⚡ **Abilities System** - Dash, Heal, Fireball, Teleport, and more
- 📈 **Leveling System** - Gain XP and level up with rewards
- 🎯 **Kill Streaks** - Rewards for maintaining kill streaks
- 🔌 **PlaceholderAPI** - Full PAPI support

## Features

### MiniMessage Format
This plugin uses **MiniMessage only** for all text formatting. No legacy color codes (`&c`, `&a`, etc.) are supported.

**MiniMessage Examples:**
```
<gradient:red:blue>Gradient Text</gradient>
<rainbow>Rainbow Text</rainbow>
<gold><bold>Bold Gold Text</bold></gold>
<hover:show_text:'Tooltip'>Hover me</hover>
<click:run_command:/kits>Click to open kits</click>
```

**Common Colors:**
```
<red>Red text</red>
<green>Green text</green>
<blue>Blue text</blue>
<yellow>Yellow text</yellow>
<gold>Gold text</gold>
<aqua>Aqua text</aqua>
<gray>Gray text</gray>
<white>White text</white>
<dark_red>Dark red</dark_red>
```

**Formatting:**
```
<bold>Bold</bold>
<italic>Italic</italic>
<underlined>Underlined</underlined>
<strikethrough>Strikethrough</strikethrough>
<obfuscated>Obfuscated</obfuscated>
```

All messages in `messages.yml` and `config.yml` use MiniMessage format.

### Kit System
Create custom kits with:
- Custom items with enchantments
- Armor with protection
- Potion effects
- Abilities (Dash, Heal, Fireball, etc.)
- Economy integration (purchase kits)
- Permissions

### Combat Features
- **1.8 Combat**: Classic hit delay mechanics
- **Anti-Cleanup**: Temporary invulnerability after kills
- **Damage Indicators**: Visual feedback on hits
- **Kill Streaks**: Rewards for consecutive kills
- **Custom Death Messages**: Configurable kill/death messages

### Statistics & Progression
- Persistent player stats (MySQL/SQLite)
- Kills, Deaths, K/D Ratio
- Current and Best Kill Streaks
- XP and Leveling System
- Level-up rewards

### Zones
- **Safe Zones**: No PvP, auto-healing
- **PvP Zones**: Combat enabled
- Easy setup with commands
- Per-zone configuration

### GUI System
- Kit Selector with pagination
- Player Statistics viewer
- Leaderboards (Kills, Streaks, Levels)
- Admin management panel

### Abilities
Built-in abilities:
- **Dash**: Launch forward
- **Heal**: Restore health
- **Fireball**: Launch projectile
- **Freeze**: Slow nearby enemies
- **Lightning**: Strike target with lightning
- **Teleport**: Teleport behind target
- **Invisibility**: Become invisible
- **Strength**: Temporary strength boost

## Commands

### Player Commands
- `/kitpvp` - Open main menu
- `/kits` - Select a kit
- `/stats [player]` - View statistics
- `/leaderboard` - View top players

### Admin Commands
- `/kitpvp reload` - Reload configuration
- `/kitpvp setspawn` - Set arena spawn point
- `/kitpvp setzone <safe|pvp>` - Create zones
- `/kitpvp createkit <name>` - Create new kit
- `/kitpvp editkit <name>` - Edit existing kit
- `/kitpvp deletekit <name>` - Delete kit
- `/kitpvp gui` - Open admin panel

## Permissions

### Player Permissions
- `kitpvp.kit.<kitname>` - Access specific kit
- `kitpvp.ability.<ability>` - Use specific ability

### Admin Permissions
- `kitpvp.admin` - All admin permissions
- `kitpvp.command.reload` - Reload plugin
- `kitpvp.command.setspawn` - Set spawn point
- `kitpvp.command.setzone` - Create zones
- `kitpvp.command.createkit` - Create kits
- `kitpvp.command.editkit` - Edit kits
- `kitpvp.command.deletekit` - Delete kits

## Configuration

### Main Config (`config.yml`)
Configure:
- Combat mechanics (1.8 style, hit delay, damage indicators)
- Kill streaks and rewards
- Leveling system and XP requirements
- Economy settings
- Database (SQLite/MySQL)
- Scoreboard layout
- Leaderboard settings
- Safe zone behavior
- Arena spawn settings

### Kits (`kits/*.yml`)
Each kit has its own YAML file:
```yaml
name: "&6&lWarrior"
description: "&7A balanced kit for combat"
icon:
  material: IRON_SWORD
  name: "&6&lWarrior Kit"
  lore:
    - "&7A balanced kit with sword and armor"
permission: "kitpvp.kit.warrior"
price: 500
items:
  - slot: 0
    material: IRON_SWORD
    enchantments:
      - "SHARPNESS:1"
armor:
  helmet:
    material: IRON_HELMET
effects:
  - "SPEED:1"
abilities:
  - "dash"
```

### Messages (`messages.yml`)
Fully customizable messages with MiniMessage support

### Abilities (`abilities.yml`)
Configure ability cooldowns, power, and effects

### Zones (`zones.yml`)
Define safe and PvP zones

## PlaceholderAPI

Available placeholders:
- `%kitpvp_kills%` - Player kills
- `%kitpvp_deaths%` - Player deaths
- `%kitpvp_kdr%` - Kill/Death ratio
- `%kitpvp_streak%` - Current killstreak
- `%kitpvp_best_streak%` - Best killstreak
- `%kitpvp_level%` - Player level
- `%kitpvp_xp%` - Current XP
- `%kitpvp_required_xp%` - XP needed for next level
- `%kitpvp_kit%` - Active kit name

## Database

### SQLite (Default)
No setup required, data stored in `data.db`

### MySQL (Optional)
Configure in `config.yml`:
```yaml
database:
  type: "MYSQL"
  mysql:
    host: "localhost"
    port: 3306
    database: "gotcraftkitpvp"
    username: "root"
    password: "password"
```

## BungeeCord Support

Enable in `config.yml`:
```yaml
general:
  bungee-mode: true
```

Players will spawn directly in the arena when joining the server.

## Dependencies

### Required
- Paper 1.20+ (or compatible fork)

### Optional
- Vault (for economy features)
- PlaceholderAPI (for placeholder support)

## Installation

1. Download the plugin JAR
2. Place in your `plugins/` folder
3. (Optional) Install Vault and an economy plugin
4. (Optional) Install PlaceholderAPI
5. Restart your server
6. Configure `config.yml`, `messages.yml`, and create kits
7. Set spawn with `/kitpvp setspawn`
8. Create zones with `/kitpvp setzone`

## Building from Source

```bash
git clone https://github.com/yourusername/GotCraftKitPvp.git
cd GotCraftKitPvp
mvn clean package
```

The compiled JAR will be in `target/gotcraftkitpvp-1.0-SNAPSHOT.jar`

## Support

For issues, feature requests, or questions:
- GitHub Issues: [Create an issue](https://github.com/yourusername/GotCraftKitPvp/issues)
- Discord: [Your Discord Server]

## License

[Your chosen license]

## Credits

Created by lubomirstankov
Built with ❤️ for the Minecraft community

