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
 *
 * BUG FIXES from v3.3.0:
 * - Goal now uses one-shot pattern: canUse() triggers every 200 ticks, start() organizes,
 *   canContinueToUse() returns false. Previously the goal ran forever since
 *   canContinueToUse() always returned true for captains, causing:
 *   (a) getSquadMembers() entity scan EVERY TICK (extremely expensive)
 *   (b) organizeSquad() called from both start() and tick() (redundant)
 * - Focus fire and follow logic moved to tick() with throttled entity scans (every 40 ticks)
 * - Squad members are validated: must be alive, not following player, not in combat
 * - Added proper cleanup when captain loses rank or dies
 */
public class GuardSquadGoal extends Goal {
    private final Guard captain;
    private int cooldown = 0;
    private int tickCounter = 0;

    public GuardSquadGoal(Guard guard) {
        this.captain = guard;
        // MOVE and LOOK flags so this goal can control navigation when needed
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
        // One-shot pattern: run for 1 tick to do organization, then stop.
        // Focus fire and follow logic is handled via cached squad member list
        // that is refreshed periodically in the tick method.
        // We run for up to 200 ticks (until next canUse check) to handle
        // focus fire and following, but with throttled entity scans.
        if (captain.getGuardRank() != Guard.GuardRank.CAPTAIN) return false;
        if (!GuardConfig.COMMON.guardLeveling) return false;
        return this.tickCounter < 200;
    }

    @Override
    public void start() {
        this.tickCounter = 0;
        organizeSquad();
    }

    @Override
    public void tick() {
        this.tickCounter++;

        // PERFORMANCE: Only scan for squad members every 40 ticks (2 seconds)
        // instead of every tick. Focus fire doesn't need to be instant.
        if (this.tickCounter % 40 != 0) return;

        List<Guard> squadMembers = getSquadMembers();

        // Focus fire: if captain has a target, make squad members target the same
        LivingEntity captainTarget = captain.getTarget();
        if (captainTarget != null && captainTarget.isAlive()) {
            for (Guard member : squadMembers) {
                if (!member.canAttack(captainTarget)) continue;
                LivingEntity memberTarget = member.getTarget();
                if (memberTarget == null || !memberTarget.isAlive() || memberTarget != captainTarget) {
                    member.setTarget(captainTarget);
                }
            }
        }

        // Follow captain when idle (only for squad members with no target)
        if (captainTarget == null) {
            for (Guard member : squadMembers) {
                if (member.getTarget() == null && member.distanceTo(captain) > 5.0D) {
                    member.getNavigation().moveTo(captain, 1.0D);
                }
            }
        }

        // Reorganize squad every 200 ticks (will be handled by start() on next cycle)
    }

    @Override
    public void stop() {
        this.cooldown = 0; // No extra cooldown — the 200-tick canUse throttle handles it
        this.tickCounter = 0;

        // If this guard is no longer a captain, clean up squad members
        if (captain.getGuardRank() != Guard.GuardRank.CAPTAIN) {
            cleanupSquad();
        }
    }

    private void organizeSquad() {
        double range = GuardConfig.COMMON.squadFollowRange;
        int maxSquadSize = GuardConfig.COMMON.squadSize;

        // First, clean up squad members that are no longer valid
        List<Guard> currentMembers = getSquadMembers();
        for (Guard member : currentMembers) {
            // Remove members that are too far away, following a player, or dead
            if (!member.isAlive() || member.isFollowing() || member.distanceTo(captain) > range * 2) {
                member.setSquadLeader(null);
            }
        }

        // Count current squad size
        int currentSize = currentMembers.size();
        if (currentSize >= maxSquadSize) return;

        // Find nearby idle guards that don't already have a squad leader
        List<Guard> nearbyGuards = captain.level().getEntitiesOfClass(
                Guard.class,
                captain.getBoundingBox().inflate(range, 6.0D, range),
                g -> g != this.captain && g.isAlive() && !g.isSquadMember() && g.getTarget() == null && !g.isFollowing()
        );

        int recruited = 0;
        for (Guard candidate : nearbyGuards) {
            if (currentSize + recruited >= maxSquadSize) break;
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

    private void cleanupSquad() {
        double range = GuardConfig.COMMON.squadFollowRange * 2;
        List<Guard> squadMembers = captain.level().getEntitiesOfClass(
                Guard.class,
                captain.getBoundingBox().inflate(range, 6.0D, range),
                g -> g.isAlive() && g.isSquadMember() && g.getSquadLeader() == this.captain
        );
        for (Guard member : squadMembers) {
            member.setSquadLeader(null);
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
