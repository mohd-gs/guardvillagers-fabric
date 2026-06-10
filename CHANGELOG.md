# Changelog

All notable changes to the Guard Villagers Fabric edition will be documented in this file.

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
