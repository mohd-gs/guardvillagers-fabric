package tallestegg.guardvillagers.mixins;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tallestegg.guardvillagers.configuration.GuardConfig;
import tallestegg.guardvillagers.common.entities.Guard;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.animal.golem.IronGolem;

/**
 * Mixin to handle post-damage events for guard arrow cleanup.
 * If friendly fire is disabled, undo damage from guard arrows to villagers/golems/guards.
 */
@Mixin(LivingEntity.class)
public class LivingEntityHurtPostMixin {

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void guardvillagers$onActuallyHurt(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!GuardConfig.COMMON.guardArrowsHurtVillagers) {
            if (self instanceof AbstractVillager || self instanceof IronGolem || self instanceof Guard) {
                if (source.getEntity() instanceof Guard) {
                    self.setHealth(self.getHealth() + amount);
                }
            }
        }
    }
}
