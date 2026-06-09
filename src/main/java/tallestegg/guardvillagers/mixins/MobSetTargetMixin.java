package tallestegg.guardvillagers.mixins;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tallestegg.guardvillagers.HandlerEvents;

import org.jetbrains.annotations.Nullable;

@Mixin(Mob.class)
public abstract class MobSetTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"))
    private void guardvillagers$onSetTarget(@Nullable LivingEntity target, CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        // Ensure we only run on the server side and the mob is actually added to the world
        if (!mob.level().isClientSide() && mob.level().getEntity(mob.getUUID()) != null) {
            HandlerEvents.onMobSetTarget(mob, target);
        }
    }
}
