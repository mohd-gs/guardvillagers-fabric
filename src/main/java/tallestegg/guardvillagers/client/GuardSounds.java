package tallestegg.guardvillagers.client;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import tallestegg.guardvillagers.GuardVillagers;

public class GuardSounds {
    public static final Holder<SoundEvent> GUARD_AMBIENT = register("entity.guard.ambient");
    public static final Holder<SoundEvent> GUARD_DEATH = register("entity.guard.death");
    public static final Holder<SoundEvent> GUARD_HURT = register("entity.guard.hurt");
    public static final Holder<SoundEvent> GUARD_YES = register("entity.guard.yes");
    public static final Holder<SoundEvent> GUARD_NO = register("entity.guard.no");

    private static Holder<SoundEvent> register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, name);
        // BUG FIX: Use BuiltInRegistries.SOUND_EVENT (which is a Registry<SoundEvent>)
        // instead of Registries.SOUND_EVENT (which is a ResourceKey<Registry<SoundEvent>>).
        // registerForHolder requires a Registry instance, not a ResourceKey.
        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void register() {
        // Registration happens in field initializers
    }
}
