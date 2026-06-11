package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Military Formation System for Guard Villagers.
 * <p>
 * This goal organizes nearby guards into tactical formations during combat
 * and while idle. Formations are determined by the mix of weapon types
 * present among nearby guards.
 * <p>
 * Formation Types:
 * - SHIELD_WALL: Shield bearers in front, ranged behind
 * - PHALANX: Shield wall + spears attacking over shoulders
 * - ARROW_LINE: Ranged in horizontal line, melee on flanks
 * - WEDGE: V-shape charge with berserkers at tip
 * - SKIRMISH: Loose spread (fallback)
 * <p>
 * BUG FIXES (v4.0.1):
 * - Fixed List mutation bug: guards.add(0, this.guard) modified the list
 *   returned by getNearbyGuards(), corrupting index calculations in
 *   calculateFormationPosition(). Now creates a new ArrayList copy.
 * - Fixed canContinueToUse() being too restrictive: removed the check
 *   that broke formation when no target and not patrolling. Guards should
 *   maintain defensive formations even during peacetime.
 * - Lowered priority from 6 to 4 so formations can actually run instead
 *   of being always overridden by WalkBackToCheckPointGoal (priority 3).
 */
public class GuardFormationGoal extends Goal {
    private final Guard guard;
    private FormationType currentFormation = FormationType.SKIRMISH;
    private Vec3 formationTarget = null; // Where this guard should stand
    private int scanCooldown = 0;
    private int repositionCooldown = 0;

    public enum FormationType {
        SHIELD_WALL,  // Shield bearers in front, ranged behind
        PHALANX,      // Shield wall + spears behind
        ARROW_LINE,   // Ranged in a line, melee on flanks
        WEDGE,        // V-shape charge formation
        SKIRMISH      // Loose formation (fallback)
    }

    public GuardFormationGoal(Guard guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.GuardFormation) return false;
        if (!GuardConfig.COMMON.weaponSpecificBehavior) return false;
        if (guard.isBaby()) return false;
        if (guard.isFollowing()) return false;
        // Don't form formations while mounted
        if (guard.isPassenger()) return false;

        // PERFORMANCE: Only scan every 100 ticks (5 seconds)
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return false;
        }

        // Need at least 3 guards nearby to form a formation
        List<Guard> nearbyGuards = getNearbyGuards();
        if (nearbyGuards.size() < 3) {
            this.scanCooldown = 100; // Recheck in 5 seconds
            return false;
        }

        // Determine formation type based on weapon composition
        this.currentFormation = determineFormationType(nearbyGuards);

        // Calculate this guard's position in the formation
        this.formationTarget = calculateFormationPosition(nearbyGuards);

        if (this.formationTarget == null) {
            this.scanCooldown = 50;
            return false;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!GuardConfig.COMMON.GuardFormation) return false;
        if (guard.isFollowing()) return false;
        if (guard.isPassenger()) return false;

        // Break formation if in active close combat (enemies within 3 blocks)
        LivingEntity target = guard.getTarget();
        if (target != null && target.isAlive() && guard.distanceTo(target) < 3.0D) {
            return false; // Too close for formation — fight individually
        }

        // Keep formation even without a target (peacetime defensive formation)
        // Only break if we've been running too long without revalidation
        return this.formationTarget != null;
    }

    @Override
    public void start() {
        this.repositionCooldown = 0;
    }

    @Override
    public void stop() {
        this.formationTarget = null;
        this.scanCooldown = 60; // 3 second cooldown before reforming
    }

    @Override
    public void tick() {
        if (this.formationTarget == null) return;

        // Reposition every 20 ticks (1 second) to avoid constant path recalculation
        if (this.repositionCooldown > 0) {
            this.repositionCooldown--;
            return;
        }
        this.repositionCooldown = 20;

        // Recalculate formation position if we have a target (formation faces enemy)
        LivingEntity target = guard.getTarget();
        if (target != null && target.isAlive()) {
            List<Guard> nearbyGuards = getNearbyGuards();
            this.formationTarget = calculateFormationPosition(nearbyGuards);
        }

        if (this.formationTarget == null) return;

        double dist = guard.distanceToSqr(this.formationTarget);
        if (dist > 2.0D) { // More than ~1.4 blocks from position
            guard.getNavigation().moveTo(this.formationTarget.x, this.formationTarget.y, this.formationTarget.z, 1.0D);
        } else {
            guard.getNavigation().stop();
        }

        // Face the enemy if we have one
        if (target != null) {
            guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    // === Formation Determination ===

    private List<Guard> getNearbyGuards() {
        double range = GuardConfig.COMMON.formationRange;
        return guard.level().getEntitiesOfClass(
                Guard.class,
                guard.getBoundingBox().inflate(range, 4.0D, range),
                g -> g != this.guard && g.isAlive() && !g.isFollowing() && !g.isPassenger()
        );
    }

    /**
     * Determine the best formation based on the weapon composition of nearby guards.
     */
    private FormationType determineFormationType(List<Guard> guards) {
        int shieldBearers = 0;
        int spearGuards = 0;
        int rangedGuards = 0;
        int berserkerAxes = 0;

        for (Guard g : guards) {
            WeaponBehavior.WeaponType type = WeaponBehavior.getWeaponType(g);
            if (g.isShieldGuard()) shieldBearers++;
            if (type == WeaponBehavior.WeaponType.SPEAR || type == WeaponBehavior.WeaponType.TRIDENT) spearGuards++;
            if (type == WeaponBehavior.WeaponType.BOW || type == WeaponBehavior.WeaponType.CROSSBOW) rangedGuards++;
            if (type == WeaponBehavior.WeaponType.AXE && g.isBerserker()) berserkerAxes++;
        }

        // Also count this guard's role
        WeaponBehavior.WeaponType myType = WeaponBehavior.getWeaponType(guard);
        if (guard.isShieldGuard()) shieldBearers++;
        if (myType == WeaponBehavior.WeaponType.SPEAR || myType == WeaponBehavior.WeaponType.TRIDENT) spearGuards++;
        if (myType == WeaponBehavior.WeaponType.BOW || myType == WeaponBehavior.WeaponType.CROSSBOW) rangedGuards++;
        if (myType == WeaponBehavior.WeaponType.AXE && guard.isBerserker()) berserkerAxes++;

        // PHALANX: Shield wall + spears = classic combined arms
        if (shieldBearers >= 2 && spearGuards >= 1) {
            return FormationType.PHALANX;
        }

        // SHIELD_WALL: Shields + ranged = defensive line
        if (shieldBearers >= 2 && rangedGuards >= 1) {
            return FormationType.SHIELD_WALL;
        }

        // WEDGE: Multiple berserkers/axes = charge formation
        if (berserkerAxes >= 2) {
            return FormationType.WEDGE;
        }

        // ARROW_LINE: Multiple ranged = firing line
        if (rangedGuards >= 2) {
            return FormationType.ARROW_LINE;
        }

        // Default: SKIRMISH
        return FormationType.SKIRMISH;
    }

    /**
     * Calculate this guard's position in the formation.
     * The formation is centered on the formation leader (highest-rank guard).
     * The formation faces toward the enemy (or leader's facing direction if no enemy).
     *
     * FIX: Creates a new ArrayList copy before adding this guard,
     * preventing mutation of the getNearbyGuards() result.
     */
    private Vec3 calculateFormationPosition(List<Guard> nearbyGuards) {
        // Find the formation leader (highest rank, or closest to center)
        Guard leader = findFormationLeader(nearbyGuards);
        if (leader == null) return null;

        // Determine formation facing direction
        LivingEntity target = findCommonTarget(nearbyGuards);
        double facingAngle;
        if (target != null) {
            facingAngle = Math.atan2(target.getZ() - leader.getZ(), target.getX() - leader.getX());
        } else {
            facingAngle = Math.toRadians(leader.getYRot()); // Face leader's direction
        }

        // FIX: Create a NEW list that includes this guard
        // Previously, guards.add(0, this.guard) mutated the list from
        // getNearbyGuards(), corrupting subsequent calculations
        List<Guard> allGuards = new ArrayList<>(nearbyGuards);
        allGuards.add(0, this.guard);

        // Sort guards into roles for formation assignment
        allGuards.sort((a, b) -> {
            int roleA = getFormationRolePriority(a);
            int roleB = getFormationRolePriority(b);
            if (roleA != roleB) return Integer.compare(roleA, roleB);
            // Same role: sort by distance to leader (closer = center)
            return Double.compare(a.distanceToSqr(leader), b.distanceToSqr(leader));
        });

        // Calculate positions based on formation type
        Vec3 rawPosition = switch (currentFormation) {
            case SHIELD_WALL -> calculateShieldWallPosition(allGuards, leader, facingAngle);
            case PHALANX -> calculatePhalanxPosition(allGuards, leader, facingAngle);
            case ARROW_LINE -> calculateArrowLinePosition(allGuards, leader, facingAngle);
            case WEDGE -> calculateWedgePosition(allGuards, leader, facingAngle);
            case SKIRMISH -> calculateSkirmishPosition(allGuards, leader, facingAngle);
        };

        // Validate that the position has solid ground beneath
        return validateGroundPosition(rawPosition);
    }

    private Guard findFormationLeader(List<Guard> guards) {
        // Include this guard in the leader search
        Guard best = this.guard;
        int bestRank = this.guard.getGuardRank().level;
        for (Guard g : guards) {
            if (g.getGuardRank().level > bestRank) {
                bestRank = g.getGuardRank().level;
                best = g;
            }
        }
        return best;
    }

    private LivingEntity findCommonTarget(List<Guard> guards) {
        // Find the most common target among the guards (including self)
        LivingEntity target = guard.getTarget();
        if (target != null && target.isAlive()) return target;
        // Fall back to any guard's target
        for (Guard g : guards) {
            if (g.getTarget() != null && g.getTarget().isAlive()) return g.getTarget();
        }
        return null;
    }

    /**
     * Formation role priority (lower = front line):
     * 0 = Shield bearer (front line)
     * 1 = Spear/trident (behind shields)
     * 2 = Sword (front line support)
     * 3 = Mace (front line)
     * 4 = Ranged (back line)
     * 5 = Berserker (lead charge)
     */
    private int getFormationRolePriority(Guard g) {
        WeaponBehavior.WeaponType type = WeaponBehavior.getWeaponType(g);
        if (g.isShieldGuard()) return 0;
        if (type == WeaponBehavior.WeaponType.SPEAR || type == WeaponBehavior.WeaponType.TRIDENT) return 1;
        if (type == WeaponBehavior.WeaponType.SWORD) return 2;
        if (type == WeaponBehavior.WeaponType.MACE) return 3;
        if (type == WeaponBehavior.WeaponType.BOW || type == WeaponBehavior.WeaponType.CROSSBOW) return 4;
        if (type == WeaponBehavior.WeaponType.AXE && g.isBerserker()) return 5;
        return 2;
    }

    /**
     * Get this guard's index in the sorted formation list.
     */
    private int getMyIndex(List<Guard> sortedGuards) {
        for (int i = 0; i < sortedGuards.size(); i++) {
            if (sortedGuards.get(i) == this.guard) return i;
        }
        return 0;
    }

    // === Formation Position Calculations ===

    /**
     * SHIELD_WALL: Shield bearers form a horizontal line facing the enemy.
     * Ranged guards stand 3-4 blocks behind the shield line.
     * Other melee guards fill gaps in the shield line or stand behind.
     */
    private Vec3 calculateShieldWallPosition(List<Guard> guards, Guard leader, double facingAngle) {
        int myIndex = getMyIndex(guards);
        int myRole = getFormationRolePriority(this.guard);

        double cos = Math.cos(facingAngle);
        double sin = Math.sin(facingAngle);
        // Perpendicular to facing direction (for horizontal line)
        double perpCos = Math.cos(facingAngle + Math.PI / 2);
        double perpSin = Math.sin(facingAngle + Math.PI / 2);

        double spacing = GuardConfig.COMMON.formationSpacing;
        int lineSize = (int) guards.stream().filter(g -> getFormationRolePriority(g) <= 2).count();
        if (lineSize < 1) lineSize = 1;

        if (myRole <= 2) {
            // Front line: shield bearers and swords
            int posInLine = myIndex;
            int centerOffset = posInLine - lineSize / 2;
            double lateralOffset = centerOffset * spacing;

            return new Vec3(
                    leader.getX() + perpCos * lateralOffset + cos * 1.0D,
                    leader.getY(),
                    leader.getZ() + perpSin * lateralOffset + sin * 1.0D
            );
        } else if (myRole == 4) {
            // Ranged: 4 blocks behind the shield line
            int rangedIndex = myIndex - lineSize;
            int rangedLineSize = (int) guards.stream().filter(g -> getFormationRolePriority(g) == 4).count();
            int centerOffset = rangedIndex - rangedLineSize / 2;
            double lateralOffset = centerOffset * spacing;

            return new Vec3(
                    leader.getX() + perpCos * lateralOffset - cos * 4.0D,
                    leader.getY(),
                    leader.getZ() + perpSin * lateralOffset - sin * 4.0D
            );
        } else {
            // Heavy weapons (axe, mace): 2 blocks behind, ready to charge
            return new Vec3(
                    leader.getX() - cos * 2.0D,
                    leader.getY(),
                    leader.getZ() - sin * 2.0D
            );
        }
    }

    /**
     * PHALANX: Shield wall in front, spears behind attacking over shields.
     * Ranged further behind. Classic Greek phalanx formation.
     */
    private Vec3 calculatePhalanxPosition(List<Guard> guards, Guard leader, double facingAngle) {
        int myRole = getFormationRolePriority(this.guard);

        double cos = Math.cos(facingAngle);
        double sin = Math.sin(facingAngle);
        double perpCos = Math.cos(facingAngle + Math.PI / 2);
        double perpSin = Math.sin(facingAngle + Math.PI / 2);

        double spacing = GuardConfig.COMMON.formationSpacing;

        if (myRole == 0) {
            // Shield bearers: front line
            int shieldCount = (int) guards.stream().filter(g -> getFormationRolePriority(g) == 0).count();
            int shieldIndex = 0;
            for (Guard g : guards) {
                if (g == this.guard) break;
                if (getFormationRolePriority(g) == 0) shieldIndex++;
            }
            int centerOffset = shieldIndex - shieldCount / 2;

            return new Vec3(
                    leader.getX() + perpCos * centerOffset * spacing + cos * 1.0D,
                    leader.getY(),
                    leader.getZ() + perpSin * centerOffset * spacing + sin * 1.0D
            );
        } else if (myRole == 1) {
            // Spears: directly behind a shield bearer, attacking over their shoulder
            int spearIndex = 0;
            for (Guard g : guards) {
                if (g == this.guard) break;
                if (getFormationRolePriority(g) == 1) spearIndex++;
            }
            int shieldCount = (int) guards.stream().filter(g -> getFormationRolePriority(g) == 0).count();
            int centerOffset = spearIndex - shieldCount / 2;

            return new Vec3(
                    leader.getX() + perpCos * centerOffset * spacing - cos * 1.0D,
                    leader.getY(),
                    leader.getZ() + perpSin * centerOffset * spacing - sin * 1.0D
            );
        } else if (myRole == 4) {
            // Ranged: far behind the phalanx
            return new Vec3(
                    leader.getX() - cos * 6.0D,
                    leader.getY(),
                    leader.getZ() - sin * 6.0D
            );
        } else {
            // Others: second rank
            return new Vec3(
                    leader.getX() - cos * 2.0D,
                    leader.getY(),
                    leader.getZ() - sin * 2.0D
            );
        }
    }

    /**
     * ARROW_LINE: Ranged guards form a horizontal line for maximum firing arc.
     * Melee guards protect the flanks.
     */
    private Vec3 calculateArrowLinePosition(List<Guard> guards, Guard leader, double facingAngle) {
        int myRole = getFormationRolePriority(this.guard);

        double cos = Math.cos(facingAngle);
        double sin = Math.sin(facingAngle);
        double perpCos = Math.cos(facingAngle + Math.PI / 2);
        double perpSin = Math.sin(facingAngle + Math.PI / 2);

        double spacing = GuardConfig.COMMON.formationSpacing;

        if (myRole == 4) {
            // Ranged: horizontal line, evenly spaced
            int rangedCount = (int) guards.stream().filter(g -> getFormationRolePriority(g) == 4).count();
            int rangedIndex = 0;
            for (Guard g : guards) {
                if (g == this.guard) break;
                if (getFormationRolePriority(g) == 4) rangedIndex++;
            }
            int centerOffset = rangedIndex - rangedCount / 2;

            return new Vec3(
                    leader.getX() + perpCos * centerOffset * spacing,
                    leader.getY(),
                    leader.getZ() + perpSin * centerOffset * spacing
            );
        } else {
            // Melee: flank guards, one on each side
            int meleeCount = (int) guards.stream().filter(g -> getFormationRolePriority(g) != 4).count();
            int meleeIndex = 0;
            for (Guard g : guards) {
                if (g == this.guard) break;
                if (getFormationRolePriority(g) != 4) meleeIndex++;
            }

            // Alternate: left flank, right flank, left flank, etc.
            boolean leftFlank = meleeIndex % 2 == 0;
            int flankPos = (meleeIndex / 2) + 1;
            double lateralOffset = (leftFlank ? -1 : 1) * (flankPos * spacing + 2.0D);

            return new Vec3(
                    leader.getX() + perpCos * lateralOffset + cos * 2.0D,
                    leader.getY(),
                    leader.getZ() + perpSin * lateralOffset + sin * 2.0D
            );
        }
    }

    /**
     * WEDGE: V-shape formation for charging. Berserkers at the tip.
     */
    private Vec3 calculateWedgePosition(List<Guard> guards, Guard leader, double facingAngle) {
        int myIndex = getMyIndex(guards);

        double cos = Math.cos(facingAngle);
        double sin = Math.sin(facingAngle);
        double perpCos = Math.cos(facingAngle + Math.PI / 2);
        double perpSin = Math.sin(facingAngle + Math.PI / 2);

        double spacing = GuardConfig.COMMON.formationSpacing;

        if (myIndex == 0) {
            // Tip of the wedge (leader/berserker)
            return new Vec3(
                    leader.getX() + cos * 3.0D,
                    leader.getY(),
                    leader.getZ() + sin * 3.0D
            );
        }

        // Each row of the V has one more guard on each side
        int row = 1;
        int posInRow = myIndex - 1;
        while (posInRow >= row * 2) {
            posInRow -= row * 2;
            row++;
        }

        boolean isLeft = posInRow < row;
        int sideOffset = isLeft ? -(posInRow + 1) : (posInRow - row + 1);

        return new Vec3(
                leader.getX() + perpCos * sideOffset * spacing - cos * row * spacing,
                leader.getY(),
                leader.getZ() + perpSin * sideOffset * spacing - sin * row * spacing
        );
    }

    /**
     * SKIRMISH: Loose formation. Guards spread out but maintain general cohesion.
     * No strict positioning — just stay within range of each other.
     */
    private Vec3 calculateSkirmishPosition(List<Guard> guards, Guard leader, double facingAngle) {
        int myIndex = getMyIndex(guards);

        // Spread guards in a loose arc around the leader
        double arcAngle = facingAngle + (myIndex - guards.size() / 2.0D) * 0.5D;
        double radius = 3.0D + myIndex * 0.5D;

        return new Vec3(
                leader.getX() + Math.cos(arcAngle) * radius,
                leader.getY(),
                leader.getZ() + Math.sin(arcAngle) * radius
        );
    }

    public FormationType getCurrentFormation() {
        return this.currentFormation;
    }

    /**
     * Validate that a formation position has solid ground beneath it.
     * If the position is floating or inside a block, try to find the nearest
     * ground level. Returns null if no valid ground position can be found.
     */
    private Vec3 validateGroundPosition(Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos.x, pos.y, pos.z);

        // Check if there is solid ground beneath (within 3 blocks down)
        for (int i = 0; i <= 3; i++) {
            BlockPos below = blockPos.below(i);
            var belowState = guard.level().getBlockState(below);
            if (!belowState.isAir() && belowState.isSolid()) {
                // Found solid ground — return position on top of it
                return new Vec3(pos.x, below.getY() + 1.0D, pos.z);
            }
        }

        // Also check upward (in case the calculated position is inside a block)
        for (int i = 1; i <= 3; i++) {
            BlockPos above = blockPos.above(i);
            BlockPos below = above.below();
            var belowState = guard.level().getBlockState(below);
            var aboveState = guard.level().getBlockState(above);
            if (!belowState.isAir() && belowState.isSolid() && aboveState.isAir()) {
                return new Vec3(pos.x, below.getY() + 1.0D, pos.z);
            }
        }

        // No valid ground found — return the original position and let the
        // pathfinder handle it (it will fail gracefully and the guard won't move)
        return pos;
    }
}
