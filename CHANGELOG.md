# Changelog

All notable changes to the Guard Villagers Fabric edition will be documented in this file.

## [4.0.0-26.1.2] — 2026-06-11

### Added

#### Weapon Behavior System
- Every weapon type now has unique combat characteristics: attack speed, optimal range, minimum engagement distance, movement speed modifier, and tactical behavior
- **Sword**: Fast attacks (0.75s cooldown), close range (1.8 blocks), parry between swings, circles enemy, 50% flanking chance
- **Axe**: Slow attacks (1.5s cooldown), berserker bonus, shield-breaking, always flanks for rear attacks, targets shielded enemies
- **Mace**: Medium-slow attacks (1.25s cooldown), aggressive charge, bonus vs armored targets, knockback specialist
- **Trident**: Medium attacks (1.1s cooldown), hybrid ranged+melee, extended reach (3.0 blocks), prioritizes mounted enemies
- **Bow**: Ranged (12.0 blocks), kiting, counter-snipes ranged foes
- **Crossbow**: Ranged (10.0 blocks), powerful shots, medium range
- New class: `WeaponBehavior.java` — centralizes all weapon-specific combat parameters
- New config options: `weaponSpecificBehavior`, `spearVsMountedBonus`, `axeVsShieldBonus`, `maceDamageBonus`, `axeShieldDisableSeconds`, `rangedAccuracyBase`, `rangedAccuracyPerRank`

#### Military Formation System
- Guards automatically organize into tactical formations when 3+ guards are nearby
- **Shield Wall**: Shield bearers in front line, ranged guards behind — most defensive
- **Phalanx**: Shield wall + spears attacking over shoulders — classic combined arms
- **Arrow Line**: Ranged guards in horizontal line, melee on flanks — maximizes firing arcs
- **Wedge**: V-shape charge with berserkers at tip — for breaking through enemy lines
- **Skirmish**: Loose spread — fallback for small groups
- Formation type determined by weapon composition of nearby guards
- Formation leader is the highest-rank guard
- Formations face toward the enemy and dissolve when enemies are within 3 blocks
- Ground validation ensures guards don't stand on air or inside blocks
- New class: `GuardFormationGoal.java`
- New config options: `formationRange`, `formationSpacing`

#### Flanking AI
- Guards can circle around enemies to attack from sides or behind instead of rushing directly
- Sword guards: 50% chance to flank (others hold the line)
- Berserker axes: Always try to flank for devastating rear attacks
- Spear guards: 60% chance to circle at extended range
- Requires at least one other guard engaging from the front
- Calculates flank positions based on enemy facing direction
- Prevents guards from bunching up and blocking each other
- New class: `GuardFlankingGoal.java`
- New config option: `guardFlanking`

#### Smart Target Prioritization
- Guards evaluate threat levels and pick the most dangerous enemy, not just the nearest
- Threat-based scoring: swelling creepers (-50), active raiders (-25), ranged attackers (-15), low-health enemies (-10)
- Weapon affinity: axes prioritize shielded, spears prioritize mounted, maces prioritize armored, ranged counter-snipe
- Distance-based base score: closer enemies are generally higher priority
- Enemies targeting guards or that recently hurt guards get priority boost
- New class: `TargetPrioritizationGoal.java`
- New config option: `smartTargetPrioritization`

#### Anti-Creeper Behavior
- Guards detect and flee from charging creepers within 8 blocks
- All guards flee regardless of weapon type at 1.4x speed
- Ranged guards can shoot from safe distance while retreating
- Runs at highest priority (1) to ensure self-preservation overrides combat
- Dramatically reduces guard casualties from creeper explosions
- New class: `AntiCreeperGoal.java`
- New config option: `antiCreeperBehavior`

#### Squad System
- Captain-rank guards automatically organize nearby idle guards into squads (3-5 members)
- Squad members target the same enemy as their captain (focus fire)
- Squad members follow their captain when idle
- Members validated every 40 ticks: must be alive, not following player, not in combat
- Proper cleanup when captain loses rank or dies
- New class: `GuardSquadGoal.java`
- New config options: `squadSize`, `squadFollowRange`

#### Patrol Flag Item
- New craftable item for defining custom patrol waypoint routes
- Right-click blocks to add waypoints (stored in item NBT via CustomData)
- Right-click a guard to assign the stored waypoints as a patrol route
- Sneak + right-click a guard to view its current patrol route
- Waypoints displayed as item tooltips
- Maximum 8 waypoints per route (configurable)
- New class: `PatrolFlagItem.java`
- New config option: `maxPatrolWaypoints`

#### Captain Inspiration Aura
- Captain-rank guards passively inspire nearby guards with a damage bonus
- Range: 10 blocks; Damage bonus: +0.2
- New config options: `captainInspirationRange`, `captainInspirationDamageBonus`

#### Ranged Accuracy by Rank
- Archer guards improve accuracy as they level up
- Base inaccuracy: 8.0 (Recruit); improvement per rank: 2.0
- Makes leveling meaningful for ranged guards
- New config options: `rangedAccuracyBase`, `rangedAccuracyPerRank`

#### Cavalry Charge Bonus
- Mounted guards deal extra damage when charging on horseback
- Damage bonus: +0.5 (stacks with mounted knockback bonus)
- New config option: `cavalryChargeDamageBonus`

#### Combat Stance Damage Bonus
- War Horn combat stance now provides a configurable damage bonus
- New config option: `combatStanceDamageBonus` (default 0.3)

#### Dual-Layer Friendly Fire Prevention
- Pre-damage mixin (`LivingEntityDamageMixin`) cancels damage entirely before it applies
- Post-damage mixin (`LivingEntityHurtPostMixin`) restores health if damage slips through
- New mixin classes: `LivingEntityDamageMixin.java`, `LivingEntityHurtPostMixin.java`

#### Entity Load Event Injection
- New mixin (`ServerLevelMixin`) injects event handling on `addFreshEntity`
- Ensures mob target assignment logic runs for entities spawned during gameplay
- New class: `ServerLevelMixin.java`

#### ServerPlayer Accessor
- Accessor mixin exposing `nextContainerCounter` and `initMenu` for guard inventory GUI
- Uses Fabric `@Invoker` pattern
- New class: `ServerPlayerAccessor.java`

#### Client Initialization
- Dedicated `GuardVillagersClient` class handles model layer registration, entity renderer, and client networking
- Separate `GuardPacketHandler` for client-side inventory screen handling
- New classes: `GuardVillagersClient.java`, `GuardPacketHandler.java` (client)

#### Mod Compatibility
- `ModCompat.java` placeholder for gun mod compatibility (disabled for Fabric)
- Included for future extension

### Fixed

#### Shield-Food Swap Loop (Critical)
- **Problem**: Wounded guard with shield would infinitely swap shield and food — drop shield, pick food, eat one tick, pick shield back, drop food, repeat
- **Fix**: Instant food consumption via `finishUsingItem()` + `heal()` without swapping shield; `recentlyDroppedIds` set tracks dropped items for 60 ticks

#### Bow Guard Freeze (Critical)
- **Problem**: Bow guards would stop shooting and stand still indefinitely
- **Fix**: `RaiseShieldGoal.stop()` always calls `stopUsingItem()` (was inverted); ranged guards skip shield raise when >5 blocks from target

#### Horse Mounting Never Worked (Critical)
- **Problem**: Guards never mounted tamed, saddled horses
- **Fix**: Added `mounted` flag for proper `canContinueToUse` lifecycle; fixed `canUse` to use `isPassenger()` instead of `isVehicle()`

#### Guards Swapping Horses Every 5 Seconds (Critical)
- **Problem**: Mounted guards would dismount and remount a different horse every 5 seconds
- **Fix**: Changed `isVehicle()` to `isPassenger()` — the former checks if something rides the guard (wrong), the latter checks if the guard rides something (correct)

#### Horse Mounting Blocked by Dead/Far Targets
- **Problem**: Guard wouldn't mount horses because `getTarget() != null` was true even for dead or distant targets
- **Fix**: `isInActiveCombat()` checks that target is alive AND within 16 blocks

#### Goal Priority Conflict
- **Problem**: GuardMountHorseGoal at priority 3 conflicted with WalkBackToCheckPointGoal (same priority, MOVE flag)
- **Fix**: Moved GuardMountHorseGoal to priority 5

#### Friendly Fire Damage Leak
- **Problem**: Guard arrows sometimes hurt villagers/golems despite config being disabled
- **Fix**: Dual-layer mixin prevention (pre-damage cancel + post-damage heal)

#### Mount Navigation
- **Problem**: Mounted guards couldn't be steered; horses stood still while guards tried to move
- **Fix**: Added `tickMountNavigation()` to forward guard's MoveControl to horse's Navigation for both combat and peacetime

### Changed

- Updated version from 3.2.0 to 4.0.0
- `GuardFormation` config now controls the new formation system (was previously shield guard grouping)
- `GuardItems.java` now registers Patrol Flag item
- `guardvillagers.mixins.json` updated with 4 new mixins: `LivingEntityDamageMixin`, `LivingEntityHurtPostMixin`, `ServerLevelMixin`, `ServerPlayerAccessor`
- Goal registration in `Guard.java` updated with new goals at appropriate priorities
- `PickupBetterEquipmentGoal` rewritten with instant food consumption and recentlyDroppedIds tracking
- `GuardMountHorseGoal` rewritten with proper mounting lifecycle, `isPassenger()` check, and priority 5
- `GuardRetreatGoal` updated with weapon-specific retreat distances
- `GuardShareFoodGoal` updated with improved food sharing logic

---

## [3.1.0-26.1.2] — 2026-06-10

### Added

#### Guard Leveling & Ranking System
- Guards now track their kill count and advance through four ranks: Recruit, Soldier, Veteran, and Captain
- Each rank provides permanent health and damage bonuses via `AttributeModifier`
- Rank thresholds are fully configurable (default: 5/15/30 kills)
- Rank modifiers persist through server restarts and chunk reloads (re-applied on entity load)
- Optional rank display in guard name tags (controlled by `showRankInName` client config)
- New config options: `guardLeveling`, `killsForSoldier`, `killsForVeteran`, `killsForCaptain`, `soldierHealthBonus`, `soldierDamageBonus`, `veteranHealthBonus`, `veteranDamageBonus`, `captainHealthBonus`, `captainDamageBonus`

#### Weapon Specialization
- Guards with ranged weapons (bow/crossbow) now automatically retreat when enemies get within a configurable distance
- Guards with axes receive a configurable "Berserker" damage bonus
- Retreat behavior is suppressed when the guard is in a wounded state
- New config options: `weaponSpecialization`, `archerRetreatDistance`, `berserkerDamageBonus`
- New goal: `GuardRetreatGoal`

#### War Horn Item
- Added a craftable War Horn item with 50 durability
- Using the horn plays a raid horn sound and rallies all guards within a 128-block radius
- Rallied guards enter a combat stance for a configurable duration (default: 30 seconds)
- Idle guards are immediately assigned the nearest enemy target within 32 blocks
- Guards with no nearby enemy navigate toward the horn-blower
- Recipe: available via `guardvillagers:war_horn` recipe
- New config options: `warHornRange`, `warHornCombatDurationSeconds`

#### Enhanced Mount System
- Guards automatically seek and mount nearby unclaimed horses when idle
- Mounted guards receive a speed compensator attribute modifier so they don't move slower than on foot
- Mounted guards deal extra knockback (configurable, default 1.3x)
- Mounting is disabled during active combat to prevent guards walking away from fights
- 10-second cooldown between mount attempts
- Speed compensator is properly removed when the guard dismounts
- New config options: `guardsAutoMountHorses`, `mountedKnockbackBonus`
- New goal: `GuardMountHorseGoal`

#### Wounded Behavior
- When a guard's health drops below a configurable threshold (default 25%), they enter a wounded state
- Wounded guards retreat from combat and navigate toward the nearest villager for healing
- Movement speed is reduced by 30% while wounded via a transient attribute modifier
- Recovery occurs when health rises above the recovery threshold (default 40%)
- Wounded guards will still defend themselves if cornered but won't seek out enemies
- New config options: `woundedBehavior`, `woundedHealthThreshold`, `recoveredHealthThreshold`

#### Night Watch
- Guards become more vigilant during nighttime with extended follow range
- Follow range is multiplied by a configurable factor (default 2x) at night
- Guards automatically transition between day patrol and night watch modes
- Returns to normal range at dawn
- New config options: `nightWatchEnabled`, `nightFollowRangeMultiplier`

#### Auto Equipment Upgrade
- Guards automatically scan for dropped items within a configurable range (default 3 blocks)
- Compares weapon tier (rarity + enchantment level as proxy) and equips upgrades
- Compares armor defense values and swaps for better pieces
- Picks up shields and food for the offhand slot when empty
- Old equipment is dropped on the ground for other guards or players to pick up
- Disabled during active combat to prevent mid-fight looting
- 2-second cooldown between equipment checks
- New config options: `autoEquipmentUpgrade`, `equipmentPickupRange`
- New goal: `PickupBetterEquipmentGoal`

### Fixed

#### Guard Coordination Bug (Critical)
- **Problem**: When 100+ guards were present, most would stand idle while a few fought. This was because there was no mechanism for guards to alert each other about threats — `HurtByTargetGoal.alertOthers()` only triggered when a guard was directly attacked, and `onMobSetTarget` only fired when a protected entity was targeted.
- **Solution**: Added `GuardHelpNearbyGuardGoal` (priority 4 in target selector). When a guard is idle or its target is dead/far away, it scans for nearby fighting guards and adopts their target. This ensures coordinated village defense even with large guard populations.
- **Technical detail**: Uses `TargetingConditions.forCombat().range(16).ignoreLineOfSight()` with a 1-second cooldown to prevent excessive target-switching.

#### Attack Target Mismatch
- **Problem**: `doHurtTarget()` was calling `super.doHurtTarget((ServerLevel) level(), target)` but the target parameter was sometimes different from `getTarget()`, leading to guards attacking stale or incorrect targets.
- **Solution**: The method now properly uses the passed `target` parameter consistently.

#### Rank Persistence After Restart
- **Problem**: Guards would lose their health/damage bonuses after server restart or chunk reload because attribute modifiers were only applied when a kill triggered a rank-up, not on entity load.
- **Solution**: `applyRankModifiers()` is now called in `readAdditionalSaveData()` after loading kill count and rank data.

### Changed

- Migrated entire codebase from **Minecraft 1.21.1 (Yarn mappings)** to **Minecraft 26.1.2 (Mojang Official Mappings)**
- Updated to **Fabric Loom 1.15**, **Java 25**, **Gradle 9.4.0**
- Removed Yarn mappings dependency — MC 26.1.x is the first fully unobfuscated version
- Entity data serialization now uses `ValueInput`/`ValueOutput` instead of `CompoundTag`
- Gossip data serialization now uses `GossipContainer.CODEC`
- Item registration now follows the `Item.Properties.setId(ResourceKey)` pattern required by MC 26.1.x
- `Entity.getEncodeId()` replaced with `BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()` (the original method is protected in 26.1.x)
- Configuration system switched from Forge config to custom JSON-based config (`guardvillagers.json`)
- Access widener updated to use `official` mapping namespace

---

## [3.0.4-26.1.2] — Previous Release

### Changed
- Initial Fabric port from NeoForge for Minecraft 26.1.2
- Basic guard functionality: spawning, inventory, combat, patrol, follow, reputation
- Village defense coordination with iron golems
- Zombie conversion on death
- Cleric healing and blacksmith repair behaviors
- 18 language translations

### Known Issues
- Guard coordination with large populations was unreliable (fixed in 3.1.0)
