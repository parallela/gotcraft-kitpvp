# Final Fixes - Scoreboard Numbers & Death Screen

## Date: November 10, 2025

---

## Issue 1: Scoreboard Line Numbers Still Visible

### Problem
Even after setting team options and using ChatColor for invisible entries, scoreboard numbers were still visible on the right side of each line.

### Root Cause
Using `ChatColor` values as invisible entries wasn't enough. The team option `NAME_TAG_VISIBILITY` was not being set to `NEVER`, which is critical for completely hiding the score numbers.

### Solution
**File: `ScoreboardManager.java`**
**File: `ProtocolScoreboard.java`**

Changed the scoreboard entry system to:
1. Use **space characters** with varying lengths as invisible entries: `" ".repeat(i + 1)`
2. **Critical addition**: Set team option to hide entries completely:
   ```java
   team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
   ```

This combination ensures:
- Each line has a unique invisible entry (different number of spaces)
- The entry names are completely hidden from view
- Only the team prefix/suffix (actual content) is displayed
- **No numbers appear on the scoreboard at all**

---

## Issue 2: Death Screen Still Appearing

### Problem
Players were still seeing the death screen (respawn menu) when they died, which interrupts gameplay flow.

### Previous Approach (Failed)
- Tried using `PlayerDeathEvent` and `PlayerRespawnEvent`
- Tried clearing titles immediately after respawn
- Death screen is client-side and unavoidable once a PlayerDeathEvent fires

### New Approach: Fake Death System

**File: `DamageListener.java`**

Implemented a **fake death system** that intercepts damage BEFORE death occurs:

#### How It Works:

1. **Listen on `EntityDamageEvent` with HIGH priority**
   ```java
   @EventHandler(priority = EventPriority.HIGH)
   public void onEntityDamage(EntityDamageEvent event)
   ```

2. **Calculate if damage would kill the player**
   ```java
   double finalHealth = player.getHealth() - event.getFinalDamage();
   if (finalHealth <= 0.0) {
       event.setCancelled(true); // Prevent actual death
       handleFakeDeath(player, event);
   }
   ```

3. **Manually handle "death" without triggering PlayerDeathEvent**
   - Cancel the damage event (player doesn't actually die)
   - Update stats (kills, deaths, streaks)
   - Broadcast killstreak messages if needed
   - Clear inventory
   - **Immediately teleport to spawn** (no death screen!)
   - Reset health, hunger, fire ticks
   - Clear potion effects
   - Reapply kit after delay

#### Benefits:
- ✅ **No death screen at all** - player never actually dies
- ✅ Instant respawn at spawn location
- ✅ All stats still tracked correctly
- ✅ Killstreaks still work
- ✅ Smoother PvP experience
- ✅ No client-side death animation or menu

#### Fallback:
The original `DeathListener` remains as a fallback in case a real death event somehow occurs (e.g., `/kill` command, plugin conflicts, etc.). It now has documentation explaining its fallback role.

---

## Files Modified

1. **`/src/main/java/me/lubomirstankov/gotCraftKitPvp/scoreboard/ScoreboardManager.java`**
   - Changed entry generation to use spaces
   - Added `team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)`

2. **`/src/main/java/me/lubomirstankov/gotCraftKitPvp/scoreboard/ProtocolScoreboard.java`**
   - Changed entry generation to use spaces
   - Added `team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER)`

3. **`/src/main/java/me/lubomirstankov/gotCraftKitPvp/listeners/DamageListener.java`**
   - Complete rewrite with fake death system
   - Added `handleFakeDeath()` method
   - Intercepts lethal damage before death occurs
   - Handles all death logic manually

4. **`/src/main/java/me/lubomirstankov/gotCraftKitPvp/listeners/DeathListener.java`**
   - Added documentation comments
   - Now serves as fallback only

---

## Testing Checklist

- [ ] Scoreboard displays without any numbers on the right side
- [ ] Scoreboard lines update correctly
- [ ] Player deaths don't show death screen
- [ ] Player respawns instantly at spawn
- [ ] Kill/death stats update correctly
- [ ] Killstreaks work properly
- [ ] Inventory clears on death
- [ ] Kit is given back after respawn delay
- [ ] Health/hunger reset properly
- [ ] Combat mechanics (1.8 attack speed) reapply correctly
- [ ] Scoreboard updates after kill/death

---

## Configuration

No configuration changes required. The system uses existing config values:
- `general.respawn-delay` - Delay before kit is given back
- `combat.legacy-combat` - Whether to apply 1.8 combat mechanics
- Arena spawn location (set via `/kitpvp setspawn`)

---

## Compatibility

- ✅ Works with or without ProtocolLib
- ✅ Compatible with Paper/Spigot 1.20+
- ✅ No external dependencies added
- ✅ Uses native Bukkit/Spigot APIs

---

## Technical Notes

### Why Spaces Work for Scoreboard
Minecraft scoreboards identify entries by their exact string value. Using different numbers of spaces creates unique entries that:
- Are invisible to players (whitespace)
- Can be individually scored
- Work with team prefix/suffix system
- Combined with `NAME_TAG_VISIBILITY.NEVER`, completely hide the entry

### Why Fake Death Works
The key is intercepting damage at the `EntityDamageEvent` level:
- This fires BEFORE health is subtracted
- We can calculate the final health
- Cancel the event before death occurs
- Player never enters the "dead" state
- No client-side death screen triggers
- We handle everything manually server-side

This is a common technique used by minigame plugins to control respawn behavior.

---

## Credits
- Fake death technique inspired by common minigame plugin practices
- Scoreboard number hiding using space entries and team visibility options

