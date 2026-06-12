package tallestegg.guardvillagers.mixins;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tallestegg.guardvillagers.configuration.GuardConfig;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.common.entities.BannerAlliance;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.server.level.ServerLevel;

/**
 * Mixin to handle guard damage interactions:
 * 1. Prevent guard arrows from hurting villagers/golems/guards when friendly fire is disabled.
 * 2. Banner system: Prevent guards from hurting allies (same banner team).
 * 3. Banner system: Declare war when a guard from one team attacks a guard from another team.
 * 4. Banner system: Prevent damage to neutral guards (different banners, not at war).
 */
@Mixin(LivingEntity.class)
public class LivingEntityDamageMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void guardvillagers$onDamagePre(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Original friendly fire check
        if (!GuardConfig.COMMON.guardArrowsHurtVillagers) {
            if (self instanceof AbstractVillager || self instanceof IronGolem || self instanceof Guard) {
                if (source.getEntity() instanceof Guard) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }

        // Banner system: Guard vs Guard interactions
        if (self instanceof Guard hurtGuard && source.getEntity() instanceof Guard attackerGuard) {
            // Declare war if different banner teams
            BannerAlliance.onGuardHurtBy(hurtGuard, attackerGuard);

            // Block damage if they shouldn't be fighting (allies or neutral)
            if (!BannerAlliance.shouldAttackGuard(attackerGuard, hurtGuard)) {
                cir.setReturnValue(false);
            }
        }
    }
}
