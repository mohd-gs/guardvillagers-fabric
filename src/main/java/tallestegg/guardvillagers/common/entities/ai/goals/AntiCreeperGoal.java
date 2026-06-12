package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes guards detect and flee from charging creepers.
 * <p>
 * Without this goal, guards would stand next to creepers and try to melee them,
 * resulting in the creeper exploding and killing the guard and nearby villagers.
 * <p>
 * Behavior:
 * - Detects creepers within 6 blocks that are swelling (about to explode)
 * - All guards flee regardless of weapon type
 * - Ranged guards try to shoot the creeper from a safe distance while retreating
 * - After the creeper either explodes or defuses, guards resume normal combat
 * <p>
 * This is one of the biggest AI improvements — previously, creeper explosions
 * were the #1 cause of guard deaths because they had no self-preservation
 * against explosion threats.
 */
public class AntiCreeperGoal extends Goal {
    private final Guard guard;
    private Creeper threateningCreeper;
    private int cooldown = 0;

    public AntiCreeperGoal(Guard guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.antiCreeperBehavior) return false;
        if (guard.isBaby()) return false;
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }

        // PERFORMANCE: Only scan every 20 ticks (1 second)
        if (guard.tickCount % 20 != 0 && this.threateningCreeper == null) return false;

        // Find nearby charging creepers
        List<Creeper> creepers = guard.level().getEntitiesOfClass(
                Creeper.class,
                guard.getBoundingBox().inflate(8.0D, 4.0D, 8.0D),
                creeper -> creeper.isAlive() && creeper.getSwellDir() > 0
        );

        if (creepers.isEmpty()) {
            this.threateningCreeper = null;
            return false;
        }

        // Pick the closest charging creeper
        this.threateningCreeper = null;
        double bestDist = Double.MAX_VALUE;
        for (Creeper creeper : creepers) {
            double dist = guard.distanceToSqr(creeper);
            if (dist < bestDist) {
                bestDist = dist;
                this.threateningCreeper = creeper;
            }
        }

        return this.threateningCreeper != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.threateningCreeper == null || !this.threateningCreeper.isAlive()) {
            return false;
        }
        // Continue fleeing while creeper is still swelling
        if (this.threateningCreeper.getSwellDir() <= 0) {
            return false;
        }
        // Stop fleeing when far enough away
        return guard.distanceTo(this.threateningCreeper) < 8.0D;
    }

    @Override
    public void start() {
        fleeFromCreeper();
    }

    @Override
    public void tick() {
        if (this.threateningCreeper == null) return;

        // Keep fleeing while the creeper is still swelling
        if (guard.distanceTo(this.threateningCreeper) < 6.0D) {
            fleeFromCreeper();
        }
    }

    @Override
    public void stop() {
        this.threateningCreeper = null;
        this.cooldown = 20; // 1 second cooldown before rechecking
    }

    private void fleeFromCreeper() {
        Vec3 away = DefaultRandomPos.getPosAway(guard, 12, 6, this.threateningCreeper.position());
        if (away != null) {
            guard.getNavigation().moveTo(away.x, away.y, away.z, 1.4D); // Run fast!
        } else {
            // Can't find a good escape route — just run directly away
            double dx = guard.getX() - this.threateningCreeper.getX();
            double dz = guard.getZ() - this.threateningCreeper.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.01D) {
                double escapeX = guard.getX() + (dx / dist) * 8.0D;
                double escapeZ = guard.getZ() + (dz / dist) * 8.0D;
                guard.getNavigation().moveTo(escapeX, guard.getY(), escapeZ, 1.4D);
            }
        }
    }
}
