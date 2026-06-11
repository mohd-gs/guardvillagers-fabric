package tallestegg.guardvillagers.mixins;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tallestegg.guardvillagers.configuration.GuardConfig;
import tallestegg.guardvillagers.common.entities.Guard;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.server.level.ServerLevel;

/**
 * Mixin to prevent guard arrows from hurting villagers/golems/guards when friendly fire is disabled.
 * Cancels the damage entirely instead of undoing it after the fact.
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void guardvillagers$onDamagePre(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!GuardConfig.COMMON.guardArrowsHurtVillagers) {
            if (self instanceof AbstractVillager || self instanceof IronGolem || self instanceof Guard) {
                if (source.getEntity() instanceof Guard) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
