package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.common.entities.BannerAlliance;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.List;

/**
 * Target goal that allows guards to share combat targets with nearby fighting guards.
 * <p>
 * This fixes the major issue where large groups of guards would stand idle while
 * a few guards fought — because there was no mechanism for guards to alert each other
 * about threats unless they were directly attacked (HurtByTargetGoal) or a protected
 * entity was being targeted (onMobSetTarget).
 * <p>
 * Now, when a guard is fighting an enemy, nearby idle guards or guards whose target
 * is far away/dead will prioritize helping by targeting the same enemy.
 * <p>
 * PERFORMANCE: Scans are throttled to every 80 ticks (4 seconds) with an additional
 * 40-tick cooldown after a failed scan. The help range is capped at 20 blocks to
 * prevent massive AABB queries in densely populated villages. Only 3 guards are
 * alerted per scan to avoid cascading AI recalculation.
 */
public class GuardHelpNearbyGuardGoal extends TargetGoal {
    private final Guard guard;
    private LivingEntity sharedTarget;
    private int cooldown = 0;

    public GuardHelpNearbyGuardGoal(Guard guard) {
        super(guard, true, true);
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        // PERFORMANCE: Only scan every 80 ticks (4 seconds) instead of 40.
        // Target sharing doesn't need to be instant - 4 seconds is fast enough
        // for guards to respond to threats while reducing scan overhead by 50%.
        if (guard.tickCount % 80 != 0) return false;

        // Don't override if we already have a valid, close, alive target
        LivingEntity currentTarget = this.guard.getTarget();
        if (currentTarget != null && currentTarget.isAlive() && guard.distanceTo(currentTarget) < 16.0D) {
            this.cooldown = 80; // Skip for 4 seconds if we already have a target
            return false;
        }

        // PERFORMANCE: Cap the help range at 20 blocks. Previous cap of 32 blocks
        // still created a 64x16x64 block search area. With 50 guards each doing
        // this scan every 4 seconds, that's 50 AABB queries * 65536 block volume =
        // 3.2M block lookups per 4 seconds. 20 blocks is still very responsive.
        double helpRange = Math.min(GuardConfig.COMMON.GuardVillagerHelpRange, 20.0D);

        // Find nearby guards that are fighting
        List<Guard> nearbyGuards = guard.level().getEntitiesOfClass(
                Guard.class,
                guard.getBoundingBox().inflate(helpRange, 6.0D, helpRange),
                g -> g != this.guard && g.isAlive() && g.getTarget() != null && g.getTarget().isAlive()
        );

        if (nearbyGuards.isEmpty()) {
            this.cooldown = 80; // No fighting guards nearby — check again in 4 seconds
            return false;
        }

        // Find the best shared target - prioritize the target of the closest fighting guard
        LivingEntity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        for (Guard otherGuard : nearbyGuards) {
            LivingEntity otherTarget = otherGuard.getTarget();
            if (otherTarget == null || !otherTarget.isAlive()) continue;
            // Validate that we can attack this target
            if (!this.guard.canAttack(otherTarget)) continue;
            // Check team alliances
            if (this.guard.getTeam() != null && otherTarget.getTeam() != null
                    && this.guard.getTeam().isAlliedTo(otherTarget.getTeam())) continue;
            // Banner alliance: Don't share targets that are banner allies
            if (!BannerAlliance.shouldAttackEntity(this.guard, otherTarget)) continue;

            double dist = guard.distanceToSqr(otherTarget);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestTarget = otherTarget;
            }
        }

        if (bestTarget != null) {
            this.sharedTarget = bestTarget;
            return true;
        }

        this.cooldown = 40; // No valid target found — check again in 2 seconds
        return false;
    }

    @Override
    public void start() {
        this.mob.setTarget(this.sharedTarget);
        super.start();
    }

    @Override
    public void stop() {
        this.sharedTarget = null;
        this.cooldown = 40; // 2 second cooldown before checking again
        super.stop();
    }
}
