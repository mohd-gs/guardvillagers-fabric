package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.entity.EquipmentSlot;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that makes guards WALK toward food and equipment items on the ground
 * during peacetime (no combat target).
 * <p>
 * This goal is separate from PickupBetterEquipmentGoal because:
 * - PickupBetterEquipmentGoal handles the actual EQUIP logic (is this item better?)
 * - This goal handles the MOVEMENT logic (walk toward items when idle)
 * <p>
 * Why a separate goal?
 * - During combat, guards should NOT walk away from enemies to grab items.
 * - During peacetime, guards should actively seek out items within 10 blocks.
 * - Having a dedicated peacetime walking goal ensures consistent behavior
 *   without interfering with combat priorities.
 * <p>
 * Flow: GuardWalkToItemGoal walks the guard near the item →
 *       PickupBetterEquipmentGoal detects the item within range → equips it.
 * <p>
 * If the guard is wounded and sees food, it will prioritize walking to food.
 * If the guard sees better equipment, it will walk to that too.
 */
public class GuardWalkToItemGoal extends Goal {
    private final Guard guard;
    private ItemEntity targetItem = null;
    private int scanCooldown = 0;
    private int timeoutCounter = 0;
    private static final double SCAN_RANGE = 10.0D;  // How far to detect items
    private static final double ARRIVAL_DISTANCE = 2.0D;  // Close enough for PickupBetterEquipmentGoal to grab
    private static final double WALK_SPEED = 0.65D;  // Peacetime walking speed (not too rushed)
    private static final int TIMEOUT_TICKS = 200;  // Give up after 10 seconds of walking

    public GuardWalkToItemGoal(Guard guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.autoEquipmentUpgrade) return false;
        if (guard.isBaby()) return false;
        // Only during peacetime — don't walk toward items during combat
        if (guard.getTarget() != null && guard.isAggressive()) return false;
        // Don't interrupt eating
        if (guard.isEating()) return false;
        // Don't walk to items while following a player
        if (guard.isFollowing()) return false;

        // Scan every 30 ticks (1.5 seconds) to reduce performance impact
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
            return false;
        }
        this.scanCooldown = 30;

        // Look for interesting items on the ground
        List<ItemEntity> items = guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(SCAN_RANGE),
                ie -> !ie.getItem().isEmpty() && ie.isAlive() && isDesirable(ie.getItem())
        );

        if (items.isEmpty()) return false;

        // Sort by priority: wounded guards prioritize food, then by distance
        items.sort((a, b) -> {
            boolean aFood = isFoodItem(a.getItem());
            boolean bFood = isFoodItem(b.getItem());
            // Wounded guards (below 75% health) prioritize food
            if (guard.getHealth() < guard.getMaxHealth() * 0.75D) {
                if (aFood && !bFood) return -1;
                if (!aFood && bFood) return 1;
            }
            // Then sort by distance (closer first)
            return Double.compare(guard.distanceToSqr(a), guard.distanceToSqr(b));
        });

        this.targetItem = items.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetItem == null || !this.targetItem.isAlive()) return false;
        if (guard.getTarget() != null && guard.isAggressive()) return false;
        if (guard.isFollowing() || guard.isEating()) return false;
        // Timeout — don't chase items forever
        if (this.timeoutCounter > TIMEOUT_TICKS) return false;
        // Close enough — PickupBetterEquipmentGoal should handle it now
        double distSq = guard.distanceToSqr(targetItem);
        if (distSq < ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) return false;
        return true;
    }

    @Override
    public void start() {
        this.timeoutCounter = 0;
        guard.getNavigation().moveTo(targetItem, WALK_SPEED);
    }

    @Override
    public void stop() {
        this.targetItem = null;
        this.timeoutCounter = 0;
        guard.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.timeoutCounter++;

        if (this.targetItem == null || !this.targetItem.isAlive()) return;

        // Look at the item while walking
        guard.getLookControl().setLookAt(targetItem.getX(), targetItem.getY(), targetItem.getZ());

        // Re-path every 20 ticks for smoother navigation around obstacles
        if (this.timeoutCounter % 20 == 0 || guard.getNavigation().isDone()) {
            double distSq = guard.distanceToSqr(targetItem);
            if (distSq > ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
                guard.getNavigation().moveTo(targetItem, WALK_SPEED);
            }
        }
    }

    /**
     * Check if an item is desirable for the guard to walk toward.
     * This is different from "isInteresting" in PickupBetterEquipmentGoal:
     * - isDesirable: Should I walk toward this? (broader — include things I might not equip)
     * - isInteresting: Should I equip this? (stricter — only things better than current gear)
     */
    private boolean isDesirable(ItemStack stack) {
        // Food — always desirable, especially if guard has no food or is wounded
        if (isFoodItem(stack)) {
            // If guard has no food in offhand, definitely walk to it
            ItemStack offhand = guard.guardInventory.getItem(4);
            if (!isFoodItem(offhand)) return true;
            // If guard has food but this is better, walk to it
            if (isFoodItem(offhand)) {
                return getFoodValue(stack) > getFoodValue(offhand);
            }
        }

        // Shields — desirable if guard doesn't have one
        if (stack.getItem() instanceof ShieldItem) {
            ItemStack offhand = guard.guardInventory.getItem(4);
            return !(offhand.getItem() instanceof ShieldItem);
        }

        // Armor — desirable if better than current
        Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            int invIndex = Guard.slotToInventoryIndex(equippable.slot());
            ItemStack current = guard.guardInventory.getItem(invIndex);
            return current.isEmpty() || getArmorTierSimple(stack) > getArmorTierSimple(current);
        }

        // Weapons — desirable if better than current
        if (isWeaponishSimple(stack)) {
            ItemStack current = guard.guardInventory.getItem(5);
            return current.isEmpty() || getWeaponTierSimple(stack) > getWeaponTierSimple(current);
        }

        return false;
    }

    private static boolean isFoodItem(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.FOOD);
    }

    private static int getFoodValue(ItemStack stack) {
        net.minecraft.world.food.FoodProperties food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        return food != null ? food.nutrition() : 0;
    }

    private static boolean isWeaponishSimple(ItemStack stack) {
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof net.minecraft.world.item.ProjectileWeaponItem
                || item instanceof net.minecraft.world.item.AxeItem
                || item instanceof net.minecraft.world.item.TridentItem
                || item instanceof net.minecraft.world.item.MaceItem) return true;
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
        return itemId.contains("_sword");
    }

    private static int getWeaponTierSimple(ItemStack stack) {
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (itemId.contains("netherite_sword") || itemId.contains("netherite_axe")) return 6;
        if (itemId.contains("diamond_sword") || itemId.contains("diamond_axe")) return 5;
        if (itemId.contains("iron_sword") || itemId.contains("iron_axe")) return 4;
        if (itemId.contains("copper_sword") || itemId.contains("copper_axe")) return 4;
        if (itemId.contains("stone_sword") || itemId.contains("stone_axe")) return 3;
        if (itemId.contains("golden_sword") || itemId.contains("golden_axe")) return 2;
        if (itemId.contains("wooden_sword") || itemId.contains("wooden_axe")) return 2;
        if (stack.getItem() instanceof net.minecraft.world.item.MaceItem) return 5;
        if (stack.getItem() instanceof net.minecraft.world.item.TridentItem) return 5;
        if (stack.getItem() instanceof net.minecraft.world.item.CrossbowItem) return 4;
        if (stack.getItem() instanceof net.minecraft.world.item.BowItem) return 3;
        return stack.getRarity().ordinal() + (stack.isEnchanted() ? 1 : 0);
    }

    private static int getArmorTierSimple(ItemStack stack) {
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (itemId.contains("netherite")) return 6;
        if (itemId.contains("diamond")) return 5;
        if (itemId.contains("iron")) return 4;
        if (itemId.contains("copper")) return 4;
        if (itemId.contains("chainmail") || itemId.contains("chain")) return 3;
        if (itemId.contains("turtle")) return 3;
        if (itemId.contains("golden")) return 2;
        if (itemId.contains("leather")) return 1;
        return stack.getRarity().ordinal() + (stack.isEnchanted() ? 1 : 0);
    }
}
