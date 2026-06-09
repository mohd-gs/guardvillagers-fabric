package tallestegg.guardvillagers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import tallestegg.guardvillagers.common.entities.Guard;

public class GuardEntityType {
    private static final ResourceKey<EntityType<?>> GUARD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "guard"));

    public static final EntityType<Guard> GUARD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            GUARD_KEY,
            EntityType.Builder.of(Guard::new, MobCategory.MISC)
                    .sized(0.6F, 1.90F)
                    .ridingOffset(-0.7F)
                    .clientTrackingRange(10)
                    .build(GUARD_KEY)
    );

    public static void register() {
        // Registration happens via the static initializer above
    }
}
