package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

public class GuardRetreatGoal extends Goal {
    private final Guard guard;

    public GuardRetreatGoal(Guard guard) {
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.weaponSpecialization) return false;
        if (!guard.isHoldingRangedWeapon()) return false;
        // Bow guards handle retreat within GuardBowAttack (kiting while shooting).
        // This goal handles retreat for crossbow and trident guards only.
        if (guard.getMainHandItem().getItem() instanceof BowItem) return false;
        // Don't retreat if wounded - the wounded behavior handles retreat separately
        if (guard.isWounded()) return false;
        LivingEntity target = guard.getTarget();
        if (target == null) return false;
        // PERFORMANCE: Only check distance every 10 ticks
        if (guard.tickCount % 10 != 0) return false;
        return guard.distanceTo(target) < GuardConfig.COMMON.archerRetreatDistance;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = guard.getTarget();
        if (target == null) return false;
        if (!guard.isHoldingRangedWeapon()) return false;
        // Continue retreating until we're at a safe distance (1.5x the retreat threshold)
        return guard.distanceTo(target) < GuardConfig.COMMON.archerRetreatDistance * 1.5D;
    }

    @Override
    public void tick() {
        LivingEntity target = guard.getTarget();
        if (target == null) return;
        // Move away from target
        Vec3 away = DefaultRandomPos.getPosAway(guard, 8, 4, target.position());
        if (away != null) {
            guard.getNavigation().moveTo(away.x, away.y, away.z, 1.2D);
        }
    }
}
