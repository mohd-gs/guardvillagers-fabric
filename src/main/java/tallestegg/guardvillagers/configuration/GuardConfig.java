package tallestegg.guardvillagers.configuration;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import tallestegg.guardvillagers.GuardVillagers;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

        public void fromJson(JsonObject obj) {
            RaidAnimals = obj.getAsJsonPrimitive("RaidAnimals").getAsBoolean();
            WitchesVillager = obj.getAsJsonPrimitive("WitchesVillager").getAsBoolean();
            IllagersRunFromPolarBears = obj.getAsJsonPrimitive("IllagersRunFromPolarBears").getAsBoolean();
            AttackAllMobs = obj.getAsJsonPrimitive("AttackAllMobs").getAsBoolean();
            MobsAttackGuards = obj.getAsJsonPrimitive("MobsAttackGuards").getAsBoolean();
            MobBlackList = jsonList(obj, "MobBlackList");
            MobWhiteList = jsonList(obj, "MobWhiteList");
            professionsThatHeal = jsonList(obj, "professionsThatHeal");
            professionsThatRepairGolems = jsonList(obj, "professionsThatRepairGolems");
            professionsThatRepairGuards = jsonList(obj, "professionsThatRepairGuards");
            maxClericHeal = obj.getAsJsonPrimitive("maxClericHeal").getAsInt();
            maxGolemRepair = obj.getAsJsonPrimitive("maxGolemRepair").getAsInt();
            maxVillageRepair = obj.getAsJsonPrimitive("maxVillageRepair").getAsInt();
            armorersRepairGuardArmor = obj.getAsJsonPrimitive("armorersRepairGuardArmor").getAsBoolean();
            ConvertVillagerIfHaveHOTV = obj.getAsJsonPrimitive("ConvertVillagerIfHaveHOTV").getAsBoolean();
            BlacksmithHealing = obj.getAsJsonPrimitive("BlacksmithHealing").getAsBoolean();
            ClericHealing = obj.getAsJsonPrimitive("ClericHealing").getAsBoolean();
            VillagersRunFromPolarBears = obj.getAsJsonPrimitive("VillagersRunFromPolarBears").getAsBoolean();
            convertibleProfessions = jsonList(obj, "convertibleProfessions");
            golemFloat = obj.getAsJsonPrimitive("golemFloat").getAsBoolean();
            guardSinkToFightUnderWater = obj.getAsJsonPrimitive("guardSinkToFightUnderWater").getAsBoolean();
            depthGuardHuntUnderwater = obj.getAsJsonPrimitive("depthGuardHuntUnderwater").getAsInt();
            mobsGuardsProtectTargeted = jsonList(obj, "mobsGuardsProtectTargeted");
            mobsGuardsProtectHurt = jsonList(obj, "mobsGuardsProtectHurt");
            guardCrossbowAttackRadius = obj.getAsJsonPrimitive("guardCrossbowAttackRadius").getAsDouble();
            structuresThatSpawnGuards = jsonList(obj, "structuresThatSpawnGuards");
            guardSpawnInVillage = obj.getAsJsonPrimitive("guardSpawnInVillage").getAsInt();
            convertGuardOnDeath = obj.getAsJsonPrimitive("convertGuardOnDeath").getAsBoolean();
            multiFollow = obj.getAsJsonPrimitive("multiFollow").getAsBoolean();
            chanceToDropEquipment = obj.getAsJsonPrimitive("chanceToDropEquipment").getAsDouble();
            GuardsRunFromPolarBears = obj.getAsJsonPrimitive("GuardsRunFromPolarBears").getAsBoolean();
            GuardsOpenDoors = obj.getAsJsonPrimitive("GuardsOpenDoors").getAsBoolean();
            GuardRaiseShield = obj.getAsJsonPrimitive("GuardRaiseShield").getAsBoolean();
            chanceToBreakEquipment = obj.getAsJsonPrimitive("chanceToBreakEquipment").getAsDouble();
            guardTeleport = obj.getAsJsonPrimitive("guardTeleport").getAsBoolean();
            GuardFormation = obj.getAsJsonPrimitive("GuardFormation").getAsBoolean();
            friendlyFireCheckValue = obj.getAsJsonPrimitive("friendlyFireCheckValue").getAsDouble();
            FriendlyFire = obj.getAsJsonPrimitive("FriendlyFire").getAsBoolean();
            GuardVillagerHelpRange = obj.getAsJsonPrimitive("GuardVillagerHelpRange").getAsDouble();
            amountOfHealthRegenerated = obj.getAsJsonPrimitive("amountOfHealthRegenerated").getAsDouble();
            guardArrowsHurtVillagers = obj.getAsJsonPrimitive("guardArrowsHurtVillagers").getAsBoolean();
            giveGuardStuffHOTV = obj.getAsJsonPrimitive("giveGuardStuffHOTV").getAsBoolean();
            setGuardPatrolHotv = obj.getAsJsonPrimitive("setGuardPatrolHotv").getAsBoolean();
            reputationRequirement = obj.getAsJsonPrimitive("reputationRequirement").getAsInt();
            followHero = obj.getAsJsonPrimitive("followHero").getAsBoolean();
            reputationRequirementToBeAttacked = obj.getAsJsonPrimitive("reputationRequirementToBeAttacked").getAsInt();
            guardPatrolVillageAi = obj.getAsJsonPrimitive("guardPatrolVillageAi").getAsBoolean();
            guardPatrolAroundVillageWorkstations = obj.getAsJsonPrimitive("guardPatrolAroundVillageWorkstations").getAsBoolean();
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
            return obj;
        }
    }

    public static class ClientConfig {
        public boolean GuardSteve = false;
        public boolean bigHeadBabyVillager = true;
        public boolean guardInventoryNumbers = true;

        public void fromJson(JsonObject obj) {
            GuardSteve = obj.getAsJsonPrimitive("GuardSteve").getAsBoolean();
            bigHeadBabyVillager = obj.getAsJsonPrimitive("bigHeadBabyVillager").getAsBoolean();
            guardInventoryNumbers = obj.getAsJsonPrimitive("guardInventoryNumbers").getAsBoolean();
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("GuardSteve", GuardSteve);
            obj.addProperty("bigHeadBabyVillager", bigHeadBabyVillager);
            obj.addProperty("guardInventoryNumbers", guardInventoryNumbers);
            return obj;
        }
    }

    public static class StartUpConfig {
        public double healthModifier = 20.0;
        public double speedModifier = 0.5;
        public double followRangeModifier = 20.0;

        public void fromJson(JsonObject obj) {
            healthModifier = obj.getAsJsonPrimitive("healthModifier").getAsDouble();
            speedModifier = obj.getAsJsonPrimitive("speedModifier").getAsDouble();
            followRangeModifier = obj.getAsJsonPrimitive("followRangeModifier").getAsDouble();
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("healthModifier", healthModifier);
            obj.addProperty("speedModifier", speedModifier);
            obj.addProperty("followRangeModifier", followRangeModifier);
            return obj;
        }
    }

    private static List<String> jsonList(JsonObject obj, String key) {
        List<String> list = new ArrayList<>();
        if (obj.has(key)) {
            for (JsonElement e : obj.getAsJsonArray(key)) {
                list.add(e.getAsString());
            }
        }
        return list;
    }

    private static JsonArray listToJson(List<String> list) {
        JsonArray arr = new JsonArray();
        for (String s : list) {
            arr.add(s);
        }
        return arr;
    }
}
