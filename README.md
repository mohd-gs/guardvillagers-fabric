# Guard Villagers — Fabric Edition

<p align="center">
  <img src="src/main/resources/guard_villagers.png" alt="Guard Villagers Logo" width="128" height="128">
</p>

<p align="center">
  <strong>Armed village protectors for Minecraft — now on Fabric!</strong>
</p>

<p align="center">
  <a href="https://github.com/mohd-gs/guardvillagers-fabric"><img src="https://img.shields.io/badge/Minecraft-26.1.2-green" alt="MC Version"></a>
  <img src="https://img.shields.io/badge/Fabric-0.19.3-orange" alt="Fabric">
  <img src="https://img.shields.io/badge/Java-25-blue" alt="Java">
  <img src="https://img.shields.io/badge/Version-3.2.0-brightgreen" alt="Version">
</p>

---

## What is Guard Villagers?

**Guard Villagers** adds armed guards to Minecraft villages. These NPC defenders spawn naturally in villages and protect villagers, iron golems, and players from hostile mobs. They come with a deep combat AI system, weapon specialization, ranking progression, and dozens of configurable options.

Originally created by **TallestEgg** for NeoForge, this is the complete **Fabric port** rebuilt from the ground up for **Minecraft 26.1.2** with 10 new features, critical bug fixes, and performance optimizations.

---

## Table of Contents

- [Core Mechanics](#core-mechanics)
- [Recruiting Guards](#recruiting-guards)
- [Combat System](#combat-system)
- [New Features (v3.2.0)](#new-features-v320)
- [Interaction Guide](#interaction-guide)
- [Configuration](#configuration)
- [Performance Optimizations](#performance-optimizations)
- [Bug Fixes](#bug-fixes)
- [Technical Details](#technical-details)
- [Credits & License](#credits--license)

---

## Core Mechanics

### Guard Entity

Guards are a new mob type that spawns naturally in villages. Each guard has:

- **6-slot inventory**: Helmet, Chestplate, Leggings, Boots, Offhand, Mainhand
- **Biome-based variants**: Desert, Taiga, Jungle, Plains, Snow, Swamp, and more — guards visually match the village biome they spawn in
- **Gossip & Reputation system**: Uses vanilla gossip mechanics. Your reputation determines whether guards follow you, let you equip them, or attack you
- **Health regeneration**: Guards slowly regenerate health over time (configurable amount and interval)
- **Zombie conversion**: On Normal/Hard difficulty, guards killed by zombies convert into zombie villagers instead of dying — they can be cured back

### Village Defense

Guards are the backbone of village security. They automatically:

- Target and attack raiders, zombies, witches, and all hostile mobs
- Share combat targets with nearby idle guards (coordination fix)
- Defend villagers and iron golems when those are targeted or hurt by enemies
- Receive alerts when a hostile mob targets a villager — even guards far from the action respond

### Patrol System

Guards patrol the village to keep it safe:

- **Workstation patrol**: Guards walk between village workstations (default behavior)
- **Village path patrol**: Guards move through village paths (configurable)
- **Bell rally**: Right-click a bell with Hero of the Village to toggle follow mode on all nearby guards
- **Patrol points**: Bells can be used to set patrol locations

### Villager Support

Village professionals help maintain guards:

| Professional | Service |
|---|---|
| **Cleric** | Heals wounded guards (up to 3 per cleric) |
| **Armorer** | Repairs guard armor and iron golems |
| **Weaponsmith** | Repairs guard weapons and iron golems |
| **Toolsmith** | Repairs guard equipment |

---

## Recruiting Guards

### Converting a Villager

Any adult villager with a convertible profession can become a guard. By default, only **Nitwits** and **Unemployed** villagers are eligible (configurable).

**How to convert:**

1. Hold a **weapon** in your main hand — any of these work:
   - ⚔️ Sword (any tier)
   - 🪓 Axe (any tier)
   - 🏹 Bow
   - 🏹 Crossbow
   - 🔱 Trident
2. **Sneak** (crouch) and **right-click** the villager
3. The villager transforms into a guard with the weapon you were holding
4. The guard retains its biome variant and your reputation carries over

> **Note**: You may need Hero of the Village effect if the config option `ConvertVillagerIfHaveHOTV` is enabled.

### Natural Spawning

Guards spawn naturally in villages with a default count of **6 per village** (configurable). They also spawn in specific structure pools like the iron golem cage in village centers.

---

## Combat System

### Weapon Specialization

Guards adapt their combat behavior based on their weapon type. This system is enabled by default and makes each guard role unique:

| Weapon Type | Behavior |
|---|---|
| 🏹 **Bow** | Fires arrows at enemies. Automatically retreats (kites) when enemies get too close, maintaining optimal distance while shooting |
| 🏹 **Crossbow** | Charges and fires crossbow bolts. Retreats when enemies approach, repositions before recharging |
| 🔱 **Trident** | Throws tridents at range, retreats from close enemies |
| ⚔️ **Sword** | Standard melee combat, fights aggressively at close range |
| 🪓 **Axe** | Melee combat with a **Berserker damage bonus** (configurable). Shield guards hold defensive formation |
| 🔨 **Mace** | High-tier melee weapon (MC 26.1.2), comparable to diamond sword in tier |

### Shield Combat

Guards use shields to block incoming attacks:

- Shields are raised automatically when under attack
- **Kick attack**: Guards can kick nearby enemies, dealing knockback and potentially disarming them
- Shields can be disabled by axe attacks (same mechanic as player shields)
- Configurable option: `GuardRaiseShield` makes guards permanently hold shields up

### Friendly Fire Prevention

Guards will **not** shoot if a friendly entity (villager, iron golem, or other guard) is in their line of fire:

- Uses a dot-product angle check with configurable sensitivity
- When a friendly is detected, the guard stops drawing their bow and waits
- **v3.2.0 fix**: A 2-second cooldown prevents the draw-cancel-draw freeze loop that previously caused archers to become stuck

---

## New Features (v3.2.0)

### 1. Guard Leveling & Ranking System

Guards level up by killing enemies. As they accumulate kills, they advance through four ranks with permanent stat bonuses:

| Rank | Kills Required | Health Bonus | Damage Bonus |
|------|---------------|-------------|-------------|
| **Recruit** | 0 | — | — |
| **Soldier** | 5 | +4 HP | +0.5 |
| **Veteran** | 15 | +8 HP | +1.0 |
| **Captain** | 30 | +12 HP | +2.0 |

- Rank modifiers persist through server restarts and chunk reloads
- Optional rank display in name tags (client config: `showRankInName`)
- Fully configurable thresholds and bonus values

### 2. Weapon Specialization (Enhanced)

Guards now behave differently based on their weapon:

- **Archers** (bow/crossbow/trident): Automatically retreat when enemies get within configurable distance. Bow guards can now **shoot while retreating** (kiting behavior) instead of stopping to run away
- **Melee guards** (sword/axe): Standard close-range combat
- **Berserker bonus**: Guards wielding axes receive a configurable damage bonus
- **Shield guards**: Guards with shields and non-axe weapons form defensive formations around other guards

### 3. War Horn

A craftable item that rallies all guards in a large radius:

- Plays a raid horn sound audible across the village
- All guards within range (default 128 blocks) enter a **combat stance** for 30 seconds
- Idle guards are immediately assigned the nearest enemy target
- Guards with no nearby enemy navigate toward the horn-blower
- Has 50 durability — takes damage with each use
- **Recipe**: 3 iron ingots + 2 string + 1 goat horn

### 4. Archer Freeze Fix (Critical)

Previously, archers would get stuck in an infinite draw-cancel cycle when a friendly entity was in their line of fire. The `friendlyInLineOfSight()` check only ran every 5 ticks and returned `false` on other ticks, causing the bow goal to immediately re-draw after being stopped.

**Fix**: The friendly fire check result is now **cached per guard** and persists across ticks. When a friendly is detected, a **40-tick cooldown** prevents re-drawing, ensuring the guard waits before attempting another shot.

### 5. Enhanced Mount System

Guards can automatically mount nearby horses when idle:

- Guards seek out unclaimed horses within 8 blocks
- Mounted guards receive a speed compensator so they don't move slower than on foot
- **Mounted knockback bonus**: 1.3x knockback while on horseback (configurable)
- Mounting is disabled during active combat
- 10-second cooldown between mount attempts
- Speed compensator is properly removed when dismounting

### 6. Wounded Behavior

When a guard's health drops below a threshold (default 25%):

- The guard enters a **wounded** state and retreats from combat
- Movement speed is reduced by 30%
- The guard navigates toward the nearest villager for healing
- Recovery occurs when health rises above the recovery threshold (default 40%)
- Wounded guards will still defend themselves if cornered but won't seek out enemies

### 7. Equipment Pickup Fix (Critical)

Previously, guards could **never** pick up better equipment because the tier comparison used `rarity.ordinal()` — which returns 0 for ALL common-rarity items. Leather, iron, and diamond armor were all "equal" at tier 0.

**Fix**: Equipment tier is now determined by **registry name mapping**:

| Tier | Armor | Weapons |
|------|-------|---------|
| 6 | Netherite | Netherite Sword/Axe |
| 5 | Diamond | Diamond Sword/Axe, Trident, Mace |
| 4 | Iron, Copper | Iron/Copper Sword/Axe, Crossbow |
| 3 | Chainmail | Stone Sword/Axe, Bow |
| 2 | Gold | Golden Sword/Axe |
| 1 | Leather | Wooden Sword/Axe |

Additional improvements:
- **Shields**: Guards ALWAYS pick up shields, even dropping food from offhand to equip a shield
- **Food**: Guards pick up food with higher nutrition values, swapping with lower-quality food
- **Swords**: Detected by registry name since `SwordItem` class was removed in MC 26.1.2
- **Mace**: New MC 26.1.2 weapon type, ranked at tier 5 (diamond equivalent)
- Uses `setItemSlot()` instead of direct inventory manipulation for proper visual sync

### 8. Night Watch

Guards become more vigilant during nighttime:

- Follow range is multiplied by a configurable factor (default 2x) at night
- Guards automatically transition between day patrol and night watch
- Extended range allows guards to detect and engage threats from farther away
- Returns to normal range at dawn

### 9. Auto Equipment Upgrade

Guards automatically pick up better equipment from the ground:

- Scans for dropped items within configurable range (default 3 blocks)
- Compares weapon tiers and equips upgrades
- Compares armor tiers and swaps for better pieces
- Always picks up shields (even if offhand has food)
- Picks up food (swaps if current food has lower nutrition)
- Old equipment is dropped on the ground for other guards or players
- Disabled during active combat
- 1-second cooldown after successful pickup, 2-second cooldown after failed scan

### 10. Food Sharing Between Guards

Guards with food in their offhand will share with wounded comrades:

| Parameter | Value |
|---|---|
| Share range | 10 blocks |
| Target condition | Health below 50% AND no food in offhand |
| Share amount | Half of stack (rounded down) |
| Cooldown | 5 minutes per guard |
| Combat restriction | Does NOT activate during combat |
| Target selection | Closest wounded guard only |

**How it works**: A guard with food (count ≥ 2) checks for nearby wounded guards. If found, it drops half its food near the wounded guard, who picks it up automatically. Each guard helps only one other guard per cycle, and waits 5 minutes before sharing again.

---

## Interaction Guide

| Action | How |
|--------|-----|
| **Open guard inventory** | Right-click with empty hand (requires reputation or Hero of the Village) |
| **Toggle follow mode** | Right-click a guard |
| **Mass follow (all nearby)** | Right-click a bell with Hero of the Village |
| **Set patrol point** | Right-click a bell |
| **Give equipment** | Place items in guard inventory slots |
| **Convert villager** | Sneak + right-click villager while holding a weapon |
| **Use War Horn** | Right-click to rally all guards in range |
| **Feed a guard** | Give food via offhand inventory slot |

### Guard Inventory Layout

```
┌─────────┬─────────────┬─────────┐
│ Helmet  │  Chestplate │ Leggings│
├─────────┼─────────────┼─────────┤
│  Boots  │   Offhand   │ Mainhand│
│         │(Shield/Food)│(Weapon) │
└─────────┴─────────────┴─────────┘
  Slot 0     Slot 4      Slot 5
```

---

## Configuration

The config file is located at `config/guardvillagers.json`. It is divided into three sections.

### Common Config

#### Raids & Illagers

| Option | Default | Description |
|--------|---------|-------------|
| `RaidAnimals` | `true` | Raiders attack farm animals during raids |
| `WitchesVillager` | `true` | Witches target villagers |
| `IllagersRunFromPolarBears` | `true` | Illagers flee from polar bears |

#### Mob AI

| Option | Default | Description |
|--------|---------|-------------|
| `AttackAllMobs` | `true` | Guards attack all hostile mobs (not just raiders/zombies) |
| `MobsAttackGuards` | `false` | All hostile mobs target guards |
| `MobBlackList` | *(see defaults)* | Mobs guards will never attack (villagers, golems, creepers, endermen) |
| `MobWhiteList` | *empty* | Additional mobs for guards to attack |

#### Village & Spawning

| Option | Default | Description |
|--------|---------|-------------|
| `guardSpawnInVillage` | `6` | How many guards spawn per village |
| `GuardFormation` | `true` | Shield guards stay close together in formation |
| `guardPatrolVillageAi` | `false` | Guards patrol village paths |
| `guardPatrolAroundVillageWorkstations` | `true` | Guards patrol near workstations |
| `structuresThatSpawnGuards` | *(see defaults)* | Structure pools that include guard spawns |

#### Guard Behavior

| Option | Default | Description |
|--------|---------|-------------|
| `convertGuardOnDeath` | `true` | Guards convert to zombie villagers when killed by zombies |
| `GuardsOpenDoors` | `true` | Guards can open doors |
| `GuardRaiseShield` | `false` | Guards permanently raise shields (instead of reactively) |
| `GuardVillagerHelpRange` | `50.0` | Range for guards to help each other |
| `amountOfHealthRegenerated` | `1.0` | Health regenerated per interval |
| `reputationRequirement` | `15` | Min reputation to access guard inventory |
| `followHero` | `true` | Guards only follow with Hero of the Village |
| `guardTeleport` | `true` | Following guards teleport to owner if too far |
| `friendlyFireCheckValue` | `-0.9` | Angle threshold for friendly fire detection |
| `FriendlyFire` | `true` | Whether guards avoid shooting friendlies |
| `guardArrowsHurtVillagers` | `true` | Whether stray arrows can hit villagers |

#### Leveling System

| Option | Default | Description |
|--------|---------|-------------|
| `guardLeveling` | `true` | Enable the ranking system |
| `killsForSoldier` | `5` | Kills needed for Soldier rank |
| `killsForVeteran` | `15` | Kills needed for Veteran rank |
| `killsForCaptain` | `30` | Kills needed for Captain rank |
| `soldierHealthBonus` | `4.0` | Extra HP for Soldier |
| `soldierDamageBonus` | `0.5` | Extra damage for Soldier |
| `veteranHealthBonus` | `8.0` | Extra HP for Veteran |
| `veteranDamageBonus` | `1.0` | Extra damage for Veteran |
| `captainHealthBonus` | `12.0` | Extra HP for Captain |
| `captainDamageBonus` | `2.0` | Extra damage for Captain |

#### Weapon Specialization

| Option | Default | Description |
|--------|---------|-------------|
| `weaponSpecialization` | `true` | Enable weapon-based behavior |
| `archerRetreatDistance` | `3.0` | Distance at which archers retreat from enemies |
| `berserkerDamageBonus` | `0.2` | Extra damage for axe-wielders (Berserker bonus) |

#### War Horn

| Option | Default | Description |
|--------|---------|-------------|
| `warHornRange` | `128.0` | Horn effective radius (blocks) |
| `warHornCombatDurationSeconds` | `30` | Combat stance duration after horn blow |

#### Mounts

| Option | Default | Description |
|--------|---------|-------------|
| `guardsAutoMountHorses` | `true` | Guards auto-mount nearby horses when idle |
| `mountedKnockbackBonus` | `1.3` | Knockback multiplier while mounted |

#### Wounded Behavior

| Option | Default | Description |
|--------|---------|-------------|
| `woundedBehavior` | `true` | Enable wounded retreat behavior |
| `woundedHealthThreshold` | `0.25` | Health fraction to become wounded (25%) |
| `recoveredHealthThreshold` | `0.40` | Health fraction to recover (40%) |

#### Night Watch

| Option | Default | Description |
|--------|---------|-------------|
| `nightWatchEnabled` | `true` | Enable night watch mode |
| `nightFollowRangeMultiplier` | `2.0` | Follow range multiplier at night |

#### Equipment

| Option | Default | Description |
|--------|---------|-------------|
| `autoEquipmentUpgrade` | `true` | Guards auto-pickup better gear from ground |
| `equipmentPickupRange` | `3.0` | Item pickup scan range (blocks) |

### Client Config

| Option | Default | Description |
|--------|---------|-------------|
| `GuardSteve` | `false` | Use Steve model for guards instead of villager model |
| `bigHeadBabyVillager` | `true` | Baby villagers have big heads (Bedrock style) |
| `guardInventoryNumbers` | `true` | Show stack size numbers in guard inventory |
| `showRankInName` | `true` | Display rank title in guard name tags |

### Startup Config

> **Note**: Startup config options require a game restart to take effect.

| Option | Default | Description |
|--------|---------|-------------|
| `healthModifier` | `20.0` | Base guard max health |
| `speedModifier` | `0.5` | Base guard movement speed |
| `followRangeModifier` | `20.0` | Base guard follow range |

---

## Performance Optimizations

Guard Villagers v3.2.0 includes extensive performance optimizations for large villages with 50+ guards:

| Optimization | Before | After |
|---|---|---|
| Owner lookup (`getOwner()`) | Called `getPlayerByUUID()` 3x per invocation | Cached for 5 seconds (100 ticks) |
| Wounded check | Ran every tick | Cached, refreshed every 200 ticks |
| Night watch check | Ran every tick | Cached, refreshed every 200 ticks |
| Equipment scan | Every 80 ticks | Every 60 ticks (more responsive) with 1-sec cooldown |
| Target sharing scan | Every 40 ticks, 32-block range | Every 80 ticks, 20-block range cap, max 5 guards alerted |
| Friendly fire check | Returned `false` on non-check ticks (caused archer freeze) | Cached result per guard, persists across ticks |
| Mob set-target event | Every target change triggered AABB scan | 5-second cooldown per mob, 24-block range cap |
| Blacklist/whitelist lookup | `List.contains()` O(n) | `HashSet.contains()` O(1) |
| Guard help scan | Unlimited guards alerted | Max 5 guards per scan cycle |

---

## Bug Fixes

### Critical Fixes (v3.2.0)

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| **Archer freeze** — bow guards repeatedly draw and cancel without shooting | `friendlyInLineOfSight()` returned `false` on 4/5 ticks, causing bow goal to re-draw immediately after being stopped | Cache friendly fire result per guard; add 40-tick cooldown after canceling draw |
| **Equipment pickup broken** — guards never pick up better armor/weapons | `getArmorDefense()` used `rarity.ordinal()` which is 0 for ALL common armor (leather, iron, diamond) | Registry-name-based tier mapping with correct vanilla tier ranking |
| **Sword detection missing** — guards couldn't detect swords as weapons | `SwordItem` class removed in MC 26.1.2 | Detect swords by registry name suffix `_sword` |

### Fixes (v3.1.0)

| Bug | Fix |
|-----|-----|
| **Guard coordination** — most guards stand idle while a few fight | Added `GuardHelpNearbyGuardGoal` for target sharing between guards |
| **Attack target mismatch** — `doHurtTarget()` used stale target | Method now properly uses passed `target` parameter |
| **Rank persistence** — bonuses lost after restart | `applyRankModifiers()` called in `readAdditionalSaveData()` |

---

## Technical Details

### Requirements

| Dependency | Version |
|---|---|
| Minecraft | 26.1.2 |
| Fabric Loader | >= 0.17.0 |
| Fabric API | Compatible version |
| Java | 25+ |

### Build Setup

```bash
# Clone the repository
git clone https://github.com/mohd-gs/guardvillagers-fabric.git
cd guardvillagers-fabric

# Build the mod
./gradlew build

# Output JAR is in build/libs/
```

### Architecture

```
src/main/java/tallestegg/guardvillagers/
├── GuardVillagers.java               # Main mod initializer
├── GuardEntityType.java              # Entity type registration
├── GuardItems.java                    # Item registration (spawn eggs, war horn)
├── GuardSounds.java                   # Sound event registration
├── GuardStats.java                    # Stat registration
├── GuardDataAttachments.java          # Data attachment cleanup
├── GuardVillagerTags.java             # Tag definitions
├── HandlerEvents.java                 # Event handlers (interact, entity load, mob target)
├── configuration/
│   └── GuardConfig.java               # JSON-based configuration system
├── common/
│   ├── entities/
│   │   ├── Guard.java                 # Main guard entity (2400+ lines)
│   │   ├── GuardContainer.java        # Inventory container
│   │   └── ai/
│   │       ├── goals/                 # Behavior goals
│   │       │   ├── GuardRetreatGoal.java           # Crossbow/trident retreat
│   │       │   ├── GuardMountHorseGoal.java         # Auto-mount horses
│   │       │   ├── PickupBetterEquipmentGoal.java   # Auto equipment upgrade + shield/food
│   │       │   ├── GuardHelpNearbyGuardGoal.java    # Target sharing (coordination)
│   │       │   ├── GuardShareFoodGoal.java          # Food sharing with wounded guards
│   │       │   ├── AttackEntityDaytimeGoal.java     # Daytime attack behavior
│   │       │   ├── GetOutOfWaterGoal.java           # Water escape behavior
│   │       │   └── GolemFloatWaterGoal.java         # Golem water float
│   │       └── tasks/                 # Villager brain tasks
│   │           ├── HealGuardAndHero.java
│   │           ├── RepairGolem.java
│   │           ├── RepairGuardEquipment.java
│   │           ├── ShareGossipWithGuard.java
│   │           └── VillagerHelp.java
│   └── items/
│       └── WarHornItem.java           # War Horn item logic
├── client/
│   ├── GuardClientEvents.java         # Client-side event registration
│   ├── models/                        # Entity models (Villager + Steve variants)
│   ├── renderer/                      # Entity renderer
│   ├── gui/                           # Guard inventory screen
│   └── network/                       # Client packet handler
├── networking/                        # Server-side networking packets
├── mixins/                            # Mixin classes
│   ├── SinglePoolElementMixin.java              # Structure guard spawning
│   ├── DefendVillageGoalGolemMixin.java         # Golem defense coordination
│   ├── VillagerGoalPackagesMixin.java           # Villager brain task injection
│   └── MobSetTargetMixin.java                   # Target event interception
└── loot_tables/
    ├── GuardLootTables.java           # Custom loot table registration
    └── functions/
        └── ArmorSlotFunction.java     # Armor slot logic
```

### Key Technical Notes for MC 26.1.2

This mod targets **Minecraft 26.1.2**, the first fully unobfuscated version, which uses **Mojang Official Mappings** (not Yarn). Key API differences from older versions:

- **Registry**: Uses `Registry.register()` with `BuiltInRegistries` and `ResourceKey`
- **Item Registration**: `Item.Properties.setId(ResourceKey)` must be called before the item constructor
- **Entity Data Serialization**: Uses `ValueInput`/`ValueOutput` instead of `CompoundTag`
- **Gossip Container**: CODEC-based serialization with `GossipContainer.CODEC`
- **SwordItem removed**: Swords are plain `Item` instances in 26.1.2, detected by registry name
- **Access Widener**: Declared in `fabric.mod.json`, uses `official` mapping namespace
- **Entity ID Lookup**: `Entity.getEncodeId()` is protected — use `BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()`

---

## Supported Languages

| Language | Code |
|----------|------|
| English | `en_us` |
| Chinese (Simplified) | `zh_cn` |
| Chinese (Traditional) | `zh_tw` |
| German | `de_de` |
| Spanish | `es_es` |
| French | `fr_fr` |
| Italian | `it_it` |
| Japanese | `ja_jp` |
| Korean | `ko_kr` |
| Polish | `pl_pl` |
| Portuguese (BR) | `pt_br` |
| Romanian | `ro_ro` |
| Russian | `ru_ru` |
| Turkish | `tr_tr` |
| Ukrainian | `uk_ua` |
| Vietnamese | `vi_vn` |
| Czech | `cs_cz` |
| Filipino | `fil_ph` |
| Spanish (MX) | `es_mx` |

---

## Credits

- **TallestEgg** — Original mod author (NeoForge version)
- **HadeZ / SadNya69** — Texture art
- **Fabric port** — Maintained for Minecraft 26.1.2

## License

- **Code**: MIT License
- **Assets**: All Rights Reserved
