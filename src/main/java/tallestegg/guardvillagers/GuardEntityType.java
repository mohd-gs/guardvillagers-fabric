package tallestegg.guardvillagers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import tallestegg.guardvillagers.common.entities.Guard;

public class GuardEntityType {
    public static final EntityType<Guard> GUARD = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            Identifier.of(GuardVillagers.MODID, "guard"),
            EntityType.Builder.of(Guard::new, MobCategory.MISC)
                    .sized(0.6F, 1.90F)
                    .ridingOffset(-0.7F)
                    .setShouldReceiveVelocityUpdates(true)
                    .build(Identifier.of(GuardVillagers.MODID, "guard").toString())
    );

    public static void register() {
        // Registration happens via the static initializer above
    }
}
