package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.List;

/**
 * Goal that allows guards to share food with nearby wounded guards.
 * <p>
 * Mechanics:
 * - A guard with food in offhand checks for a nearby guard (within 10 blocks)
 *   whose health is below 50% and who has no food.
 * - The sharing guard drops half its food (rounded down) as an item entity
 *   near the wounded guard, who will then pick it up via PickupBetterEquipmentGoal.
 * - Only ONE guard is helped per check cycle.
 * - Does NOT activate during combat (guard has a target and is aggressive).
 * - 5-minute cooldown (6000 ticks) between shares per guard.
 */
public class GuardShareFoodGoal extends Goal {
    private final Guard guard;
    private int cooldown = 0;
    private static final int COOLDOWN_TICKS = 6000; // 5 minutes
    private static final double SHARE_RANGE = 10.0D;

    public GuardShareFoodGoal(Guard guard) {
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        // Must have food in offhand
        ItemStack offhand = guard.getOffhandItem();
        if (!isFood(offhand)) return false;
        // Don't share during combat
        if (guard.getTarget() != null && guard.isAggressive()) return false;
        // Don't share while following player
        if (guard.isFollowing()) return false;
        if (guard.isBaby()) return false;
        // Check every 100 ticks (5 seconds) to reduce scan overhead
        if (guard.tickCount % 100 != 0) return false;
        // Must have at least 2 items to share (so we keep at least 1)
        if (offhand.getCount() < 2) return false;
        return true;
    }

    @Override
    public void start() {
        ItemStack offhand = guard.getOffhandItem();
        if (!isFood(offhand) || offhand.getCount() < 2) return;
        if (!(guard.level() instanceof ServerLevel serverLevel)) return;

        // Find a nearby wounded guard with no food
        List<Guard> nearbyGuards = guard.level().getEntitiesOfClass(
                Guard.class,
                guard.getBoundingBox().inflate(SHARE_RANGE),
                g -> g != this.guard
                        && g.isAlive()
                        && !g.isBaby()
                        && g.getHealth() < g.getMaxHealth() * 0.5D
                        && !hasFood(g)
        );

        if (nearbyGuards.isEmpty()) {
            this.cooldown = 200; // 10 seconds — recheck sooner since there may be wounded guards
            return;
        }

        // Pick the closest wounded guard
        Guard target = nearbyGuards.get(0);
        double bestDist = guard.distanceToSqr(target);
        for (int i = 1; i < nearbyGuards.size(); i++) {
            double dist = guard.distanceToSqr(nearbyGuards.get(i));
            if (dist < bestDist) {
                bestDist = dist;
                target = nearbyGuards.get(i);
            }
        }

        // Calculate how many to share: half, rounded down, minimum 1
        int shareCount = offhand.getCount() / 2;
        if (shareCount < 1) shareCount = 1;

        // Split the stack
        ItemStack sharedStack = offhand.copy();
        sharedStack.setCount(shareCount);
        offhand.shrink(shareCount);

        // Drop the food near the wounded guard
        ItemEntity droppedFood = new ItemEntity(
                guard.level(),
                target.getX(), target.getY() + 0.5D, target.getZ(),
                sharedStack,
                0.0D, 0.15D, 0.0D
        );
        droppedFood.setPickUpDelay(0); // Allow instant pickup
        droppedFood.setUnlimitedLifetime(); // Don't despawn quickly
        serverLevel.addFreshEntity(droppedFood);

        // Set 5-minute cooldown
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean canContinueToUse() {
        return false; // One-shot goal: runs once per activation
    }

    /**
     * Check if an ItemStack is food.
     */
    private static boolean isFood(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.FOOD);
    }

    /**
     * Check if a guard has any food in their offhand.
     */
    private static boolean hasFood(Guard g) {
        return isFood(g.getOffhandItem());
    }
}
