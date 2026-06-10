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

public class GuardMountHorseGoal extends Goal {
    private final Guard guard;
    private LivingEntity targetMount;
    private int cooldown = 0;

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
        // PERFORMANCE: Only check every 400 ticks (20 seconds) instead of 200.
        // Horse mounting is very low priority - guards don't need to check
        // frequently for available horses.
        if (guard.tickCount % 400 != 0) return false;

        // Search for any rideable entity nearby (horses, donkeys, etc.)
        // PERFORMANCE: Use a smaller search radius (6 blocks instead of 8) and
        // check the entity type via EntityType comparison instead of string contains.
        List<LivingEntity> mounts = guard.level().getEntitiesOfClass(
                LivingEntity.class, guard.getBoundingBox().inflate(6),
                e -> e.isAlive() && !e.isVehicle() && isMountableType(e) && !(e instanceof net.minecraft.world.entity.player.Player)
        );
        if (mounts.isEmpty()) return false;
        this.targetMount = mounts.get(0);
        return true;
    }

    // PERFORMANCE: Replace string-contains check with proper type check.
    // The old code used e.getType().toString().contains("horse") which is slow
    // and imprecise (would match "seahorse" or any modded entity with "horse" in name).
    private static boolean isMountableType(LivingEntity e) {
        return e.getType() == EntityType.HORSE
                || e.getType() == EntityType.DONKEY
                || e.getType() == EntityType.MULE
                || e.getType() == EntityType.SKELETON_HORSE
                || e.getType() == EntityType.ZOMBIE_HORSE;
    }

    @Override
    public boolean canContinueToUse() {
        return targetMount != null && targetMount.isAlive() && !targetMount.isVehicle() && guard.distanceTo(targetMount) > 2.0D;
    }

    @Override
    public void stop() {
        this.targetMount = null;
        this.cooldown = 400; // 20 second cooldown after failing
    }

    @Override
    public void tick() {
        if (targetMount == null) return;
        guard.getNavigation().moveTo(targetMount, 1.0D);
        if (guard.distanceTo(targetMount) <= 2.0D) {
            boolean success = guard.startRiding(targetMount, true, true);
            if (success) {
                // Feature 6 (Advanced Cavalry): Equip horse armor from guard's inventory
                equipHorseArmor(targetMount);
            }
            this.targetMount = null;
        }
    }

    /**
     * Feature 6 (Advanced Cavalry): When a guard mounts a horse,
     * check if they have spare armor in inventory and equip the best on the horse.
     */
    private void equipHorseArmor(LivingEntity mount) {
        if (!(mount instanceof AbstractHorse horse)) return;

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
            // Equip the armor on the horse using the proper method
            horse.equipBodyArmor(null, bestArmor.copy());
            // Remove from guard's inventory
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
