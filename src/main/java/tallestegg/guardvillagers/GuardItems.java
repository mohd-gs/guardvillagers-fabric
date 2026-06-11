package tallestegg.guardvillagers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import tallestegg.guardvillagers.common.items.WarHornItem;
import tallestegg.guardvillagers.common.items.PatrolFlagItem;

public class GuardItems {
    public static final SpawnEggItem GUARD_SPAWN_EGG = registerSpawnEgg(
            GuardVillagers.MODID, "guard_spawn_egg", GuardEntityType.GUARD
    );

    public static final SpawnEggItem ILLUSIONER_SPAWN_EGG = registerSpawnEgg(
            GuardVillagers.MODID, "illusioner_spawn_egg", EntityType.ILLUSIONER
    );

    // Feature 3: War Horn item
    public static final WarHornItem WAR_HORN = registerWarHorn(
            GuardVillagers.MODID, "war_horn"
    );

    // Feature 1: Patrol Flag item
    public static final PatrolFlagItem PATROL_FLAG = registerPatrolFlag(
            GuardVillagers.MODID, "patrol_flag"
    );

    // MC 26.1.x: Item.Properties requires setId() BEFORE the Item constructor runs.
    // This follows the vanilla Items.registerItem() / registerSpawnEgg() pattern.
    private static SpawnEggItem registerSpawnEgg(String namespace, String path, EntityType<?> entityType) {
        ResourceKey<Item> resourceKey = ResourceKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path)
        );
        Item.Properties properties = new Item.Properties()
                .spawnEgg(entityType)
                .setId(resourceKey);
        SpawnEggItem item = new SpawnEggItem(properties);
        return Registry.register(BuiltInRegistries.ITEM, resourceKey, item);
    }

    private static WarHornItem registerWarHorn(String namespace, String path) {
        ResourceKey<Item> resourceKey = ResourceKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path)
        );
        Item.Properties properties = new Item.Properties()
                .durability(50)
                .setId(resourceKey);
        WarHornItem item = new WarHornItem(properties);
        return Registry.register(BuiltInRegistries.ITEM, resourceKey, item);
    }

    private static PatrolFlagItem registerPatrolFlag(String namespace, String path) {
        ResourceKey<Item> resourceKey = ResourceKey.create(
                Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path)
        );
        Item.Properties properties = new Item.Properties()
                .durability(100)
                .setId(resourceKey);
        PatrolFlagItem item = new PatrolFlagItem(properties);
        return Registry.register(BuiltInRegistries.ITEM, resourceKey, item);
    }

    public static void register() {
        // Registration happens via the static initializer above
    }

    private GuardItems() {}
}
