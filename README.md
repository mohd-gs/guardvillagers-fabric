# Guard Villagers — Fabric Edition

<p align="center">
  <img src="src/main/resources/guard_villagers.png" alt="Guard Villagers Logo" width="128" height="128">
</p>

**Guard Villagers** adds village guards to Minecraft — armed protectors that defend villagers, iron golems, and players from hostile mobs. Originally created by TallestEgg for NeoForge, this is the **Fabric port** fully updated for **Minecraft 26.1.2**.

---

## Features

### Core Mechanics

| Feature | Description |
|---------|-------------|
| **Guard Entity** | A new mob that spawns naturally in villages, armed with swords, crossbows, or bows. They come in biome-based variants (desert, taiga, jungle, etc.) |
| **Guard Inventory** | 6-slot inventory (helmet, chestplate, leggings, boots, offhand, mainhand). Open it by right-clicking a guard with an empty hand |
| **Gossip & Reputation** | Guards use the vanilla gossip system. Your reputation affects whether they follow you, let you equip them, or attack you |
| **Village Defense** | Guards automatically target raiders, zombies, and other hostile mobs that threaten villagers and iron golems |
| **Patrol System** | Guards patrol around village workstations or walk through village paths. Set patrol points with bells |
| **Follow Mode** | Right-click a guard to toggle follow mode. With Hero of the Village, you can command multiple guards at once using bells |
| **Zombie Conversion** | On Normal/Hard difficulty, guards killed by zombies convert into zombie villagers instead of dying |
| **Shield Combat** | Guards use shields to block attacks. Kicking disarm opponents at close range |
| **Healing & Repair** | Clerics heal guards, blacksmiths repair iron golems, armorers/weaponsmiths repair guard equipment |

### New Features (v3.1.0)

#### 1. Guard Leveling & Ranking System
Guards now level up by killing enemies. As they gain kills, they advance through four ranks:

| Rank | Kills Required | Health Bonus | Damage Bonus |
|------|---------------|-------------|-------------|
| **Recruit** | 0 | — | — |
| **Soldier** | 5 | +4 HP | +0.5 |
| **Veteran** | 15 | +8 HP | +1.0 |
| **Captain** | 30 | +12 HP | +2.0 |

Rank modifiers persist through server restarts and chunk reloads. The rank can optionally be displayed in the guard's name tag (configurable).

#### 2. Weapon Specialization
Guards adapt their behavior based on their equipped weapon:
- **Archer Guards** (bow/crossbow): Automatically retreat when enemies get too close, maintaining optimal firing distance
- **Melee Guards** (sword/axe): Fight aggressively at close range with standard behavior
- **Berserker Bonus**: Guards wielding axes receive a configurable damage bonus

#### 3. War Horn
A craftable item that rallies all guards in a large radius:
- Plays a raid horn sound audible across the village
- All guards within range enter a **combat stance** for a configurable duration (default: 30 seconds)
- Idle guards are immediately assigned the nearest enemy target
- Guards with no nearby enemy navigate toward the horn-blower
- Has 50 durability — takes damage with each use
- **Recipe**: 3 iron ingots + 2 string + 1 goat horn arrangement

#### 5. Enhanced Mount System
Guards can now automatically mount nearby horses when idle:
- Guards seek out unclaimed horses within 8 blocks
- Mounted guards receive a speed compensator so they don't move slower than on foot
- **Mounted Knockback Bonus**: Guards on horseback deal extra knockback (configurable, default 1.3x)
- Mounting is disabled during active combat to prevent guards walking away from fights
- 10-second cooldown between mount attempts

#### 6. Wounded Behavior
When a guard's health drops below a threshold (default 25%):
- The guard enters a **wounded** state and retreats from combat
- Movement speed is reduced by 30% while wounded
- The guard navigates toward the nearest villager for healing
- Recovery occurs when health rises above the recovery threshold (default 40%)
- Wounded guards will still defend themselves if cornered but won't seek out enemies

#### 8. Night Watch
Guards become more vigilant during nighttime:
- Follow range is multiplied by a configurable factor (default 2x) at night
- Guards that were patrolling during the day automatically transition to night watch mode
- The extended range allows guards to detect and engage threats from farther away
- Returns to normal range at dawn

#### 9. Auto Equipment Upgrade
Guards automatically pick up better equipment from the ground:
- Scans for dropped items within a configurable range (default 3 blocks)
- Compares weapon tier (using rarity + enchantment as proxy) and equips upgrades
- Compares armor defense values and swaps for better pieces
- Picks up shields and food for the offhand slot when empty
- Old equipment is dropped on the ground for other guards or players
- Disabled during active combat to prevent mid-fight looting
- 2-second cooldown between equipment checks

### Bug Fixes
- **Guard Coordination Fix**: Previously, when 100+ guards were present, most would stand idle while a few fought. This was caused by the lack of a target-sharing mechanism between guards. `GuardHelpNearbyGuardGoal` now allows fighting guards to alert nearby idle guards, ensuring coordinated defense.
- **Attack Target Fix**: `doHurtTarget()` now uses the actual target entity parameter instead of `getTarget()`, preventing attacks on stale or incorrect targets.
- **Rank Persistence Fix**: Rank attribute modifiers are re-applied on entity load, preventing guards from losing their bonuses after server restart or chunk reload.

---

## Configuration

The config file is located at `config/guardvillagers.json`. It is divided into three sections:

### Common Config
| Category | Option | Default | Description |
|----------|--------|---------|-------------|
| **Raids** | `RaidAnimals` | `true` | Raiders attack farm animals during raids |
| | `WitchesVillager` | `true` | Witches attack villagers |
| | `IllagersRunFromPolarBears` | `true` | Illagers flee from polar bears |
| **Mob AI** | `AttackAllMobs` | `true` | Guards attack all hostile mobs |
| | `MobsAttackGuards` | `false` | All mobs target guards |
| | `MobBlackList` | *(see defaults)* | Mobs guards will never attack |
| | `MobWhiteList` | *empty* | Additional mobs for guards to attack |
| **Village** | `guardSpawnInVillage` | `6` | How many guards spawn per village |
| | `GuardFormation` | `true` | Shield guards stay close together |
| | `guardPatrolVillageAi` | `false` | Guards patrol village paths |
| | `guardPatrolAroundVillageWorkstations` | `true` | Guards patrol near workstations |
| **Guard** | `convertGuardOnDeath` | `true` | Guards convert to zombie villagers |
| | `GuardsOpenDoors` | `true` | Guards can open doors |
| | `GuardRaiseShield` | `false` | Guards permanently raise shields |
| | `GuardVillagerHelpRange` | `50.0` | Range for guards to help each other |
| | `amountOfHealthRegenerated` | `1.0` | Hearts regenerated per interval |
| | `reputationRequirement` | `15` | Min reputation to access guard inventory |
| | `followHero` | `true` | Guards only follow with Hero of the Village |
| **Leveling** | `guardLeveling` | `true` | Enable the ranking system |
| | `killsForSoldier` | `5` | Kills needed for Soldier rank |
| | `killsForVeteran` | `15` | Kills needed for Veteran rank |
| | `killsForCaptain` | `30` | Kills needed for Captain rank |
| | `soldierHealthBonus` | `4.0` | Extra HP for Soldier |
| | `veteranHealthBonus` | `8.0` | Extra HP for Veteran |
| | `captainHealthBonus` | `12.0` | Extra HP for Captain |
| **Weapons** | `weaponSpecialization` | `true` | Enable weapon-based behavior |
| | `archerRetreatDistance` | `3.0` | Distance at which archers retreat |
| | `berserkerDamageBonus` | `0.2` | Extra damage for axe-wielders |
| **War Horn** | `warHornRange` | `128.0` | Horn effective radius |
| | `warHornCombatDurationSeconds` | `30` | Combat stance duration |
| **Mounts** | `guardsAutoMountHorses` | `true` | Guards auto-mount horses |
| | `mountedKnockbackBonus` | `1.3` | Knockback multiplier while mounted |
| **Wounded** | `woundedBehavior` | `true` | Enable wounded retreat |
| | `woundedHealthThreshold` | `0.25` | Health % to become wounded |
| | `recoveredHealthThreshold` | `0.40` | Health % to recover |
| **Night Watch** | `nightWatchEnabled` | `true` | Enable night watch mode |
| | `nightFollowRangeMultiplier` | `2.0` | Follow range multiplier at night |
| **Equipment** | `autoEquipmentUpgrade` | `true` | Guards auto-pickup better gear |
| | `equipmentPickupRange` | `3.0` | Item pickup scan range |

### Client Config
| Option | Default | Description |
|--------|---------|-------------|
| `GuardSteve` | `false` | Use Steve model for guards instead of villager model |
| `bigHeadBabyVillager` | `true` | Baby villagers have big heads (Bedrock style) |
| `guardInventoryNumbers` | `true` | Show stack size numbers in guard inventory |
| `showRankInName` | `true` | Display rank title in guard name tags |

### Startup Config
| Option | Default | Description |
|--------|---------|-------------|
| `healthModifier` | `20.0` | Base guard max health |
| `speedModifier` | `0.5` | Base guard movement speed |
| `followRangeModifier` | `20.0` | Base guard follow range |

> **Note**: Startup config options require a game restart to take effect.

---

## How to Recruit a Guard

1. Find a villager with a convertible profession (by default: **Nitwit** or **Unemployed**)
2. Give them a **sword** (any type)
3. The villager transforms into a guard with the matching biome variant
4. Right-click the guard with an empty hand to open their inventory
5. Equip them with better armor, weapons, shields, or food

---

## Interaction Guide

| Action | How |
|--------|-----|
| Open guard inventory | Right-click with empty hand |
| Toggle follow mode | Right-click guard |
| Mass follow (all nearby guards) | Right-click a bell with Hero of the Village |
| Set patrol point | Right-click a bell to set patrol location |
| Give equipment | Right-click guard while holding armor/weapon |
| Feed a guard | Right-click guard while holding food |

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

## Technical Details

### Requirements
- **Minecraft**: 26.1.2
- **Fabric Loader**: >= 0.17.0
- **Fabric API**: Compatible version
- **Java**: 25+

### Build Setup
```bash
# Clone the repository
git clone https://github.com/mohd-gs/guardvillagers-fabric.git
cd guardvillagers-fabric

# Build the mod
./gradlew build

# The output JAR will be in build/libs/
```

### Architecture
```
src/main/java/tallestegg/guardvillagers/
├── GuardVillagers.java           # Main mod initializer
├── GuardEntityType.java          # Entity type registration
├── GuardItems.java               # Item registration (spawn eggs, war horn)
├── GuardSounds.java              # Sound event registration
├── GuardStats.java               # Stat registration
├── GuardDataAttachments.java     # Data attachment cleanup
├── GuardVillagerTags.java        # Tag definitions
├── HandlerEvents.java            # Event handlers (interact, entity load)
├── configuration/
│   └── GuardConfig.java          # JSON-based configuration system
├── common/
│   ├── entities/
│   │   ├── Guard.java            # Main guard entity (680+ lines)
│   │   ├── GuardContainer.java   # Inventory container
│   │   └── ai/
│   │       ├── goals/            # Behavior goals
│   │       │   ├── GuardRetreatGoal.java      # Archer retreat
│   │       │   ├── GuardMountHorseGoal.java    # Auto-mount horses
│   │       │   ├── PickupBetterEquipmentGoal.java # Auto equipment upgrade
│   │       │   └── GuardHelpNearbyGuardGoal.java  # Target sharing (coordination fix)
│   │       └── tasks/            # Villager brain tasks
│   │           ├── HealGuardAndHero.java
│   │           ├── RepairGolem.java
│   │           ├── RepairGuardEquipment.java
│   │           ├── ShareGossipWithGuard.java
│   │           └── VillagerHelp.java
│   └── items/
│       └── WarHornItem.java      # War Horn item logic
├── client/
│   ├── GuardClientEvents.java    # Client-side event registration
│   ├── models/                   # Entity models
│   ├── renderer/                 # Entity renderer
│   ├── gui/                      # Guard inventory screen
│   └── network/                  # Client packet handler
├── networking/                   # Server-side networking packets
├── mixins/                       # Mixin classes
│   ├── SinglePoolElementMixin.java        # Structure guard spawning
│   ├── DefendVillageGoalGolemMixin.java   # Golem defense coordination
│   ├── VillagerGoalPackagesMixin.java     # Villager brain task injection
│   └── MobSetTargetMixin.java             # Target event interception
└── loot_tables/
    ├── GuardLootTables.java      # Custom loot table registration
    └── functions/
        └── ArmorSlotFunction.java # Armor slot logic
```

### Key Technical Notes for MC 26.1.2

This mod targets **Minecraft 26.1.2**, the first fully unobfuscated version, which uses **Mojang Official Mappings** (not Yarn). Key API differences from older versions:

- **Registry**: Uses `Registry.register()` with `BuiltInRegistries` and `ResourceKey`
- **Item Registration**: `Item.Properties.setId(ResourceKey)` must be called before the item constructor
- **Entity Data Serialization**: Uses `ValueInput`/`ValueOutput` instead of `CompoundTag`
- **Gossip Container**: CODEC-based serialization with `GossipContainer.CODEC`
- **Access Widener**: Declared in `fabric.mod.json`, uses `official` mapping namespace
- **Entity ID Lookup**: `Entity.getEncodeId()` is protected — use `BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()` instead

---

## Credits

- **TallestEgg** — Original mod author (NeoForge version)
- **HadeZ / SadNya69** — Texture art
- **Fabric port** — Maintained for Minecraft 26.1.2

## License

- **Code**: MIT License
- **Assets**: All Rights Reserved
