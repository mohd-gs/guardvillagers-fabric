package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes guards automatically mount nearby horses during peacetime.
 * <p>
 * v3.3.7 fixes:
 * - CRITICAL FIX: canContinueToUse() previously returned false when the guard
 *   was within 2 blocks of the horse (distance > 2.0D check). This meant that
 *   as soon as the guard reached the horse, the goal would stop and call stop()
 *   with a 400-tick cooldown, BEFORE tick() could execute the mounting code.
 *   The guard NEVER actually mounted the horse! Now canContinueToUse() returns
 *   true when close (so tick() can mount), and only returns false after a
 *   successful mount or if the horse becomes invalid.
 * - Tame the horse when mounting (otherwise the horse might buck the guard off).
 * - Reduced scan interval from 400 to 200 ticks (10s → still low priority but
 *   not so sluggish).
 */
public class GuardMountHorseGoal extends Goal {
    private final Guard guard;
    private LivingEntity targetMount;
    private int cooldown = 0;
    private boolean mounted = false;

    public GuardMountHorseGoal(Guard guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.guardsAutoMountHorses) return false;
        if (guard.isVehicle()) return false;
        if (guard.isBaby()) return false;
        if (this.cooldown > 0) { this.cooldown--; return false; }
        // Don't try to mount horses while in combat or following
        if (guard.getTarget() != null) return false;
        if (guard.isFollowing()) return false;
        // PERFORMANCE: Only check every 200 ticks (10 seconds)
        if (guard.tickCount % 200 != 0) return false;

        // Search for any rideable entity nearby
        List<LivingEntity> mounts = guard.level().getEntitiesOfClass(
                LivingEntity.class, guard.getBoundingBox().inflate(8),
                e -> e.isAlive() && !e.isVehicle() && isMountableType(e) && !(e instanceof net.minecraft.world.entity.player.Player)
        );
        if (mounts.isEmpty()) return false;
        // Pick the closest mount
        this.targetMount = mounts.stream()
                .min((a, b) -> Double.compare(guard.distanceTo(a), guard.distanceTo(b)))
                .orElse(null);
        return this.targetMount != null;
    }

    private static boolean isMountableType(LivingEntity e) {
        return e.getType() == EntityType.HORSE
                || e.getType() == EntityType.DONKEY
                || e.getType() == EntityType.MULE
                || e.getType() == EntityType.SKELETON_HORSE
                || e.getType() == EntityType.ZOMBIE_HORSE;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mounted) return false; // Successfully mounted — goal is done
        if (this.targetMount == null || !this.targetMount.isAlive() || this.targetMount.isVehicle()) return false;
        // Continue even when close — tick() needs to execute the mounting code
        return true;
    }

    @Override
    public void start() {
        this.mounted = false;
        if (this.targetMount != null) {
            guard.getNavigation().moveTo(this.targetMount, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.targetMount = null;
        // Only set cooldown if we didn't successfully mount
        if (!this.mounted) {
            this.cooldown = 200; // 10 second cooldown after failing
        } else {
            this.cooldown = 0; // No cooldown after successful mount
        }
        this.mounted = false;
    }

    @Override
    public void tick() {
        if (targetMount == null) return;

        double dist = guard.distanceTo(targetMount);

        if (dist <= 2.5D) {
            // Close enough to mount!
            boolean success = guard.startRiding(targetMount, true, true);
            if (success) {
                // Tame the horse so it doesn't buck the guard off
                if (targetMount instanceof AbstractHorse horse) {
                    horse.setTamed(true);
                    // Equip horse armor from guard's inventory
                    equipHorseArmor(horse);
                }
                this.mounted = true;
            }
            this.targetMount = null;
            return;
        }

        // Walk toward the horse
        if (guard.getNavigation().isDone() || guard.tickCount % 20 == 0) {
            guard.getNavigation().moveTo(targetMount, 1.0D);
        }
    }

    /**
     * When a guard mounts a horse, check if they have spare armor in
     * inventory and equip the best on the horse.
     */
    private void equipHorseArmor(AbstractHorse horse) {
        // Find the best horse armor in the guard's inventory
        ItemStack bestArmor = ItemStack.EMPTY;
        int bestArmorSlot = -1;
        int bestArmorTier = -1;

        for (int i = 0; i < guard.guardInventory.getContainerSize(); i++) {
            ItemStack stack = guard.guardInventory.getItem(i);
            if (stack.isEmpty()) continue;
            int tier = getHorseArmorTier(stack);
            if (tier > bestArmorTier) {
                bestArmorTier = tier;
                bestArmor = stack;
                bestArmorSlot = i;
            }
        }

        if (!bestArmor.isEmpty() && bestArmorSlot >= 0) {
            horse.equipBodyArmor(null, bestArmor.copy());
            guard.guardInventory.setItem(bestArmorSlot, ItemStack.EMPTY);
        }
    }

    private static int getHorseArmorTier(ItemStack stack) {
        if (stack.is(Items.LEATHER_HORSE_ARMOR)) return 1;
        if (stack.is(Items.IRON_HORSE_ARMOR)) return 2;
        if (stack.is(Items.GOLDEN_HORSE_ARMOR)) return 3;
        if (stack.is(Items.DIAMOND_HORSE_ARMOR)) return 4;
        return -1;
    }
}
