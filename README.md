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
  <img src="https://img.shields.io/badge/Version-4.0.0-brightgreen" alt="Version">
</p>

---

## What is Guard Villagers?

**Guard Villagers** adds armed guards to Minecraft villages. These NPC defenders spawn naturally in villages and protect villagers, iron golems, and players from hostile mobs. They come with a deep combat AI system, weapon specialization, military formations, smart targeting, ranking progression, and dozens of configurable options.

Originally created by **TallestEgg** for NeoForge, this is the complete **Fabric port** rebuilt from the ground up for **Minecraft 26.1.2** with major new features including weapon-specific combat behavior, tactical formations, flanking AI, squad system, creeper avoidance, and critical bug fixes.

---

## Table of Contents

- [Core Mechanics](#core-mechanics)
- [Recruiting Guards](#recruiting-guards)
- [Combat System](#combat-system)
- [What's New in v4.0](#whats-new-in-v40)
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
- Share combat targets with nearby idle guards (coordination system)
- Defend villagers and iron golems when those are targeted or hurt by enemies
- Receive alerts when a hostile mob targets a villager — even guards far from the action respond
- **Smart target prioritization**: Guards evaluate threat levels and pick the most dangerous enemy, not just the nearest one (v4.0)
- **Anti-creeper behavior**: Guards detect and flee from charging creepers instead of standing next to them and getting blown up (v4.0)

### Patrol System

Guards patrol the village to keep it safe:

- **Workstation patrol**: Guards walk between village workstations (default behavior)
- **Village path patrol**: Guards move through village paths (configurable)
- **Patrol Flag item**: New craftable item that lets players define custom waypoint routes for individual guards — right-click blocks to add waypoints, then right-click a guard to assign the route (v4.0)
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
   - Sword (any tier)
   - Axe (any tier)
   - Bow
   - Crossbow
   - Trident
   - Mace (MC 26.1.2)
2. **Sneak** (crouch) and **right-click** the villager
3. The villager transforms into a guard with the weapon you were holding
4. The guard retains its biome variant and your reputation carries over

> **Note**: You may need Hero of the Village effect if the config option `ConvertVillagerIfHaveHOTV` is enabled.

### Natural Spawning

Guards spawn naturally in villages with a default count of **6 per village** (configurable). They also spawn in specific structure pools like the iron golem cage in village centers.

---

## Combat System

### Weapon Specialization

Guards adapt their combat behavior based on their weapon type. The v4.0 `WeaponBehavior` system makes each weapon role unique with distinct attack speeds, ranges, and tactical behaviors:

| Weapon Type | Attack Speed | Optimal Range | Special Behavior |
|---|---|---|---|
| **Sword** | Fast (0.75s) | 1.8 blocks | Parry between swings, circles enemy, 50% chance to flank |
| **Axe** | Slow (1.5s) | 2.2 blocks | Berserker bonus, breaks shields, always flanks for rear attacks |
| **Mace** | Medium-slow (1.25s) | 2.0 blocks | Aggressive charge, bonus vs armored targets, knockback specialist |
| **Trident** | Medium (1.1s) | 3.0 blocks | Hybrid ranged+melee, extended reach, prioritizes mounted enemies |
| **Bow** | N/A (bow goal) | 12.0 blocks | Kiting, retreats from close enemies, counter-snipes ranged foes |
| **Crossbow** | N/A (crossbow goal) | 10.0 blocks | Powerful shots, stays at medium range |

### Weapon-Specific Damage Bonuses

| Weapon | Bonus | Config Option |
|---|---|---|
| Axe (Berserker) | +0.2 damage | `berserkerDamageBonus` |
| Axe vs Shield | +0.5 damage, 5s shield disable | `axeVsShieldBonus`, `axeShieldDisableSeconds` |
| Spear/Trident vs Mounted | +1.0 damage | `spearVsMountedBonus` |
| Mace | +0.25 damage | `maceDamageBonus` |
| Cavalry Charge (mounted) | +0.5 damage | `cavalryChargeDamageBonus` |

### Shield Combat

Guards use shields to block incoming attacks:

- Shields are raised automatically when under attack
- **Kick attack**: Guards can kick nearby enemies, dealing knockback and potentially disarming them
- Shields can be disabled by axe attacks (same mechanic as player shields)
- Configurable option: `GuardRaiseShield` makes guards permanently hold shields up
- **Shield between attacks**: Sword and spear guards raise shields between swings for protection; berserker axes do not

### Friendly Fire Prevention (Dual-Layer)

Guards will **not** harm villagers, iron golems, or other guards when friendly fire is disabled. v4.0 uses a dual-layer protection system:

1. **Pre-damage mixin** (`LivingEntityDamageMixin`): Cancels the damage event entirely before it applies — zero health lost
2. **Post-damage mixin** (`LivingEntityHurtPostMixin`): If damage somehow slips through (e.g., indirect sources), the health is restored immediately

This replaces the previous single-layer approach that sometimes let arrow damage through during lag spikes.

---

## What's New in v4.0

### 1. Weapon Behavior System (`WeaponBehavior.java`)

Every weapon type now has unique combat characteristics that go beyond simple damage bonuses. The system controls attack cooldown, optimal combat distance, minimum engagement distance, movement speed modifier, strafing behavior, hit-and-run tactics, and shield usage patterns. Guards wielding swords fight with fast footwork and parry between swings; axe berserkers charge aggressively and target shielded enemies; spear guards maintain extended reach and perform hit-and-run stabs; mace wielders smash through armor with knockback. This is enabled by the `weaponSpecificBehavior` config option and affects all melee and ranged goals.

### 2. Military Formation System (`GuardFormationGoal.java`)

When 3 or more guards are nearby, they automatically organize into tactical formations based on the weapon composition of the group:

| Formation | Trigger | Layout |
|---|---|---|
| **Shield Wall** | 2+ shields + 1+ ranged | Shield bearers form front line, ranged behind |
| **Phalanx** | 2+ shields + 1+ spear/trident | Shield wall with spears attacking over shoulders |
| **Arrow Line** | 2+ ranged guards | Ranged in horizontal line, melee on flanks |
| **Wedge** | 2+ berserker axes | V-shape charge formation with berserkers at tip |
| **Skirmish** | Default/fallback | Loose spread in an arc |

Formations face the enemy direction, maintain configurable spacing, and dissolve when enemies get within 3 blocks (too close for positioning to matter). The formation leader is the highest-rank guard. Ground validation ensures guards don't try to stand on air or inside blocks. Controlled by `GuardFormation`, `formationRange`, and `formationSpacing` config options.

### 3. Flanking AI (`GuardFlankingGoal.java`)

Instead of all guards rushing directly at the enemy, some guards now circle around to attack from the sides or behind:

- **Sword guards**: 50% chance to flank (others hold the line)
- **Berserker axes**: Always try to flank for devastating rear attacks
- **Spear guards**: 60% chance to circle at extended range for openings
- Ranged guards reposition to better angles (not true flanking)

Flanking requires at least one other guard engaging from the front. Guards calculate positions based on the enemy's facing direction and navigate to the side/behind before attacking. This prevents guards from bunching up and blocking each other, and creates more dynamic battles. Controlled by the `guardFlanking` config option.

### 4. Smart Target Prioritization (`TargetPrioritizationGoal.java`)

Guards no longer simply attack the nearest enemy. The prioritization system evaluates multiple factors to pick the best target:

| Factor | Priority Adjustment | Example |
|---|---|---|
| Swelling creeper | -50 (urgent) | Creeper about to explode near village |
| Active raider during raid | -25 | Raiders are the primary threat during raids |
| Creeper (idle) | -20 | Creepers are always high-priority targets |
| Ranged attacker | -15 | Skeletons and pillagers shooting from distance |
| Shielded enemy (axe guard) | -12 | Axe guards prioritize breaking shields |
| Mounted enemy (spear/trident) | -10 | Anti-cavalry weapon affinity |
| Nearly-dead enemy (<30% HP) | -10 | Finish off weakened targets quickly |
| Armored enemy (mace guard) | -8 | Maces are effective against armor |
| Enemy targeting a guard | -7 | Protect fellow guards under attack |
| Wounded enemy (<50% HP) | -5 | Good targets for quick elimination |

Additionally, weapon affinity shapes target selection: axe guards seek shielded enemies to break their shields; spear/trident guards prioritize mounted enemies; mace guards target heavily armored foes; ranged guards counter-snipe other ranged attackers and prioritize flying targets. Controlled by the `smartTargetPrioritization` config option.

### 5. Anti-Creeper Behavior (`AntiCreeperGoal.java`)

Previously, guards would stand next to creepers and try to melee them, resulting in explosions that killed guards and nearby villagers. The new `AntiCreeperGoal` detects creepers within 8 blocks that are swelling (about to explode) and makes all guards flee immediately at 1.4x speed. Ranged guards maintain distance while retreating. The goal runs at the highest priority (1) to ensure self-preservation overrides all other combat behaviors. After the creeper explodes or defuses, guards resume normal combat. This single improvement dramatically reduces guard casualties in everyday village defense. Controlled by the `antiCreeperBehavior` config option.

### 6. Squad System (`GuardSquadGoal.java`)

Captain-rank guards automatically organize nearby idle guards into squads:

- Squad size: 3-5 guards (configurable via `squadSize`)
- Squad members target the same enemy as their captain (focus fire)
- Squad members follow their captain when idle
- Squad members are validated every 40 ticks: must be alive, not following a player, not in combat
- Proper cleanup when a captain loses rank or dies

The squad system creates a chain of command where high-rank guards lead lower-rank ones, making village defense more coordinated and effective. Focus fire ensures that enemies are eliminated quickly rather than being peppered by unfocused attacks from many guards.

### 7. Patrol Flag Item (`PatrolFlagItem.java`)

A new craftable item that gives players fine-grained control over guard patrol routes:

- **Right-click a block**: Adds that position as a patrol waypoint (stored in item NBT via `CustomData`)
- **Right-click a guard**: Assigns the stored waypoints to the guard as a patrol route
- **Sneak + right-click a guard**: Shows the guard's current patrol route in chat
- Waypoints are displayed as tooltips on the item
- Maximum 8 waypoints per route (configurable via `maxPatrolWaypoints`)
- Guard plays a confirmation sound when assigned a route

### 8. Captain Inspiration Aura

Captain-rank guards now inspire nearby guards with a passive damage bonus:

- Range: 10 blocks (configurable via `captainInspirationRange`)
- Damage bonus: +0.2 (configurable via `captainInspirationDamageBonus`)
- Applies to all nearby guards, not just squad members
- Creates a tangible combat advantage for having high-rank guards on the front line

### 9. Ranged Accuracy by Rank

Archer guards now improve their accuracy as they level up:

- Base inaccuracy: 8.0 (Recruit rank)
- Improvement per rank: 2.0 (configurable via `rangedAccuracyPerRank`)
- A Captain-rank bow guard is significantly more accurate than a Recruit
- This makes the leveling system meaningful for ranged guards, not just melee

### 10. Cavalry Charge Bonus

Mounted guards deal extra damage when charging on horseback:

- Damage bonus: +0.5 (configurable via `cavalryChargeDamageBonus`)
- Combined with the existing mounted knockback bonus (1.3x), mounted guards are formidable
- Encourages players to provide horses for their guards

### 11. Shield-Food Swap Loop Fix (Critical)

Previously, wounded guards with shields would enter an infinite loop: drop shield to pick food, eat for one tick, pick shield back up, drop food, repeat endlessly. The fix introduces **instant food consumption**: when a wounded guard with a shield finds food on the ground, the food is consumed instantly via `finishUsingItem()` + direct `heal()` without ever swapping the shield out of the offhand slot. Additionally, a `recentlyDroppedIds` set tracks items dropped by the guard and excludes them from pickup for 60 ticks, preventing the guard from immediately picking up its own discarded equipment.

### 12. Bow Guard Freeze Fix (Critical)

Previously, bow guards would stop shooting and stand still indefinitely. The root cause was an inverted condition in `RaiseShieldGoal.stop()`: `if (!GuardRaiseShield) guard.stopUsingItem()` — which meant the shield was only lowered when the config was OFF. Fixed to always call `guard.stopUsingItem()`. Additionally, ranged guards now skip raising their shield when more than 5 blocks from their target (they should be shooting, not blocking).

### 13. Horse Mounting System (Fixed & Enhanced)

The horse mounting system was completely non-functional in previous versions. Multiple bugs were fixed across v3.3.7–v3.3.9:

- **`canContinueToUse()` premature stop**: The goal stopped before the guard could mount because it returned false when within 2 blocks of the horse. Fixed with a `mounted` flag.
- **`isVehicle()` vs `isPassenger()`**: Guards swapping horses every 5 seconds because `isVehicle()` checks if something rides the guard (wrong), not if the guard rides something. Changed to `isPassenger()`.
- **Active combat check**: `getTarget() != null` was too strict — dead/far targets blocked mounting. Fixed with `isInActiveCombat()` (target must be alive AND within 16 blocks).
- **Goal priority conflict**: Moved from priority 3 to 5 to avoid conflict with `WalkBackToCheckPointGoal`.
- **Mount navigation**: Added `tickMountNavigation()` to forward guard movement control to the horse's navigation for both combat and peacetime movement.

### 14. Entity Load Event Injection (`ServerLevelMixin.java`)

A new mixin injects event handling when entities are added to the server level via `addFreshEntity`. This ensures that mob target assignment logic runs correctly even for entities that spawn during gameplay (not just on chunk load), closing a gap where some hostile mobs would not trigger guard response.

### 15. ServerPlayer Accessor (`ServerPlayerAccessor.java`)

An accessor mixin for `ServerPlayer` that exposes private methods needed for the guard inventory GUI system. Uses Fabric's `@Invoker` pattern from mixin-gen to call `nextContainerCounter` and `initMenu`, which are required for properly opening the guard inventory container on the client side.

---

## Interaction Guide

| Action | How |
|--------|-----|
| **Open guard inventory** | Right-click with empty hand (requires reputation or Hero of the Village) |
| **Toggle follow mode** | Right-click a guard |
| **Mass follow (all nearby)** | Right-click a bell with Hero of the Village |
| **Set patrol point** | Right-click a bell |
| **Assign patrol route** | Use Patrol Flag: right-click blocks for waypoints, then right-click guard |
| **View guard's patrol route** | Sneak + right-click guard with Patrol Flag |
| **Give equipment** | Place items in guard inventory slots |
| **Convert villager** | Sneak + right-click villager while holding a weapon |
| **Use War Horn** | Right-click to rally all guards in range |
| **Feed a guard** | Give food via offhand inventory slot |

### Guard Inventory Layout

```
+---------+-------------+---------+
| Helmet  |  Chestplate | Leggings|
+---------+-------------+---------+
|  Boots  |   Offhand   | Mainhand|
|         |(Shield/Food)|(Weapon) |
+---------+-------------+---------+
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
| `GuardFormation` | `true` | Guards organize into military formations |
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
| `captainInspirationRange` | `10.0` | Range of captain's inspiration aura (blocks) |
| `captainInspirationDamageBonus` | `0.2` | Damage bonus from captain inspiration |

#### Weapon Specialization

| Option | Default | Description |
|--------|---------|-------------|
| `weaponSpecialization` | `true` | Enable weapon-based retreat behavior |
| `archerRetreatDistance` | `3.0` | Distance at which archers retreat from enemies |
| `berserkerDamageBonus` | `0.2` | Extra damage for axe-wielders (Berserker bonus) |
| `weaponSpecificBehavior` | `true` | Enable weapon-specific combat behavior (speeds, ranges, tactics) |
| `spearVsMountedBonus` | `1.0` | Extra damage for spear/trident vs mounted enemies |
| `axeVsShieldBonus` | `0.5` | Extra damage for axes vs shielded enemies |
| `maceDamageBonus` | `0.25` | Extra damage for mace-wielders |
| `axeShieldDisableSeconds` | `5` | How long axes disable shields (seconds) |
| `rangedAccuracyBase` | `8.0` | Base inaccuracy for Recruit-rank ranged guards |
| `rangedAccuracyPerRank` | `2.0` | Accuracy improvement per rank level |

#### War Horn

| Option | Default | Description |
|--------|---------|-------------|
| `warHornRange` | `128.0` | Horn effective radius (blocks) |
| `warHornCombatDurationSeconds` | `30` | Combat stance duration after horn blow |
| `combatStanceDamageBonus` | `0.3` | Extra damage during combat stance |

#### Mounts

| Option | Default | Description |
|--------|---------|-------------|
| `guardsAutoMountHorses` | `true` | Guards auto-mount nearby horses when idle |
| `mountedKnockbackBonus` | `1.3` | Knockback multiplier while mounted |
| `cavalryChargeDamageBonus` | `0.5` | Extra damage for mounted charge attacks |

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

#### Patrol

| Option | Default | Description |
|--------|---------|-------------|
| `patrolWaitTimeSeconds` | `5` | Time guards wait at each patrol waypoint |
| `maxPatrolWaypoints` | `8` | Maximum waypoints per patrol route |

#### Squad System

| Option | Default | Description |
|--------|---------|-------------|
| `squadSize` | `4` | Maximum guards per captain's squad |
| `squadFollowRange` | `15.0` | Range for squad members to follow captain |

#### Tactical AI

| Option | Default | Description |
|--------|---------|-------------|
| `guardFlanking` | `true` | Enable flanking behavior for melee guards |
| `antiCreeperBehavior` | `true` | Guards flee from charging creepers |
| `smartTargetPrioritization` | `true` | Enable smart target selection based on threat level |
| `formationRange` | `12.0` | Range for guards to detect formation members |
| `formationSpacing` | `1.5` | Spacing between guards in formation (blocks) |

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
| `speedModifier` | `0.35` | Base guard movement speed |
| `followRangeModifier` | `20.0` | Base guard follow range |

---

## Performance Optimizations

Guard Villagers v4.0 includes extensive performance optimizations for large villages with 50+ guards:

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
| Squad member scan | Entity scan every tick | Throttled to every 40 ticks |
| Formation scan | N/A (new feature) | Scans every 100 ticks; repositions every 20 ticks |
| Creeper detection | N/A (new feature) | Scans every 20 ticks; highest priority goal |
| Target prioritization | N/A (new feature) | Uses configurable `randomInterval` for scan frequency |

---

## Bug Fixes

### Critical Fixes (v4.0)

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| **Shield-food swap loop** — wounded guard with shield infinitely swaps shield and food | Guard dropped shield to eat food, then picked shield back up, then dropped food, loop | Instant food consumption: `finishUsingItem()` + `heal()` without swapping shield; `recentlyDroppedIds` tracking for 60 ticks |
| **Bow guard freeze** — archers stop shooting and stand still indefinitely | `RaiseShieldGoal.stop()` had inverted condition — only lowered shield when config was OFF | Always call `stopUsingItem()` in `stop()`; ranged guards skip shield raise when >5 blocks from target |
| **Horse mounting never worked** — guards never mounted tamed saddled horses | `canContinueToUse()` returned false within 2 blocks of horse, stopping before `tick()` could mount | Added `mounted` flag; proper `canContinueToUse` logic |
| **Guards swapping horses every 5 seconds** — mounted guards dismount and remount different horse | `isVehicle()` checks if something rides the guard (wrong); `isPassenger()` checks if guard rides something | Changed to `isPassenger()` for mounted detection |
| **Horse mounting blocked by dead targets** — guard wouldn't mount because `getTarget() != null` with dead/far target | Too-strict combat check | `isInActiveCombat()`: target must be alive AND within 16 blocks |
| **Goal priority conflict** — GuardMountHorseGoal at priority 3 conflicted with WalkBackToCheckPointGoal | Same priority, both use MOVE flag | Moved to priority 5 |
| **Friendly fire damage leak** — guard arrows sometimes hurt villagers/golems despite config | Single-layer prevention could miss during lag | Dual-layer mixin: pre-damage cancel + post-damage heal |

### Fixes (v3.2.0)

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
├── GuardItems.java                    # Item registration (spawn eggs, war horn, patrol flag)
├── GuardSounds.java                   # Sound event registration
├── GuardStats.java                    # Stat registration
├── GuardDataAttachments.java          # Data attachment cleanup
├── GuardVillagerTags.java             # Tag definitions
├── GuardPacketHandler.java            # Server-side packet handler
├── ModCompat.java                     # Gun mod compat (disabled)
├── HandlerEvents.java                 # Event handlers (interact, entity load, mob target)
├── configuration/
│   └── GuardConfig.java               # JSON-based configuration system
├── common/
│   ├── entities/
│   │   ├── Guard.java                 # Main guard entity (2500+ lines)
│   │   ├── GuardContainer.java        # Inventory container
│   │   └── ai/
│   │       ├── goals/                 # Behavior goals
│   │       │   ├── WeaponBehavior.java              # Weapon-specific combat characteristics
│   │       │   ├── GuardFormationGoal.java           # Military formations (shield wall, phalanx, etc.)
│   │       │   ├── GuardFlankingGoal.java            # Flanking AI (side/rear attacks)
│   │       │   ├── TargetPrioritizationGoal.java     # Smart target selection
│   │       │   ├── AntiCreeperGoal.java              # Flee from charging creepers
│   │       │   ├── GuardSquadGoal.java               # Captain-led squad system
│   │       │   ├── GuardRetreatGoal.java             # Crossbow/trident retreat
│   │       │   ├── GuardMountHorseGoal.java          # Auto-mount horses
│   │       │   ├── PickupBetterEquipmentGoal.java    # Auto equipment upgrade + shield/food
│   │       │   ├── GuardHelpNearbyGuardGoal.java     # Target sharing (coordination)
│   │       │   ├── GuardShareFoodGoal.java           # Food sharing with wounded guards
│   │       │   ├── AttackEntityDaytimeGoal.java      # Daytime attack behavior
│   │       │   ├── GetOutOfWaterGoal.java            # Water escape behavior
│   │       │   └── GolemFloatWaterGoal.java          # Golem water float
│   │       └── tasks/                 # Villager brain tasks
│   │           ├── HealGuardAndHero.java
│   │           ├── RepairGolem.java
│   │           ├── RepairGuardEquipment.java
│   │           ├── ShareGossipWithGuard.java
│   │           └── VillagerHelp.java
│   └── items/
│       ├── WarHornItem.java           # War Horn item logic
│       └── PatrolFlagItem.java        # Patrol Flag waypoint item
├── client/
│   ├── GuardVillagersClient.java      # Client-side initialization (models, renderer, network)
│   ├── GuardClientEvents.java         # Client-side event registration
│   ├── network/
│   │   └── GuardPacketHandler.java    # Client packet handler (inventory GUI)
│   ├── models/                        # Entity models (Villager + Steve variants)
│   ├── renderer/                      # Entity renderer
│   ├── gui/                           # Guard inventory screen
│   └── GuardSounds.java               # Client sound registration
├── networking/                        # Server-side networking packets
│   ├── GuardNetworking.java
│   ├── GuardOpenInventoryPacket.java
│   ├── GuardSetPatrolPosPacket.java
│   └── GuardFollowPacket.java
├── mixins/                            # Mixin classes
│   ├── SinglePoolElementMixin.java              # Structure guard spawning
│   ├── DefendVillageGoalGolemMixin.java         # Golem defense coordination
│   ├── VillagerGoalPackagesMixin.java           # Villager brain task injection
│   ├── MobSetTargetMixin.java                   # Target event interception
│   ├── LivingEntityDamageMixin.java             # Pre-damage friendly fire prevention
│   ├── LivingEntityHurtPostMixin.java           # Post-damage friendly fire cleanup
│   ├── ServerLevelMixin.java                    # Entity load event injection
│   └── ServerPlayerAccessor.java                # ServerPlayer method access for GUI
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
- **MaceItem**: New weapon type in 26.1.2, detected by `instanceof MaceItem`
- **DataComponents**: Armor uses `Equippable` data component; food uses `DataComponents.FOOD`
- **Access Widener**: Declared in `fabric.mod.json`, uses `official` mapping namespace
- **Entity ID Lookup**: `Entity.getEncodeId()` is protected — use `BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()`
- **Mixin compatibility**: `JAVA_25` compatibility level; uses `@Invoker` accessor pattern

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
