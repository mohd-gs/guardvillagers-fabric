package tallestegg.guardvillagers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.Stat;

public class GuardStats {
    public static final Identifier GUARDS_MADE = Registry.register(
            BuiltInRegistries.CUSTOM_STAT,
            Identifier.of(GuardVillagers.MODID, "guards_made"),
            Identifier.of(GuardVillagers.MODID, "guards_made")
    );

    public static void register() {
        // Registration happens via the static initializer above
    }
}
