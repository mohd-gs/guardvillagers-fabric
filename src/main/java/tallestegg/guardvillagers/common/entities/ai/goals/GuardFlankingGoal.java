package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.EnumSet;

/**
 * Flanking behavior for guards.
 * <p>
 * Instead of all guards rushing directly at the enemy, some guards
 * should circle around to attack from the sides or behind. This:
 * <ul>
 *   <li>Makes combat more realistic and tactical</li>
 *   <li>Prevents guards from bunching up and blocking each other</li>
 *   <li>Allows guards to attack enemies from their vulnerable sides</li>
 *   <li>Creates a more dynamic and interesting battle</li>
 * </ul>
 * <p>
 * Which guards flank:
 * - Sword guards with shields: 50% chance to flank (others hold the line)
 * - Berserker axes: Always try to flank for devastating rear attacks
 * - Spear guards: Circle at extended range to find openings
 * - Ranged guards: Reposition to better angles (not true flanking)
 * <p>
 * The guard calculates a position to the side/behind the enemy and
 * navigates there before attacking.
 */
public class GuardFlankingGoal extends Goal {
    private final Guard guard;
    private Vec3 flankTarget;
    private int repositionCooldown = 0;
    private boolean isFlanking = false;

    public GuardFlankingGoal(Guard guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.weaponSpecificBehavior) return false;
        if (!GuardConfig.COMMON.guardFlanking) return false;
        if (guard.isBaby()) return false;
        if (guard.isFollowing()) return false;

        LivingEntity target = guard.getTarget();
        if (target == null || !target.isAlive()) return false;

        // Don't flank if too far from target
        if (guard.distanceTo(target) > 10.0D) return false;

        // Determine if this guard should flank based on weapon type
        WeaponBehavior.WeaponType weaponType = WeaponBehavior.getWeaponType(guard);
        if (!shouldFlank(weaponType)) return false;

        // Don't flank if already in a good flanking position
        if (isFlanking && isAtFlankPosition(target)) return false;

        // Calculate flank position
        this.flankTarget = calculateFlankPosition(target);
        return this.flankTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = guard.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (this.flankTarget == null) return false;

        // Stop if we've reached the flank position
        if (guard.distanceToSqr(this.flankTarget) < 2.0D) {
            this.isFlanking = true;
            return false; // Reached position — let melee goal take over
        }

        return true;
    }

    @Override
    public void start() {
        this.isFlanking = false;
        if (this.flankTarget != null) {
            guard.getNavigation().moveTo(this.flankTarget.x, this.flankTarget.y, this.flankTarget.z, 1.2D);
        }
    }

    @Override
    public void stop() {
        this.flankTarget = null;
        this.isFlanking = false;
        this.repositionCooldown = 40; // 2 second cooldown before next flank attempt
    }

    @Override
    public void tick() {
        if (this.flankTarget == null) return;

        // Repath every 30 ticks in case the enemy moved
        if (this.repositionCooldown <= 0) {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                this.flankTarget = calculateFlankPosition(target);
                if (this.flankTarget != null) {
                    guard.getNavigation().moveTo(this.flankTarget.x, this.flankTarget.y, this.flankTarget.z, 1.2D);
                }
            }
            this.repositionCooldown = 30;
        } else {
            this.repositionCooldown--;
        }

        // Keep facing the target
        LivingEntity target = guard.getTarget();
        if (target != null) {
            guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
    }

    private boolean shouldFlank(WeaponBehavior.WeaponType weaponType) {
        // Check if there are other guards already engaging from the front
        // (We don't need to flank if we're the only one fighting)
        long nearbyFightingGuards = guard.level().getEntitiesOfClass(
                Guard.class,
                guard.getBoundingBox().inflate(10.0D, 4.0D, 10.0D),
                g -> g != this.guard && g.isAlive() && g.getTarget() != null
        ).size();

        if (nearbyFightingGuards < 1) return false; // No one holding the front

        return switch (weaponType) {
            case SWORD -> guard.getRandom().nextFloat() < 0.5F; // 50% chance
            case AXE -> guard.isBerserker(); // Berserkers always flank
            case SPEAR -> guard.getRandom().nextFloat() < 0.6F; // 60% chance
            default -> false;
        };
    }

    /**
     * Calculate a position to the side or behind the target.
     * Uses the target's facing direction to determine "behind".
     */
    private Vec3 calculateFlankPosition(LivingEntity target) {
        // Get the direction the enemy is facing
        float enemyYaw = target.getYRot() * ((float) Math.PI / 180F);

        // Pick left or right flank based on guard ID for consistency
        boolean goLeft = (guard.getId() % 2 == 0);

        // Flank angle: 90-135 degrees to the side of the enemy
        double flankAngle = enemyYaw + (goLeft ? Math.PI * 0.6 : -Math.PI * 0.6);

        // Distance: melee guards get close, ranged stay further
        WeaponBehavior.WeaponType weaponType = WeaponBehavior.getWeaponType(guard);
        double flankDistance = weaponType.isMelee() ? 3.0D : 6.0D;

        double targetX = target.getX() + Math.cos(flankAngle) * flankDistance;
        double targetZ = target.getZ() + Math.sin(flankAngle) * flankDistance;

        // Find a safe position near our calculated point
        Vec3 candidate = new Vec3(targetX, target.getY(), targetZ);
        Vec3 safePos = DefaultRandomPos.getPosTowards(guard, 8, 4, candidate, (float) Math.PI / 4F);

        return safePos != null ? safePos : candidate;
    }

    private boolean isAtFlankPosition(LivingEntity target) {
        if (this.flankTarget == null) return false;
        return guard.distanceToSqr(this.flankTarget) < 4.0D; // Within 2 blocks
    }
}
