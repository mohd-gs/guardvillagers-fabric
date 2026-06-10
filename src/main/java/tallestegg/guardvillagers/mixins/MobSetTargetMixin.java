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
        // PERFORMANCE: Use isAlive() && !level().isClientSide() instead of
        // level().getEntity(uuid). The old check called getEntity(UUID) which
        // iterates entities in the chunk manager — extremely expensive when
        // called on every setTarget(). isAlive() is O(1) and sufficient.
        if (!mob.level().isClientSide() && mob.isAlive()) {
            HandlerEvents.onMobSetTarget(mob, target);
        }
    }
}
