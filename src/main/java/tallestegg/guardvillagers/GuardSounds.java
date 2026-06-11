package tallestegg.guardvillagers;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class GuardSounds {
    public static final SoundEvent GUARD_AMBIENT = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.ambient"),
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.ambient"))
    );

    public static final SoundEvent GUARD_DEATH = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.death"),
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.death"))
    );

    public static final SoundEvent GUARD_HURT = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.hurt"),
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.hurt"))
    );

    public static final SoundEvent GUARD_YES = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.yes"),
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.yes"))
    );

    public static final SoundEvent GUARD_NO = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.no"),
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entity.guard.no"))
    );

    public static void register() {
        // Registration happens via the static initializer above
    }
}
