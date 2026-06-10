package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
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
        // Don't retreat if wounded - the wounded behavior handles retreat separately
        if (guard.isWounded()) return false;
        LivingEntity target = guard.getTarget();
        if (target == null) return false;
        // PERFORMANCE: Only check distance every 5 ticks
        if (guard.tickCount % 5 != 0) return false;
        return guard.distanceTo(target) < GuardConfig.COMMON.archerRetreatDistance;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
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
