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
            Identifier.of(GuardVillagers.MODID, "guard_spawn_egg"),
            new SpawnEggItem(GuardEntityType.GUARD, 0x5A3F28, 0x3C89A6, new Item.Settings())
    );

    public static final SpawnEggItem ILLUSIONER_SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.of(GuardVillagers.MODID, "illusioner_spawn_egg"),
            new SpawnEggItem(EntityType.ILLUSIONER, 0x7B5E3A, 0x7B8FA6, new Item.Settings())
    );

    public static void register() {
        // Registration happens via the static initializer above
    }

    private GuardItems() {}
}
