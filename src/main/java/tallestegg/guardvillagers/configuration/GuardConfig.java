package tallestegg.guardvillagers.configuration;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import tallestegg.guardvillagers.GuardVillagers;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class GuardConfig {
    public static final CommonConfig COMMON = new CommonConfig();
    public static final ClientConfig CLIENT = new ClientConfig();
    public static final StartUpConfig STARTUP = new StartUpConfig();

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("guardvillagers.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            save();
            return;
        }
        try (Reader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("common")) {
                COMMON.fromJson(root.getAsJsonObject("common"));
            }
            if (root.has("client")) {
                CLIENT.fromJson(root.getAsJsonObject("client"));
            }
            if (root.has("startup")) {
                STARTUP.fromJson(root.getAsJsonObject("startup"));
            }
        } catch (Exception e) {
            GuardVillagers.LOGGER.error("Failed to load Guard Villagers config", e);
            save();
        }
        // PERFORMANCE: Rebuild cached Sets after loading config
        COMMON.rebuildSets();
    }

    public static void save() {
        JsonObject root = new JsonObject();
        root.add("common", COMMON.toJson());
        root.add("client", CLIENT.toJson());
        root.add("startup", STARTUP.toJson());

        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            GuardVillagers.LOGGER.error("Failed to save Guard Villagers config", e);
        }
    }

    public static class CommonConfig {
        // PERFORMANCE: Cached Sets for O(1) lookups instead of O(n) List.contains()
        // These are rebuilt whenever the config is loaded or saved.
        private Set<String> mobBlackListSet = new HashSet<>();
        private Set<String> mobWhiteListSet = new HashSet<>();
        private Set<String> protectTargetedSet = new HashSet<>();
        private Set<String> protectHurtSet = new HashSet<>();
        private Set<String> convertibleProfessionsSet = new HashSet<>();

        public void rebuildSets() {
            mobBlackListSet = new HashSet<>(MobBlackList);
            mobWhiteListSet = new HashSet<>(MobWhiteList);
            protectTargetedSet = new HashSet<>(mobsGuardsProtectTargeted);
            protectHurtSet = new HashSet<>(mobsGuardsProtectHurt);
            convertibleProfessionsSet = new HashSet<>(convertibleProfessions);
        }

        /** O(1) lookup for blacklist */
        public boolean isBlackListed(String entityId) {
            return mobBlackListSet.contains(entityId);
        }

        /** O(1) lookup for whitelist */
        public boolean isWhiteListed(String entityId) {
            return mobWhiteListSet.contains(entityId);
        }

        /** O(1) lookup for protect-targeted list — used in onMobSetTarget which fires VERY frequently */
        public boolean isProtectTargeted(String entityId) {
            return protectTargetedSet.contains(entityId);
        }

        /** O(1) lookup for protect-hurt list */
        public boolean isProtectHurt(String entityId) {
            return protectHurtSet.contains(entityId);
        }

        /** O(1) lookup for convertible professions — used in entity interaction events */
        public boolean isConvertibleProfession(String professionId) {
            return convertibleProfessionsSet.contains(professionId);
        }

        // Raids and illagers
        public boolean RaidAnimals = true;
        public boolean WitchesVillager = true;
        public boolean IllagersRunFromPolarBears = true;

        // Mob AI in general
        public boolean AttackAllMobs = true;
        public boolean MobsAttackGuards = false;
        public List<String> MobBlackList = new ArrayList<>(List.of("minecraft:villager", "minecraft:iron_golem", "minecraft:wandering_trader", "guardvillagers:guard", "minecraft:creeper", "minecraft:enderman"));
        public List<String> MobWhiteList = new ArrayList<>();

        // Villager stuff
        public List<String> professionsThatHeal = new ArrayList<>(List.of("minecraft:cleric"));
        public List<String> professionsThatRepairGolems = new ArrayList<>(List.of("minecraft:armorer", "minecraft:weaponsmith"));
        public List<String> professionsThatRepairGuards = new ArrayList<>(List.of("minecraft:weaponsmith", "minecraft:armorer", "minecraft:toolsmith"));
        public int maxClericHeal = 3;
        public int maxGolemRepair = 3;
        public int maxVillageRepair = 3;
        public boolean armorersRepairGuardArmor = true;
        public boolean ConvertVillagerIfHaveHOTV = false;
        public boolean BlacksmithHealing = true;
        public boolean ClericHealing = true;
        public boolean VillagersRunFromPolarBears = true;
        public List<String> convertibleProfessions = new ArrayList<>(List.of("nitwit", "none"));

        // Golem stuff
        public boolean golemFloat = false;

        // Guard stuff
        public boolean guardSinkToFightUnderWater = true;
        public int depthGuardHuntUnderwater = 5;
        public List<String> mobsGuardsProtectTargeted = new ArrayList<>(List.of("minecraft:villager", "guardvillagers:guard", "minecraft:iron_golem"));
        public List<String> mobsGuardsProtectHurt = new ArrayList<>(List.of("minecraft:villager", "guardvillagers:guard", "minecraft:iron_golem"));
        public double guardCrossbowAttackRadius = 8.0;
        public List<String> structuresThatSpawnGuards = new ArrayList<>(List.of("minecraft:village/common/iron_golem"));
        public int guardSpawnInVillage = 6;
        public boolean convertGuardOnDeath = true;
        public boolean multiFollow = true;
        public double chanceToDropEquipment = 100.0;
        public boolean GuardsRunFromPolarBears = false;
        public boolean GuardsOpenDoors = true;
        public boolean GuardRaiseShield = false;
        public double chanceToBreakEquipment = 1.0;
        public boolean guardTeleport = true;
        public boolean GuardFormation = true;
        public double friendlyFireCheckValue = -0.9;
        public boolean FriendlyFire = true;
        public double GuardVillagerHelpRange = 50.0;
        public double amountOfHealthRegenerated = 1.0;
        public boolean guardArrowsHurtVillagers = true;
        public boolean giveGuardStuffHOTV = false;
        public boolean setGuardPatrolHotv = false;
        public int reputationRequirement = 15;
        public boolean followHero = true;
        public int reputationRequirementToBeAttacked = -100;
        public boolean guardPatrolVillageAi = false;
        public boolean guardPatrolAroundVillageWorkstations = true;

        // Guard Leveling
        public boolean guardLeveling = true;
        public int killsForSoldier = 5;
        public int killsForVeteran = 15;
        public int killsForCaptain = 30;
        public double soldierHealthBonus = 4.0;
        public double soldierDamageBonus = 0.5;
        public double veteranHealthBonus = 8.0;
        public double veteranDamageBonus = 1.0;
        public double captainHealthBonus = 12.0;
        public double captainDamageBonus = 2.0;

        // Weapon-Based Specialization
        public boolean weaponSpecialization = true;
        public double archerRetreatDistance = 3.0;
        public double berserkerDamageBonus = 0.2;

        // War Horn
        public double warHornRange = 128.0;
        public int warHornCombatDurationSeconds = 30;
        public double combatStanceDamageBonus = 0.3;

        // Enhanced Mount System
        public boolean guardsAutoMountHorses = true;
        public double mountedKnockbackBonus = 1.3;

        // Wounded Behavior
        public boolean woundedBehavior = true;
        public double woundedHealthThreshold = 0.25;
        public double recoveredHealthThreshold = 0.40;

        // Night Watch
        public boolean nightWatchEnabled = true;
        public double nightFollowRangeMultiplier = 2.0;

        // Auto Equipment Upgrade
        public boolean autoEquipmentUpgrade = true;
        public double equipmentPickupRange = 3.0;

        public void fromJson(JsonObject obj) {
            RaidAnimals = getBoolSafe(obj, "RaidAnimals", RaidAnimals);
            WitchesVillager = getBoolSafe(obj, "WitchesVillager", WitchesVillager);
            IllagersRunFromPolarBears = getBoolSafe(obj, "IllagersRunFromPolarBears", IllagersRunFromPolarBears);
            AttackAllMobs = getBoolSafe(obj, "AttackAllMobs", AttackAllMobs);
            MobsAttackGuards = getBoolSafe(obj, "MobsAttackGuards", MobsAttackGuards);
            MobBlackList = jsonList(obj, "MobBlackList", MobBlackList);
            MobWhiteList = jsonList(obj, "MobWhiteList", MobWhiteList);
            professionsThatHeal = jsonList(obj, "professionsThatHeal", professionsThatHeal);
            professionsThatRepairGolems = jsonList(obj, "professionsThatRepairGolems", professionsThatRepairGolems);
            professionsThatRepairGuards = jsonList(obj, "professionsThatRepairGuards", professionsThatRepairGuards);
            maxClericHeal = getIntSafe(obj, "maxClericHeal", maxClericHeal);
            maxGolemRepair = getIntSafe(obj, "maxGolemRepair", maxGolemRepair);
            maxVillageRepair = getIntSafe(obj, "maxVillageRepair", maxVillageRepair);
            armorersRepairGuardArmor = getBoolSafe(obj, "armorersRepairGuardArmor", armorersRepairGuardArmor);
            ConvertVillagerIfHaveHOTV = getBoolSafe(obj, "ConvertVillagerIfHaveHOTV", ConvertVillagerIfHaveHOTV);
            BlacksmithHealing = getBoolSafe(obj, "BlacksmithHealing", BlacksmithHealing);
            ClericHealing = getBoolSafe(obj, "ClericHealing", ClericHealing);
            VillagersRunFromPolarBears = getBoolSafe(obj, "VillagersRunFromPolarBears", VillagersRunFromPolarBears);
            convertibleProfessions = jsonList(obj, "convertibleProfessions", convertibleProfessions);
            golemFloat = getBoolSafe(obj, "golemFloat", golemFloat);
            guardSinkToFightUnderWater = getBoolSafe(obj, "guardSinkToFightUnderWater", guardSinkToFightUnderWater);
            depthGuardHuntUnderwater = getIntSafe(obj, "depthGuardHuntUnderwater", depthGuardHuntUnderwater);
            mobsGuardsProtectTargeted = jsonList(obj, "mobsGuardsProtectTargeted", mobsGuardsProtectTargeted);
            mobsGuardsProtectHurt = jsonList(obj, "mobsGuardsProtectHurt", mobsGuardsProtectHurt);
            guardCrossbowAttackRadius = getDoubleSafe(obj, "guardCrossbowAttackRadius", guardCrossbowAttackRadius);
            structuresThatSpawnGuards = jsonList(obj, "structuresThatSpawnGuards", structuresThatSpawnGuards);
            guardSpawnInVillage = getIntSafe(obj, "guardSpawnInVillage", guardSpawnInVillage);
            convertGuardOnDeath = getBoolSafe(obj, "convertGuardOnDeath", convertGuardOnDeath);
            multiFollow = getBoolSafe(obj, "multiFollow", multiFollow);
            chanceToDropEquipment = getDoubleSafe(obj, "chanceToDropEquipment", chanceToDropEquipment);
            GuardsRunFromPolarBears = getBoolSafe(obj, "GuardsRunFromPolarBears", GuardsRunFromPolarBears);
            GuardsOpenDoors = getBoolSafe(obj, "GuardsOpenDoors", GuardsOpenDoors);
            GuardRaiseShield = getBoolSafe(obj, "GuardRaiseShield", GuardRaiseShield);
            chanceToBreakEquipment = getDoubleSafe(obj, "chanceToBreakEquipment", chanceToBreakEquipment);
            guardTeleport = getBoolSafe(obj, "guardTeleport", guardTeleport);
            GuardFormation = getBoolSafe(obj, "GuardFormation", GuardFormation);
            friendlyFireCheckValue = getDoubleSafe(obj, "friendlyFireCheckValue", friendlyFireCheckValue);
            FriendlyFire = getBoolSafe(obj, "FriendlyFire", FriendlyFire);
            GuardVillagerHelpRange = getDoubleSafe(obj, "GuardVillagerHelpRange", GuardVillagerHelpRange);
            amountOfHealthRegenerated = getDoubleSafe(obj, "amountOfHealthRegenerated", amountOfHealthRegenerated);
            guardArrowsHurtVillagers = getBoolSafe(obj, "guardArrowsHurtVillagers", guardArrowsHurtVillagers);
            giveGuardStuffHOTV = getBoolSafe(obj, "giveGuardStuffHOTV", giveGuardStuffHOTV);
            setGuardPatrolHotv = getBoolSafe(obj, "setGuardPatrolHotv", setGuardPatrolHotv);
            reputationRequirement = getIntSafe(obj, "reputationRequirement", reputationRequirement);
            followHero = getBoolSafe(obj, "followHero", followHero);
            reputationRequirementToBeAttacked = getIntSafe(obj, "reputationRequirementToBeAttacked", reputationRequirementToBeAttacked);
            guardPatrolVillageAi = getBoolSafe(obj, "guardPatrolVillageAi", guardPatrolVillageAi);
            guardPatrolAroundVillageWorkstations = getBoolSafe(obj, "guardPatrolAroundVillageWorkstations", guardPatrolAroundVillageWorkstations);
            guardLeveling = getBoolSafe(obj, "guardLeveling", guardLeveling);
            killsForSoldier = getIntSafe(obj, "killsForSoldier", killsForSoldier);
            killsForVeteran = getIntSafe(obj, "killsForVeteran", killsForVeteran);
            killsForCaptain = getIntSafe(obj, "killsForCaptain", killsForCaptain);
            soldierHealthBonus = getDoubleSafe(obj, "soldierHealthBonus", soldierHealthBonus);
            soldierDamageBonus = getDoubleSafe(obj, "soldierDamageBonus", soldierDamageBonus);
            veteranHealthBonus = getDoubleSafe(obj, "veteranHealthBonus", veteranHealthBonus);
            veteranDamageBonus = getDoubleSafe(obj, "veteranDamageBonus", veteranDamageBonus);
            captainHealthBonus = getDoubleSafe(obj, "captainHealthBonus", captainHealthBonus);
            captainDamageBonus = getDoubleSafe(obj, "captainDamageBonus", captainDamageBonus);
            weaponSpecialization = getBoolSafe(obj, "weaponSpecialization", weaponSpecialization);
            archerRetreatDistance = getDoubleSafe(obj, "archerRetreatDistance", archerRetreatDistance);
            berserkerDamageBonus = getDoubleSafe(obj, "berserkerDamageBonus", berserkerDamageBonus);
            warHornRange = getDoubleSafe(obj, "warHornRange", warHornRange);
            warHornCombatDurationSeconds = getIntSafe(obj, "warHornCombatDurationSeconds", warHornCombatDurationSeconds);
            combatStanceDamageBonus = getDoubleSafe(obj, "combatStanceDamageBonus", combatStanceDamageBonus);
            guardsAutoMountHorses = getBoolSafe(obj, "guardsAutoMountHorses", guardsAutoMountHorses);
            mountedKnockbackBonus = getDoubleSafe(obj, "mountedKnockbackBonus", mountedKnockbackBonus);
            woundedBehavior = getBoolSafe(obj, "woundedBehavior", woundedBehavior);
            woundedHealthThreshold = getDoubleSafe(obj, "woundedHealthThreshold", woundedHealthThreshold);
            recoveredHealthThreshold = getDoubleSafe(obj, "recoveredHealthThreshold", recoveredHealthThreshold);
            nightWatchEnabled = getBoolSafe(obj, "nightWatchEnabled", nightWatchEnabled);
            nightFollowRangeMultiplier = getDoubleSafe(obj, "nightFollowRangeMultiplier", nightFollowRangeMultiplier);
            autoEquipmentUpgrade = getBoolSafe(obj, "autoEquipmentUpgrade", autoEquipmentUpgrade);
            equipmentPickupRange = getDoubleSafe(obj, "equipmentPickupRange", equipmentPickupRange);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("RaidAnimals", RaidAnimals);
            obj.addProperty("WitchesVillager", WitchesVillager);
            obj.addProperty("IllagersRunFromPolarBears", IllagersRunFromPolarBears);
            obj.addProperty("AttackAllMobs", AttackAllMobs);
            obj.addProperty("MobsAttackGuards", MobsAttackGuards);
            obj.add("MobBlackList", listToJson(MobBlackList));
            obj.add("MobWhiteList", listToJson(MobWhiteList));
            obj.add("professionsThatHeal", listToJson(professionsThatHeal));
            obj.add("professionsThatRepairGolems", listToJson(professionsThatRepairGolems));
            obj.add("professionsThatRepairGuards", listToJson(professionsThatRepairGuards));
            obj.addProperty("maxClericHeal", maxClericHeal);
            obj.addProperty("maxGolemRepair", maxGolemRepair);
            obj.addProperty("maxVillageRepair", maxVillageRepair);
            obj.addProperty("armorersRepairGuardArmor", armorersRepairGuardArmor);
            obj.addProperty("ConvertVillagerIfHaveHOTV", ConvertVillagerIfHaveHOTV);
            obj.addProperty("BlacksmithHealing", BlacksmithHealing);
            obj.addProperty("ClericHealing", ClericHealing);
            obj.addProperty("VillagersRunFromPolarBears", VillagersRunFromPolarBears);
            obj.add("convertibleProfessions", listToJson(convertibleProfessions));
            obj.addProperty("golemFloat", golemFloat);
            obj.addProperty("guardSinkToFightUnderWater", guardSinkToFightUnderWater);
            obj.addProperty("depthGuardHuntUnderwater", depthGuardHuntUnderwater);
            obj.add("mobsGuardsProtectTargeted", listToJson(mobsGuardsProtectTargeted));
            obj.add("mobsGuardsProtectHurt", listToJson(mobsGuardsProtectHurt));
            obj.addProperty("guardCrossbowAttackRadius", guardCrossbowAttackRadius);
            obj.add("structuresThatSpawnGuards", listToJson(structuresThatSpawnGuards));
            obj.addProperty("guardSpawnInVillage", guardSpawnInVillage);
            obj.addProperty("convertGuardOnDeath", convertGuardOnDeath);
            obj.addProperty("multiFollow", multiFollow);
            obj.addProperty("chanceToDropEquipment", chanceToDropEquipment);
            obj.addProperty("GuardsRunFromPolarBears", GuardsRunFromPolarBears);
            obj.addProperty("GuardsOpenDoors", GuardsOpenDoors);
            obj.addProperty("GuardRaiseShield", GuardRaiseShield);
            obj.addProperty("chanceToBreakEquipment", chanceToBreakEquipment);
            obj.addProperty("guardTeleport", guardTeleport);
            obj.addProperty("GuardFormation", GuardFormation);
            obj.addProperty("friendlyFireCheckValue", friendlyFireCheckValue);
            obj.addProperty("FriendlyFire", FriendlyFire);
            obj.addProperty("GuardVillagerHelpRange", GuardVillagerHelpRange);
            obj.addProperty("amountOfHealthRegenerated", amountOfHealthRegenerated);
            obj.addProperty("guardArrowsHurtVillagers", guardArrowsHurtVillagers);
            obj.addProperty("giveGuardStuffHOTV", giveGuardStuffHOTV);
            obj.addProperty("setGuardPatrolHotv", setGuardPatrolHotv);
            obj.addProperty("reputationRequirement", reputationRequirement);
            obj.addProperty("followHero", followHero);
            obj.addProperty("reputationRequirementToBeAttacked", reputationRequirementToBeAttacked);
            obj.addProperty("guardPatrolVillageAi", guardPatrolVillageAi);
            obj.addProperty("guardPatrolAroundVillageWorkstations", guardPatrolAroundVillageWorkstations);
            obj.addProperty("guardLeveling", guardLeveling);
            obj.addProperty("killsForSoldier", killsForSoldier);
            obj.addProperty("killsForVeteran", killsForVeteran);
            obj.addProperty("killsForCaptain", killsForCaptain);
            obj.addProperty("soldierHealthBonus", soldierHealthBonus);
            obj.addProperty("soldierDamageBonus", soldierDamageBonus);
            obj.addProperty("veteranHealthBonus", veteranHealthBonus);
            obj.addProperty("veteranDamageBonus", veteranDamageBonus);
            obj.addProperty("captainHealthBonus", captainHealthBonus);
            obj.addProperty("captainDamageBonus", captainDamageBonus);
            obj.addProperty("weaponSpecialization", weaponSpecialization);
            obj.addProperty("archerRetreatDistance", archerRetreatDistance);
            obj.addProperty("berserkerDamageBonus", berserkerDamageBonus);
            obj.addProperty("warHornRange", warHornRange);
            obj.addProperty("warHornCombatDurationSeconds", warHornCombatDurationSeconds);
            obj.addProperty("combatStanceDamageBonus", combatStanceDamageBonus);
            obj.addProperty("guardsAutoMountHorses", guardsAutoMountHorses);
            obj.addProperty("mountedKnockbackBonus", mountedKnockbackBonus);
            obj.addProperty("woundedBehavior", woundedBehavior);
            obj.addProperty("woundedHealthThreshold", woundedHealthThreshold);
            obj.addProperty("recoveredHealthThreshold", recoveredHealthThreshold);
            obj.addProperty("nightWatchEnabled", nightWatchEnabled);
            obj.addProperty("nightFollowRangeMultiplier", nightFollowRangeMultiplier);
            obj.addProperty("autoEquipmentUpgrade", autoEquipmentUpgrade);
            obj.addProperty("equipmentPickupRange", equipmentPickupRange);
            return obj;
        }
    }

    public static class ClientConfig {
        public boolean GuardSteve = false;
        public boolean bigHeadBabyVillager = true;
        public boolean guardInventoryNumbers = true;
        public boolean showRankInName = true;

        public void fromJson(JsonObject obj) {
            GuardSteve = getBoolSafe(obj, "GuardSteve", GuardSteve);
            bigHeadBabyVillager = getBoolSafe(obj, "bigHeadBabyVillager", bigHeadBabyVillager);
            guardInventoryNumbers = getBoolSafe(obj, "guardInventoryNumbers", guardInventoryNumbers);
            showRankInName = getBoolSafe(obj, "showRankInName", showRankInName);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("GuardSteve", GuardSteve);
            obj.addProperty("bigHeadBabyVillager", bigHeadBabyVillager);
            obj.addProperty("guardInventoryNumbers", guardInventoryNumbers);
            obj.addProperty("showRankInName", showRankInName);
            return obj;
        }
    }

    public static class StartUpConfig {
        public double healthModifier = 20.0;
        public double speedModifier = 0.35;
        public double followRangeModifier = 20.0;

        public void fromJson(JsonObject obj) {
            healthModifier = getDoubleSafe(obj, "healthModifier", healthModifier);
            speedModifier = getDoubleSafe(obj, "speedModifier", speedModifier);
            followRangeModifier = getDoubleSafe(obj, "followRangeModifier", followRangeModifier);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("healthModifier", healthModifier);
            obj.addProperty("speedModifier", speedModifier);
            obj.addProperty("followRangeModifier", followRangeModifier);
            return obj;
        }
    }

    private static List<String> jsonList(JsonObject obj, String key, List<String> defaultList) {
        if (!obj.has(key)) return new ArrayList<>(defaultList);
        List<String> list = new ArrayList<>();
        for (JsonElement e : obj.getAsJsonArray(key)) {
            list.add(e.getAsString());
        }
        return list;
    }

    private static boolean getBoolSafe(JsonObject obj, String key, boolean defaultValue) {
        if (!obj.has(key)) return defaultValue;
        try { return obj.getAsJsonPrimitive(key).getAsBoolean(); }
        catch (Exception e) { return defaultValue; }
    }

    private static int getIntSafe(JsonObject obj, String key, int defaultValue) {
        if (!obj.has(key)) return defaultValue;
        try { return obj.getAsJsonPrimitive(key).getAsInt(); }
        catch (Exception e) { return defaultValue; }
    }

    private static double getDoubleSafe(JsonObject obj, String key, double defaultValue) {
        if (!obj.has(key)) return defaultValue;
        try { return obj.getAsJsonPrimitive(key).getAsDouble(); }
        catch (Exception e) { return defaultValue; }
    }

    private static JsonArray listToJson(List<String> list) {
        JsonArray arr = new JsonArray();
        for (String s : list) {
            arr.add(s);
        }
        return arr;
    }
}
