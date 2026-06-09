package tallestegg.guardvillagers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class GuardItems {
    public static final SpawnEggItem GUARD_SPAWN_EGG = registerSpawnEgg(
            GuardVillagers.MODID, "guard_spawn_egg", GuardEntityType.GUARD
    );

    public static final SpawnEggItem ILLUSIONER_SPAWN_EGG = registerSpawnEgg(
            GuardVillagers.MODID, "illusioner_spawn_egg", EntityType.ILLUSIONER
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

    public static void register() {
        // Registration happens via the static initializer above
    }

    private GuardItems() {}
}
