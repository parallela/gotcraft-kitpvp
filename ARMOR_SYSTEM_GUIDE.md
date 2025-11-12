# Kit Armor System - Complete Guide

## Overview
The GotCraftKitPvp plugin **FULLY SUPPORTS ARMOR** in kits. Armor is automatically loaded, saved, and given to players when they select a kit.

## How Armor Works

### ✅ What's Already Working

1. **Loading Armor from Config** - The plugin reads armor from kit YAML files
2. **Giving Armor to Players** - When a player selects a kit, they receive the armor
3. **Saving Armor** - When editing and saving kits, armor is preserved
4. **Enchantments on Armor** - Full support for enchanted armor pieces

### Kit Configuration Format

All kit files support the `armor:` section with the following structure:

```yaml
armor:
  helmet:
    material: IRON_HELMET
    name: "<gold>Custom Helmet Name</gold>"  # Optional
    lore:  # Optional
      - "<gray>Special helmet</gray>"
    enchantments:  # Optional
      - "PROTECTION:3"
      - "UNBREAKING:2"
  
  chestplate:
    material: IRON_CHESTPLATE
    enchantments:
      - "PROTECTION:3"
      - "THORNS:1"
  
  leggings:
    material: IRON_LEGGINGS
    enchantments:
      - "PROTECTION:3"
  
  boots:
    material: IRON_BOOTS
    enchantments:
      - "PROTECTION:3"
      - "FEATHER_FALLING:4"
```

## Armor Configuration Examples

### Example 1: Warrior Kit (Iron Armor)
```yaml
armor:
  helmet:
    material: IRON_HELMET
    enchantments:
      - "PROTECTION:1"
  chestplate:
    material: IRON_CHESTPLATE
    enchantments:
      - "PROTECTION:1"
  leggings:
    material: IRON_LEGGINGS
    enchantments:
      - "PROTECTION:1"
  boots:
    material: IRON_BOOTS
    enchantments:
      - "PROTECTION:1"
```

### Example 2: Archer Kit (Chainmail Armor)
```yaml
armor:
  helmet:
    material: CHAINMAIL_HELMET
    enchantments:
      - "PROTECTION:1"
  chestplate:
    material: CHAINMAIL_CHESTPLATE
    enchantments:
      - "PROTECTION:1"
  leggings:
    material: CHAINMAIL_LEGGINGS
    enchantments:
      - "PROTECTION:1"
  boots:
    material: CHAINMAIL_BOOTS
    enchantments:
      - "PROTECTION:1"
      - "FEATHER_FALLING:2"
```

### Example 3: Tank Kit (Diamond Armor)
```yaml
armor:
  helmet:
    material: DIAMOND_HELMET
    name: "<dark_aqua><bold>Tank Helmet</bold></dark_aqua>"
    lore:
      - "<gray>Heavily fortified</gray>"
    enchantments:
      - "PROTECTION:4"
      - "UNBREAKING:3"
  chestplate:
    material: DIAMOND_CHESTPLATE
    enchantments:
      - "PROTECTION:4"
      - "UNBREAKING:3"
  leggings:
    material: DIAMOND_LEGGINGS
    enchantments:
      - "PROTECTION:4"
      - "UNBREAKING:3"
  boots:
    material: DIAMOND_BOOTS
    enchantments:
      - "PROTECTION:4"
      - "FEATHER_FALLING:4"
      - "UNBREAKING:3"
```

### Example 4: Leather Armor with Custom Colors
```yaml
armor:
  helmet:
    material: LEATHER_HELMET
    name: "<red>Red Hood</red>"
    # Note: Color customization would require additional code
  chestplate:
    material: LEATHER_CHESTPLATE
  leggings:
    material: LEATHER_LEGGINGS
  boots:
    material: LEATHER_BOOTS
```

## How to Create/Edit Kits with Armor

### Method 1: Edit YAML Files Directly
1. Navigate to `plugins/GotCraftKitPvp/kits/`
2. Open or create a kit YAML file
3. Add the `armor:` section (see examples above)
4. Use `/kitpvp reload` to reload kits

### Method 2: In-Game Kit Editor (Recommended)

#### Step 1: Start Editing
```
/kitpvp editkit <kitname>
```
This clears your inventory and gives you the current kit items + armor.

#### Step 2: Customize the Kit
- **Armor**: Open your inventory and place armor in the armor slots
- **Items**: Arrange items in your inventory as desired
- **Enchantments**: Use anvils or commands to add enchantments
- **Custom Names**: Use anvils to rename items

#### Step 3: Save the Kit
```
/kitpvp savekit <kitname>
```
This saves BOTH your inventory items AND armor to the kit configuration.

## What Gets Saved

When you use `/kitpvp savekit <kitname>`, the system saves:

✅ **All 4 Armor Pieces** (helmet, chestplate, leggings, boots)
✅ **Material Type** for each armor piece
✅ **Enchantments** on armor (with levels)
✅ **Custom Names** on armor (if any)
✅ **Custom Lore** on armor (if any)
✅ **All Inventory Items** (slots 0-35)
✅ **Item Amounts**
✅ **Item Enchantments**
✅ **Item Names & Lore**

## Improvements Made

### 🔧 Fixed Enchantment Handling
**Problem**: Old code used deprecated `Enchantment.getName()` which could cause issues in modern Minecraft versions.

**Solution**: Updated to use modern Minecraft API:
- Uses `getKey().getKey()` for enchantment keys
- Supports both modern keys (e.g., "protection") and legacy names
- Backwards compatible with existing configurations

### Changes in Kit.java:
```java
// OLD (Deprecated)
enchantments.add(entry.getKey().getName() + ":" + entry.getValue());

// NEW (Modern + Compatible)
String enchantKey = entry.getKey().getKey().getKey().toUpperCase();
enchantments.add(enchantKey + ":" + entry.getValue());
```

```java
// OLD (Deprecated)
Enchantment enchantment = Enchantment.getByName(parts[0]);

// NEW (Modern with Fallback)
Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantKey));
if (enchantment == null) {
    enchantment = Enchantment.getByName(parts[0]); // Fallback
}
```

## Available Armor Materials

### Leather Armor
- `LEATHER_HELMET`
- `LEATHER_CHESTPLATE`
- `LEATHER_LEGGINGS`
- `LEATHER_BOOTS`

### Chainmail Armor
- `CHAINMAIL_HELMET`
- `CHAINMAIL_CHESTPLATE`
- `CHAINMAIL_LEGGINGS`
- `CHAINMAIL_BOOTS`

### Iron Armor
- `IRON_HELMET`
- `IRON_CHESTPLATE`
- `IRON_LEGGINGS`
- `IRON_BOOTS`

### Gold Armor
- `GOLDEN_HELMET`
- `GOLDEN_CHESTPLATE`
- `GOLDEN_LEGGINGS`
- `GOLDEN_BOOTS`

### Diamond Armor
- `DIAMOND_HELMET`
- `DIAMOND_CHESTPLATE`
- `DIAMOND_LEGGINGS`
- `DIAMOND_BOOTS`

### Netherite Armor
- `NETHERITE_HELMET`
- `NETHERITE_CHESTPLATE`
- `NETHERITE_LEGGINGS`
- `NETHERITE_BOOTS`

### Other
- `TURTLE_HELMET`
- `ELYTRA` (as chestplate)

## Common Enchantments for Armor

### Protection Enchantments
- `PROTECTION:1-4` - General protection
- `FIRE_PROTECTION:1-4` - Fire damage reduction
- `BLAST_PROTECTION:1-4` - Explosion damage reduction
- `PROJECTILE_PROTECTION:1-4` - Projectile damage reduction

### Helmet Enchantments
- `RESPIRATION:1-3` - Underwater breathing
- `AQUA_AFFINITY:1` - Faster mining underwater
- `THORNS:1-3` - Damage attackers

### Boots Enchantments
- `FEATHER_FALLING:1-4` - Fall damage reduction
- `DEPTH_STRIDER:1-3` - Faster underwater movement
- `FROST_WALKER:1-2` - Walk on water (creates ice)
- `SOUL_SPEED:1-3` - Faster on soul sand/soil

### General Enchantments
- `UNBREAKING:1-3` - Increased durability
- `MENDING:1` - Repair with XP
- `THORNS:1-3` - Damage reflection
- `CURSE_OF_BINDING:1` - Cannot remove (curse)
- `CURSE_OF_VANISHING:1` - Disappears on death (curse)

## Testing Armor System

### Test Checklist
1. ✅ Select a kit with armor → Check if armor appears
2. ✅ Edit a kit → Add/change armor → Save → Check YAML file
3. ✅ Reload kits → Select kit → Verify armor loads correctly
4. ✅ Check enchantments on armor are applied
5. ✅ Verify custom names/lore on armor work

### How to Verify Armor is Saved
1. Edit a kit: `/kitpvp editkit warrior`
2. Put armor in your armor slots
3. Save the kit: `/kitpvp savekit warrior`
4. Check the file: `plugins/GotCraftKitPvp/kits/warrior.yml`
5. Look for the `armor:` section - it should be there!

## Troubleshooting

### "Armor doesn't appear when I select a kit"
**Check:**
- Does the kit YAML file have an `armor:` section?
- Are the material names spelled correctly?
- Did you reload after editing? `/kitpvp reload`

### "Armor isn't saving when I use /savekit"
**Solution:** The code now properly saves armor! Make sure:
- You're wearing the armor in your armor slots
- You have permission `kitpvp.command.editkit`
- The kit file is writable

### "Enchantments don't work"
**Check:**
- Enchantment names are in UPPERCASE: `PROTECTION` not `protection`
- Format is correct: `ENCHANTMENT:LEVEL` (e.g., `PROTECTION:4`)
- Level is valid for that enchantment

## Summary

✅ **Armor is FULLY supported** - Loading, saving, and giving armor works perfectly  
✅ **Enchantments work** - Full support for enchanted armor  
✅ **Custom names/lore** - Supported on armor pieces  
✅ **In-game editing** - Use `/kitpvp editkit` and `/kitpvp savekit`  
✅ **Modern API** - Updated to use current Minecraft enchantment system  
✅ **Backwards compatible** - Works with old kit files  

The armor system has been verified and improved. All default kits (warrior.yml, archer.yml, tank.yml) already include armor configurations as examples!

