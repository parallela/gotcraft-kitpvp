# 1.8 Combat Mechanics Fix - November 10, 2025

## Summary

Fixed and enhanced the 1.8 combat mechanics to ensure proper attack cooldown removal on Paper 1.21.3. The implementation now correctly removes the attack cooldown bar, allows hitting while sprinting, and provides true 1.8 PvP feel.

## Issues Fixed

### 1. Attack Speed Value
**Problem:** The attack speed was set to 1024.0, which is incorrect
**Solution:** Changed to 16.0, which is the proper value for 1.8 combat (verified from old OldCombat plugin)

### 2. Attribute Constant Name
**Problem:** Used `GENERIC_ATTACK_SPEED` which doesn't exist in Paper 1.21.3
**Solution:** Changed to `ATTACK_SPEED` (correct constant for Paper 1.21+)

### 3. Non-existent Method
**Problem:** Used `player.setAttackCooldown(0)` which was removed in newer Paper versions
**Solution:** Removed this method call - the attack speed attribute of 16.0 is sufficient

### 4. Missing Respawn Reset
**Problem:** Attack speed wasn't reapplied after player respawn
**Solution:** Added attack speed reset in `DeathListener.onPlayerRespawn()`

### 5. Missing Damage Multiplier
**Problem:** Damage values didn't match 1.8 feel
**Solution:** Added optional 1.05x damage multiplier (configurable)

### 6. Can't Hit While Sprinting (NEW)
**Problem:** In 1.9+, sprinting stops when you attack. In 1.8, you can hit while sprinting
**Solution:** Restore sprint state 1 tick after attack to maintain 1.8 behavior

### 7. Kit Items Show MiniMessage Tags (NEW)
**Problem:** Kit item names/lore displayed raw MiniMessage tags like `<red>` instead of colors
**Solution:** Parse MiniMessage to legacy format for item display names and lore

### 8. ProtocolLib Scoreboard Error (NEW)
**Problem:** ProtocolLib scoreboard threw errors on Paper 1.21.3
**Solution:** Disabled ProtocolLib scoreboard - standard Bukkit scoreboard already hides numbers perfectly

## Files Modified

### 1. PlayerJoinListener.java
```java
// Changed from GENERIC_ATTACK_SPEED and 1024.0 to:
var attackSpeed = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED);
if (attackSpeed != null) {
    attackSpeed.setBaseValue(16.0);  // Proper 1.8 value
}
```

### 2. DeathListener.java
```java
// Added attack speed reset on respawn:
if (plugin.getConfig().getBoolean("combat.legacy-combat", true)) {
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
        try {
            var attackSpeed = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.setBaseValue(16.0);
            }
        } catch (Exception e) {
            // Ignore if attribute doesn't exist
        }
    }, 1L);
}
```

### 3. KitManager.java
```java
// Fixed attribute name and value when giving kits:
var attackSpeed = player.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED);

// NEW: Restore sprint after attack (1.8 behavior)
boolean wasSprinting = attacker.isSprinting();
if (wasSprinting) {
    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
        if (attacker.isOnline()) {
            attacker.setSprinting(true);
        }
    }, 1L);
}
if (attackSpeed != null) {
### 6. Kit.java (NEW)
```java
// Fixed item display to parse MiniMessage properly:
public ItemStack toItemStack() {
    // Parse MiniMessage format to legacy string for display
    String parsed = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
            .serialize(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(name));
### Sprint Preservation
- In 1.9+, the game automatically stops sprint when you attack
- In 1.8, you could hit while sprinting continuously
- Solution: Detect sprint state before hit, restore it 1 tick later
- This allows true 1.8-style combo rushing

    meta.setDisplayName(parsed);
}
```

### 7. ScoreboardManager.java (NEW)
```java
// Disabled ProtocolLib scoreboard - standard Bukkit already works:
this.useProtocolLib = false;
plugin.getLogger().info("Using standard scoreboard (numbers already hidden)");
```
### Item Display
- Kit items use MiniMessage format in config files
- Automatically parsed to legacy color codes for display
- No more visible `<red>` or `<gradient>` tags in inventory


    attackSpeed.setBaseValue(16.0);
}
```

### 4. CombatListener.java
```java
// Removed non-existent setAttackCooldown() method
// Added optional damage multiplier:
if (plugin.getConfig().getBoolean("combat.legacy-damage-multiplier", true)) {
    event.setDamage(event.getDamage() * 1.05);
}
```
- [x] Can hit while sprinting (1.8 behavior)
- [x] Kit items display colors properly (no MiniMessage tags)
- [x] Scoreboard displays without errors
- [x] Scoreboard numbers are hidden

### 5. config.yml
```yaml
# Added new configuration option:
combat:
  legacy-combat: true
  legacy-damage-multiplier: true  # NEW: 1.05x damage to emulate 1.8
  hit-delay: 10
```

## How It Works

### Attack Speed Attribute
- Setting attack speed to **16.0** removes the attack cooldown completely
- This is the standard 1.8 PvP value (vanilla 1.9+ is 4.0)
- Applied on: player join, respawn, and kit selection

### Hit Delay System
- Custom hit delay implementation using timestamps
- Prevents hits faster than configured delay (default: 10 ticks / 500ms)
- Gives the classic 1.8 PvP hit timing

### Damage Multiplier (Optional)
- Applies 1.05x multiplier to all damage
- Emulates 1.8 damage values which were slightly higher
- Can be disabled in config.yml

## Configuration

```yaml
combat:
  # Enable 1.8 combat mechanics (no attack cooldown)
  legacy-combat: true
  
  # Apply 1.05x damage multiplier to emulate 1.8 damage values
  legacy-damage-multiplier: true
  
  # Hit delay in ticks (10 ticks = 0.5 seconds)
  hit-delay: 10
```

## Testing Checklist

- [x] Attack cooldown bar removed
- [x] No weapon sweep attack
- [x] Fast clicking works like 1.8
- [x] Hit delay properly enforced
- [x] Attack speed persists through respawn
- [x] Attack speed persists when changing kits
- [x] Damage multiplier applies correctly
- [x] Can be toggled in config.yml

## Reference

This implementation is based on the original OldCombat plugin pattern:

```java
// Original working code from OldCombat plugin:
private void applyOldCombat(Player player) {
    if (player.getAttribute(Attribute.GENERIC_ATTACK_SPEED) != null) {
        player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(16.0);
    }
}

@EventHandler
public void onDamage(EntityDamageByEntityEvent e) {
    if (e.getDamager() instanceof Player player) {
        e.setDamage(e.getDamage() * 1.05);
    }
}
```

## Compatibility

- **Tested on:** Paper 1.21.3
- **Java Version:** 21
- **Attack Speed Attribute:** `org.bukkit.attribute.Attribute.ATTACK_SPEED`
- **Removed Methods:** `player.setAttackCooldown()` (doesn't exist in Paper 1.21.3)

## Notes

1. The value **16.0** is crucial - it completely removes the cooldown
2. Values lower than 16.0 will show a partial cooldown bar
3. Values higher than 16.0 have no additional effect
4. The attribute must be reapplied after respawn/kit changes
5. Hit delay system works independently of attack speed attribute

