package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.List;

public class GuardMountHorseGoal extends Goal {
    private final Guard guard;
    private LivingEntity targetMount;
    private int cooldown = 0;

    public GuardMountHorseGoal(Guard guard) {
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.guardsAutoMountHorses) return false;
        if (guard.isVehicle()) return false;
        if (guard.isBaby()) return false;
        if (this.cooldown > 0) { this.cooldown--; return false; }
        // BUG FIX: Don't try to mount horses while in combat, even if not following.
        // Previously guards would walk away from fights to mount a horse.
        if (guard.getTarget() != null) return false;
        // PERFORMANCE: Only check every 200 ticks (10 seconds) instead of 100
        if (guard.tickCount % 200 != 0) return false;

        // Search for any rideable entity nearby (horses, donkeys, etc.)
        List<LivingEntity> mounts = guard.level().getEntitiesOfClass(
                LivingEntity.class, guard.getBoundingBox().inflate(8),
                e -> e.isAlive() && !e.isVehicle() && e.getType().toString().contains("horse") && !(e instanceof net.minecraft.world.entity.player.Player)
        );
        if (mounts.isEmpty()) return false;
        this.targetMount = mounts.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetMount != null && targetMount.isAlive() && !targetMount.isVehicle() && guard.distanceTo(targetMount) > 2.0D;
    }

    @Override
    public void stop() {
        this.targetMount = null;
        this.cooldown = 200;
    }

    @Override
    public void tick() {
        if (targetMount == null) return;
        guard.getNavigation().moveTo(targetMount, 1.0D);
        if (guard.distanceTo(targetMount) <= 2.0D) {
            guard.startRiding(targetMount, true, true);
            this.targetMount = null;
        }
    }
}
