package tallestegg.guardvillagers.mixins;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * 2. Banner system: Prevent guards from hurting allies (same banner team) — both guards AND players.
 * 3. Banner system: Declare war when an entity from one team attacks an entity from another team.
 * 4. Banner system: Prevent damage to neutral entities (different banners, not at war).
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

        // === Banner System: Guard vs Guard ===
        if (self instanceof Guard hurtGuard && source.getEntity() instanceof Guard attackerGuard) {
            // Declare war if different banner teams
            BannerAlliance.onGuardHurtBy(hurtGuard, attackerGuard);

            // Block damage if they shouldn't be fighting (allies or neutral)
            if (!BannerAlliance.shouldAttackGuard(attackerGuard, hurtGuard)) {
                cir.setReturnValue(false);
                return;
            }
        }

        // === Banner System: Player hitting a Guard ===
        if (self instanceof Guard hurtGuard && source.getEntity() instanceof Player attackerPlayer) {
            // If they are allies (same banner), cancel the damage completely
            // This prevents accidental friendly fire between banner allies
            if (BannerAlliance.areAllies(hurtGuard, attackerPlayer)) {
                cir.setReturnValue(false);
                return;
            }

            // If guard has a banner and player is neutral (different banner, not at war),
            // cancel the damage — neutral players shouldn't be able to hurt bannered guards
            String guardTeam = BannerAlliance.getBannerTeam(hurtGuard);
            String playerTeam = BannerAlliance.getBannerTeam(attackerPlayer);
            if (guardTeam != null && playerTeam != null && !guardTeam.equals(playerTeam)) {
                // Different banners — declare war first
                BannerAlliance.declareWar(guardTeam, playerTeam);
                // War is now declared, allow the damage
            } else if (guardTeam != null && playerTeam == null) {
                // Guard has banner, player doesn't — neutral, cancel damage
                cir.setReturnValue(false);
                return;
            }
        }

        // === Banner System: Guard hitting a Player ===
        if (self instanceof Player hurtPlayer && source.getEntity() instanceof Guard attackerGuard) {
            // If they are allies (same banner), cancel the damage
            if (BannerAlliance.areAllies(attackerGuard, hurtPlayer)) {
                cir.setReturnValue(false);
                return;
            }

            // If guard has a banner and player is neutral, cancel damage
            String guardTeam = BannerAlliance.getBannerTeam(attackerGuard);
            String playerTeam = BannerAlliance.getBannerTeam(hurtPlayer);
            if (guardTeam != null && playerTeam != null && !guardTeam.equals(playerTeam)) {
                // Different banners — if not at war, this shouldn't happen
                // but if it does, declare war
                BannerAlliance.declareWar(guardTeam, playerTeam);
            } else if (guardTeam != null && playerTeam == null) {
                // Guard has banner, player doesn't — neutral, cancel damage
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
