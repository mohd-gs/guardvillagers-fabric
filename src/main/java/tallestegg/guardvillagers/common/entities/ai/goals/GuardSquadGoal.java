package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.EnumSet;
import java.util.List;

/**
 * Squad System - Captain guards organize nearby idle guards into squads.
 *
 * - Captain guards (rank 3) periodically organize nearby idle guards into squads
 * - A squad is 3-5 guards (configurable) led by a captain
 * - Squad members target the same enemy as their captain (focus fire)
 * - Squad members follow their captain when idle
 */
public class GuardSquadGoal extends Goal {
    private final Guard captain;
    private int cooldown = 0;
    private int recalcPathCooldown = 0;

    public GuardSquadGoal(Guard guard) {
        this.captain = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Only captain-rank guards organize squads
        if (captain.getGuardRank() != Guard.GuardRank.CAPTAIN) return false;
        if (!GuardConfig.COMMON.guardLeveling) return false;
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        // Only check every 200 ticks (10 seconds) — squad organization is low priority
        if (captain.tickCount % 200 != 0) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        // Continue as long as this guard is still a captain
        return captain.getGuardRank() == Guard.GuardRank.CAPTAIN && GuardConfig.COMMON.guardLeveling;
    }

    @Override
    public void start() {
        organizeSquad();
    }

    @Override
    public void tick() {
        // Focus fire: if captain has a target, make squad members target the same
        LivingEntity captainTarget = captain.getTarget();
        if (captainTarget != null && captainTarget.isAlive()) {
            List<Guard> squadMembers = getSquadMembers();
            for (Guard member : squadMembers) {
                LivingEntity memberTarget = member.getTarget();
                if (memberTarget == null || !memberTarget.isAlive() || memberTarget != captainTarget) {
                    if (member.canAttack(captainTarget)) {
                        member.setTarget(captainTarget);
                    }
                }
            }
        }

        // Follow captain when idle
        if (captainTarget == null) {
            this.recalcPathCooldown--;
            if (this.recalcPathCooldown <= 0) {
                this.recalcPathCooldown = this.adjustedTickDelay(10);
                List<Guard> squadMembers = getSquadMembers();
                for (Guard member : squadMembers) {
                    if (member.getTarget() == null && member.distanceTo(captain) > 4.0D) {
                        member.getNavigation().moveTo(captain, 1.0D);
                    }
                }
            }
        }

        // Reorganize squad periodically
        if (captain.tickCount % 400 == 0) {
            organizeSquad();
        }
    }

    @Override
    public void stop() {
        this.cooldown = 200;
    }

    private void organizeSquad() {
        double range = GuardConfig.COMMON.squadFollowRange;
        int maxSquadSize = GuardConfig.COMMON.squadSize;

        // Find nearby idle guards that don't already have a squad leader
        List<Guard> nearbyGuards = captain.level().getEntitiesOfClass(
                Guard.class,
                captain.getBoundingBox().inflate(range, 6.0D, range),
                g -> g != this.captain && g.isAlive() && !g.isSquadMember() && g.getTarget() == null
        );

        int recruited = 0;
        for (Guard candidate : nearbyGuards) {
            if (recruited >= maxSquadSize) break;
            candidate.setSquadLeader(captain);
            recruited++;
        }
    }

    private List<Guard> getSquadMembers() {
        double range = GuardConfig.COMMON.squadFollowRange;
        return captain.level().getEntitiesOfClass(
                Guard.class,
                captain.getBoundingBox().inflate(range, 6.0D, range),
                g -> g != this.captain && g.isAlive() && g.isSquadMember() && g.getSquadLeader() == this.captain
        );
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
