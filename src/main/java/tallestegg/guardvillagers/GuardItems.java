package tallestegg.guardvillagers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class GuardItems {
    public static final SpawnEggItem GUARD_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "guard_spawn_egg"),
            new SpawnEggItem(new Item.Properties().spawnEgg(GuardEntityType.GUARD))
    );

    public static final SpawnEggItem ILLUSIONER_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "illusioner_spawn_egg"),
            new SpawnEggItem(new Item.Properties().spawnEgg(EntityType.ILLUSIONER))
    );

    public static void register() {
        // Registration happens via the static initializer above
    }

    private GuardItems() {}
}
