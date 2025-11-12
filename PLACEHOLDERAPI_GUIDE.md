# PlaceholderAPI Integration - Complete Guide

## Overview

GotCraftKitPvp now includes comprehensive PlaceholderAPI support with **leaderboard placeholders** that can be used in:
- 🏆 Holograms (DecentHolograms, HolographicDisplays, etc.)
- 🤖 NPCs (Citizens, FancyNPCs, etc.)
- 💬 Chat plugins
- 📋 Scoreboards
- 🎨 TabList plugins
- And any other plugin that supports PlaceholderAPI!

## Installation

1. Install PlaceholderAPI on your server
2. Install GotCraftKitPvp
3. Run `/papi reload` to register the expansion
4. Use placeholders with the prefix `%kitpvp_`

## Available Placeholders

### Player Statistics (Player-Specific)

These placeholders work for the player viewing them:

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%kitpvp_kills%` | Player's total kills | `42` |
| `%kitpvp_deaths%` | Player's total deaths | `15` |
| `%kitpvp_kdr%` | Player's K/D ratio | `2.80` |
| `%kitpvp_streak%` | Current kill streak | `5` |
| `%kitpvp_killstreak%` | Current kill streak (alias) | `5` |
| `%kitpvp_best_streak%` | Best kill streak ever | `12` |
| `%kitpvp_beststreak%` | Best kill streak (alias) | `12` |
| `%kitpvp_level%` | Player's level | `8` |
| `%kitpvp_xp%` | Current XP | `250` |
| `%kitpvp_required_xp%` | XP needed for next level | `500` |
| `%kitpvp_requiredxp%` | XP needed (alias) | `500` |
| `%kitpvp_kit%` | Current active kit name | `Warrior` |
| `%kitpvp_balance%` | Player's money balance | `1250.50` |
| `%kitpvp_money%` | Player's money (alias) | `1250.50` |

### Leaderboard Placeholders (Top Players)

These placeholders show **top player statistics** and work anywhere:

#### Top Killer Names

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%kitpvp_killer_1%` | Name of #1 top killer | `Steve` |
| `%kitpvp_killer_2%` | Name of #2 top killer | `Alex` |
| `%kitpvp_killer_3%` | Name of #3 top killer | `Notch` |
| `%kitpvp_killer_4%` | Name of #4 top killer | `Herobrine` |
| `%kitpvp_killer_5%` | Name of #5 top killer | `Jeb` |
| ... | ... | ... |
| `%kitpvp_killer_10%` | Name of #10 top killer | `Player10` |

**Note:** You can use any number from 1 to 100 (e.g., `%kitpvp_killer_25%`)

#### Top Killer Kills Count

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%kitpvp_kills_1%` | Kills of #1 player | `1523` |
| `%kitpvp_kills_2%` | Kills of #2 player | `1204` |
| `%kitpvp_kills_3%` | Kills of #3 player | `987` |
| `%kitpvp_kills_4%` | Kills of #4 player | `845` |
| `%kitpvp_kills_5%` | Kills of #5 player | `723` |
| ... | ... | ... |
| `%kitpvp_kills_10%` | Kills of #10 player | `456` |

#### Top Player Deaths

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%kitpvp_deaths_1%` | Deaths of #1 player | `423` |
| `%kitpvp_deaths_2%` | Deaths of #2 player | `512` |
| `%kitpvp_deaths_3%` | Deaths of #3 player | `398` |

#### Top Player K/D Ratio

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%kitpvp_kdr_1%` | K/D ratio of #1 player | `3.60` |
| `%kitpvp_kdr_2%` | K/D ratio of #2 player | `2.35` |
| `%kitpvp_kdr_3%` | K/D ratio of #3 player | `2.48` |

#### Top Player Best Streaks

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%kitpvp_streak_1%` | Best streak of #1 player | `28` |
| `%kitpvp_streak_2%` | Best streak of #2 player | `24` |
| `%kitpvp_streak_3%` | Best streak of #3 player | `19` |

#### Top Player Levels

| Placeholder | Description | Example Output |
|------------|-------------|----------------|
| `%kitpvp_level_1%` | Level of #1 player | `42` |
| `%kitpvp_level_2%` | Level of #2 player | `38` |
| `%kitpvp_level_3%` | Level of #3 player | `35` |

## Usage Examples

### Example 1: Top 3 Killers Hologram

Using DecentHolograms or HolographicDisplays:

```yaml
# holograms.yml or similar
top-killers:
  lines:
    - "&6&l⚔ TOP KILLERS ⚔"
    - ""
    - "&e#1 &f%kitpvp_killer_1% &7- &c%kitpvp_kills_1% kills"
    - "&e#2 &f%kitpvp_killer_2% &7- &c%kitpvp_kills_2% kills"
    - "&e#3 &f%kitpvp_killer_3% &7- &c%kitpvp_kills_3% kills"
```

**Result:**
```
⚔ TOP KILLERS ⚔

#1 Steve - 1523 kills
#2 Alex - 1204 kills
#3 Notch - 987 kills
```

### Example 2: Top 10 Leaderboard Hologram

```yaml
leaderboard:
  lines:
    - "&6&l━━━━━━━━━━━━━━━━━━━━━"
    - "&e&lTOP 10 KILLERS"
    - "&6&l━━━━━━━━━━━━━━━━━━━━━"
    - ""
    - "&e1. &f%kitpvp_killer_1% &7[&c%kitpvp_kills_1%&7]"
    - "&e2. &f%kitpvp_killer_2% &7[&c%kitpvp_kills_2%&7]"
    - "&e3. &f%kitpvp_killer_3% &7[&c%kitpvp_kills_3%&7]"
    - "&e4. &f%kitpvp_killer_4% &7[&c%kitpvp_kills_4%&7]"
    - "&e5. &f%kitpvp_killer_5% &7[&c%kitpvp_kills_5%&7]"
    - "&e6. &f%kitpvp_killer_6% &7[&c%kitpvp_kills_6%&7]"
    - "&e7. &f%kitpvp_killer_7% &7[&c%kitpvp_kills_7%&7]"
    - "&e8. &f%kitpvp_killer_8% &7[&c%kitpvp_kills_8%&7]"
    - "&e9. &f%kitpvp_killer_9% &7[&c%kitpvp_kills_9%&7]"
    - "&e10. &f%kitpvp_killer_10% &7[&c%kitpvp_kills_10%&7]"
```

### Example 3: Leaderboard NPC (Citizens)

Using Citizens NPC plugin:

```yaml
# Create NPC and add holograms
/npc create TopKiller
/npc hologram add &e#1 %kitpvp_killer_1%
/npc hologram add &7Kills: &c%kitpvp_kills_1%
/npc hologram add &7K/D: &a%kitpvp_kdr_1%
```

### Example 4: Chat Format

Using chat plugin that supports PlaceholderAPI:

```yaml
# chat-format.yml
format: "&7[Lv.%kitpvp_level%] &f%player_name% &7» &f%message%"
```

### Example 5: Scoreboard

Using scoreboard plugin:

```yaml
# scoreboard.yml
lines:
  - "&e&lYour Stats"
  - "&7Kills: &c%kitpvp_kills%"
  - "&7Deaths: &c%kitpvp_deaths%"
  - "&7K/D: &a%kitpvp_kdr%"
  - ""
  - "&e&lTop Killer"
  - "&f%kitpvp_killer_1%"
  - "&7Kills: &c%kitpvp_kills_1%"
```

### Example 6: TabList Header/Footer

Using TAB plugin or similar:

```yaml
# tab.yml
header:
  - ""
  - "&6&lKITPVP SERVER"
  - "&7Top Killer: &e%kitpvp_killer_1% &7(&c%kitpvp_kills_1%&7)"
  - ""

footer:
  - ""
  - "&7Your Stats: &c%kitpvp_kills%&7/&c%kitpvp_deaths% &7K/D: &a%kitpvp_kdr%"
  - ""
```

### Example 7: Advanced Stats Display

```yaml
player-stats-hologram:
  lines:
    - "&6&l━━━ &e%player_name% &6&l━━━"
    - ""
    - "&7Kills: &c%kitpvp_kills%"
    - "&7Deaths: &c%kitpvp_deaths%"
    - "&7K/D Ratio: &a%kitpvp_kdr%"
    - "&7Current Streak: &e%kitpvp_streak%"
    - "&7Best Streak: &6%kitpvp_best_streak%"
    - ""
    - "&7Level: &b%kitpvp_level% &8(&e%kitpvp_xp%&7/&e%kitpvp_required_xp% XP&8)"
    - "&7Money: &a$%kitpvp_balance%"
    - "&7Kit: &d%kitpvp_kit%"
```

## How Leaderboards Work

### Leaderboard Types

The plugin maintains separate leaderboards for:
- **Top Kills** - Players sorted by total kills
- **Top Streaks** - Players sorted by best kill streak
- **Top Levels** - Players sorted by level

### Placeholder Mapping

| Placeholder Type | Data Source |
|-----------------|-------------|
| `killer_X`, `kills_X`, `deaths_X`, `kdr_X` | Top Kills leaderboard |
| `streak_X` | Top Streaks leaderboard |
| `level_X` | Top Levels leaderboard |

### Refresh Rate

Leaderboards are automatically refreshed based on your `config.yml` settings:

```yaml
leaderboards:
  enabled: true
  entries: 10  # How many top players to track
  refresh-interval: 300  # Refresh every 5 minutes (in seconds)
```

To manually refresh leaderboards:
```
/kitpvp reload
```

## Default Behavior

- If a leaderboard position doesn't exist (e.g., only 5 players but you ask for `%kitpvp_killer_10%`):
  - **Name placeholders** return: `None`
  - **Number placeholders** return: `0`

## Configuration

### Enable/Disable Leaderboards

In `config.yml`:

```yaml
leaderboards:
  enabled: true  # Set to false to disable leaderboards
  entries: 10    # Number of top players to track (1-100)
  refresh-interval: 300  # How often to refresh (seconds)
```

### Increase Tracked Players

To show more than 10 players in leaderboards:

```yaml
leaderboards:
  entries: 50  # Track top 50 players
```

Then you can use placeholders up to `%kitpvp_killer_50%`, `%kitpvp_kills_50%`, etc.

## Troubleshooting

### Placeholders Show Raw Text

**Problem:** Placeholders display as `%kitpvp_kills%` instead of actual values

**Solution:**
1. Make sure PlaceholderAPI is installed
2. Run `/papi reload`
3. Check if expansion is registered: `/papi list`
4. You should see `kitpvp` in the list

### Leaderboard Shows "None" or "0"

**Problem:** All leaderboard placeholders show "None" or "0"

**Solutions:**
1. Check if leaderboards are enabled in `config.yml`
2. Make sure there's player data in the database
3. Wait for leaderboard refresh (default: 5 minutes)
4. Force refresh: `/kitpvp reload`

### Specific Position Shows "None"

**Problem:** `%kitpvp_killer_5%` shows "None" but `%kitpvp_killer_1%` works

**Solution:** This is normal! It means there are less than 5 players in the database. Only positions with actual players will show data.

## Complete Placeholder List

### Player Placeholders (14 total)
- `%kitpvp_kills%`
- `%kitpvp_deaths%`
- `%kitpvp_kdr%`
- `%kitpvp_streak%` / `%kitpvp_killstreak%`
- `%kitpvp_best_streak%` / `%kitpvp_beststreak%`
- `%kitpvp_level%`
- `%kitpvp_xp%`
- `%kitpvp_required_xp%` / `%kitpvp_requiredxp%`
- `%kitpvp_kit%`
- `%kitpvp_balance%` / `%kitpvp_money%`

### Leaderboard Placeholders (6 types × 100 positions each)
- `%kitpvp_killer_1%` to `%kitpvp_killer_100%`
- `%kitpvp_kills_1%` to `%kitpvp_kills_100%`
- `%kitpvp_deaths_1%` to `%kitpvp_deaths_100%`
- `%kitpvp_kdr_1%` to `%kitpvp_kdr_100%`
- `%kitpvp_streak_1%` to `%kitpvp_streak_100%`
- `%kitpvp_level_1%` to `%kitpvp_level_100%`

**Total: 614 placeholders available!**

## Summary

✅ **Comprehensive PlaceholderAPI support**  
✅ **Leaderboard placeholders for top players**  
✅ **Works with holograms, NPCs, chat, scoreboards**  
✅ **Auto-refreshing leaderboards**  
✅ **Up to 100 top players tracked**  
✅ **Player statistics placeholders**  
✅ **Easy to use syntax**  

Use these placeholders to create amazing displays of your KitPvP statistics! 🎉

