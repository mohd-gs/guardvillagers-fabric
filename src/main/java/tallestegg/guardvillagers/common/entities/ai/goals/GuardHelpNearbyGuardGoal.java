package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import tallestegg.guardvillagers.common.entities.Guard;
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
 */
public class GuardHelpNearbyGuardGoal extends TargetGoal {
    private static final TargetingConditions NEARBY_GUARD_CONDITIONS = TargetingConditions.forCombat()
            .range(16.0D).ignoreLineOfSight();

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
        // Don't override if we already have a valid, close, alive target
        LivingEntity currentTarget = this.guard.getTarget();
        if (currentTarget != null && currentTarget.isAlive() && guard.distanceTo(currentTarget) < 16.0D) {
            return false;
        }

        // Find nearby guards that are fighting
        List<Guard> nearbyGuards = guard.level().getEntitiesOfClass(
                Guard.class,
                guard.getBoundingBox().inflate(GuardConfig.COMMON.GuardVillagerHelpRange, 8.0D, GuardConfig.COMMON.GuardVillagerHelpRange),
                g -> g != this.guard && g.isAlive() && g.getTarget() != null && g.getTarget().isAlive()
        );

        if (nearbyGuards.isEmpty()) return false;

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
        this.cooldown = 20; // 1 second cooldown before checking again
        super.stop();
    }
}
