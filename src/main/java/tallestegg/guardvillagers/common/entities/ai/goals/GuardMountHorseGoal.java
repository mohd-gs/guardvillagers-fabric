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
 * v3.3.8 fixes:
 * - CRITICAL: getTarget() check was too strict — guards often have lingering
 *   targets from previous combat (dead or far-away mobs), which permanently
 *   blocked mounting. Now checks if target is actually a threat (alive + within 16 blocks).
 * - PRIORITY FIX: Moved from priority 3 (conflicts with WalkBackToCheckPointGoal
 *   which also uses MOVE flag) to priority 5 (peacetime). Mounting only happens
 *   during idle/wandering, so it should be at a lower priority.
 * - Scan interval reduced from 200 to 100 ticks (5 seconds).
 * - Better horse selection: prefer tamed+ saddled horses.
 * - Debug-friendly: no more silent failures.
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
        if (guard.isPassenger()) return false; // Already riding something (isVehicle() checks if someone rides the GUARD, isPassenger() checks if the GUARD rides something)
        if (guard.isBaby()) return false;
        if (this.cooldown > 0) { this.cooldown--; return false; }
        // Don't mount while actively following a player
        if (guard.isFollowing()) return false;
        // Don't mount while in active combat (target is alive and nearby)
        // NOTE: We don't just check getTarget() != null because guards often
        // have lingering targets from previous combat (dead or far-away mobs).
        // This was the #1 reason mounting never worked in practice!
        if (isInActiveCombat()) return false;

        // PERFORMANCE: Only check every 100 ticks (5 seconds)
        if (guard.tickCount % 100 != 0) return false;

        // Search for any rideable entity nearby (8 blocks)
        List<LivingEntity> mounts = guard.level().getEntitiesOfClass(
                LivingEntity.class, guard.getBoundingBox().inflate(8),
                e -> e.isAlive() && !e.isVehicle() && isMountableType(e)
                        && !(e instanceof net.minecraft.world.entity.player.Player)
        );
        if (mounts.isEmpty()) return false;

        // Pick the best mount: prefer tamed+saddled, then closest
        this.targetMount = mounts.stream()
                .min((a, b) -> {
                    int scoreA = getMountScore(a);
                    int scoreB = getMountScore(b);
                    if (scoreA != scoreB) return Integer.compare(scoreB, scoreA); // Higher score first
                    return Double.compare(guard.distanceTo(a), guard.distanceTo(b)); // Then closer
                })
                .orElse(null);
        return this.targetMount != null;
    }

    /**
     * Score a mount candidate. Higher = better choice.
     * +100: Tamed and saddled (ready to ride, player-prepared)
     * +50: Tamed (no bucking risk)
     * +10: Has saddle (can be controlled after mounting)
     */
    private int getMountScore(LivingEntity mount) {
        int score = 0;
        if (mount instanceof AbstractHorse horse) {
            if (horse.isTamed()) score += 50;
            if (horse.isSaddled()) score += 10;
            if (horse.isTamed() && horse.isSaddled()) score += 100; // Bonus for fully ready
        }
        return score;
    }

    private static boolean isMountableType(LivingEntity e) {
        return e.getType() == EntityType.HORSE
                || e.getType() == EntityType.DONKEY
                || e.getType() == EntityType.MULE
                || e.getType() == EntityType.SKELETON_HORSE
                || e.getType() == EntityType.ZOMBIE_HORSE;
    }

    /**
     * Check if the guard is in ACTIVE combat — target exists, is alive, and is nearby.
     * This is more lenient than getTarget() != null because guards often keep
     * dead or far-away targets, which should NOT block horse mounting.
     */
    private boolean isInActiveCombat() {
        LivingEntity target = guard.getTarget();
        if (target == null) return false;
        if (!target.isAlive()) return false; // Dead target = not combat
        if (guard.distanceTo(target) > 16.0D) return false; // Far target = not active combat
        return true; // Actually fighting someone nearby
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mounted) return false; // Successfully mounted — goal is done
        if (this.targetMount == null || !this.targetMount.isAlive() || this.targetMount.isVehicle()) return false;
        // Stop if combat starts while walking to horse
        if (isInActiveCombat()) return false;
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
            this.cooldown = 100; // 5 second cooldown after failing
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
                if (targetMount instanceof AbstractHorse horse) {
                    // Tame the horse if it isn't already (prevents bucking)
                    if (!horse.isTamed()) {
                        horse.setTamed(true);
                    }
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
