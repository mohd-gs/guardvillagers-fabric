package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.pathfinder.Path;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Goal that allows guards to detect, walk toward, and pick up better equipment
 * and food from the ground.
 * <p>
 * v3.3.2 changes:
 * - COMPLETE REWRITE: Guard now WALKS toward items instead of only picking up
 *   what's already within 3 blocks. Previously guards would never approach items.
 * - FOOD PICKUP: Guards now actively seek and pick up food from the ground,
 *   even if they already have a shield in offhand. Food is stored in offhand
 *   (shield is moved to dropped if needed). Guards prioritize food when wounded.
 * - PEACETIME BEHAVIOR: During peacetime (no combat target), guards will
 *   actively approach items within 8 blocks and walk to pick them up.
 * - COMBAT BEHAVIOR: During combat, guards only pick up items within 3 blocks
 *   (don't walk away from enemies to grab loot).
 * - CONSISTENCY: All guards now reliably pick up better equipment. The old
 *   one-shot pattern was unreliable because goal priority conflicts caused
 *   many guards to never execute the pickup logic.
 * - Movement uses pathfinding (navigation.moveTo) for smooth walking around obstacles.
 */
public class PickupBetterEquipmentGoal extends Goal {
    private final Guard guard;
    private int cooldown = 0;
    private ItemEntity targetItem = null;
    private int pathRetryCooldown = 0;
    private static final double PICKUP_DISTANCE = 1.8D;  // Distance at which item is grabbed
    private static final double PEACETIME_SCAN_RANGE = 8.0D;  // Detection range when idle
    private static final double COMBAT_SCAN_RANGE = 3.0D;  // Detection range during combat
    private static final double WALK_SPEED = 0.7D;  // Walking speed toward items

    public PickupBetterEquipmentGoal(Guard guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.autoEquipmentUpgrade) return false;
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        if (guard.isBaby()) return false;
        // Don't pick up equipment while following (player might be leading them away)
        if (guard.isFollowing()) return false;
        // Don't interrupt eating
        if (guard.isEating()) return false;
        // Check every 10 ticks (0.5 seconds) for responsive pickup
        if (guard.tickCount % 10 != 0) return false;

        double range = isInCombat() ? COMBAT_SCAN_RANGE : PEACETIME_SCAN_RANGE;
        List<ItemEntity> items = guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(range),
                ie -> !ie.getItem().isEmpty() && ie.isAlive() && isInteresting(ie.getItem())
        );
        if (items.isEmpty()) return false;

        // Pick the closest interesting item
        items.sort(Comparator.comparingDouble(guard::distanceToSqr));
        this.targetItem = items.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return false;
        }
        if (guard.isFollowing() || guard.isEating()) {
            return false;
        }
        // If we're in combat now and the item is far away, give up
        if (isInCombat() && guard.distanceToSqr(targetItem) > COMBAT_SCAN_RANGE * COMBAT_SCAN_RANGE) {
            return false;
        }
        // Don't chase items forever — timeout after 10 seconds
        if (this.pathRetryCooldown > 200) {
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.pathRetryCooldown = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        this.targetItem = null;
        this.pathRetryCooldown = 0;
        guard.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.pathRetryCooldown++;

        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return;
        }

        double distSq = guard.distanceToSqr(targetItem);

        // Close enough to pick up
        if (distSq < PICKUP_DISTANCE * PICKUP_DISTANCE) {
            if (tryEquipBetter(targetItem.getItem())) {
                targetItem.discard();
                this.cooldown = 20; // 1 second cooldown after successful pickup
            } else {
                // Item wasn't useful after all — ignore it for a while
                this.cooldown = 60; // 3 seconds
            }
            this.targetItem = null;
            return;
        }

        // Continue walking toward the item
        // Re-path every 20 ticks or if we've arrived at our current path node
        if (this.pathRetryCooldown % 20 == 0 || guard.getNavigation().isDone()) {
            moveToTarget();
        }
    }

    private void moveToTarget() {
        if (targetItem == null) return;
        guard.getNavigation().moveTo(targetItem, WALK_SPEED);
    }

    /**
     * Check if the guard is currently in combat (has a living target and is aggressive).
     */
    private boolean isInCombat() {
        return guard.getTarget() != null && guard.getTarget().isAlive() && guard.isAggressive();
    }

    /**
     * Check if an item on the ground is worth the guard's attention.
     * This filters out junk items so guards don't chase worthless things.
     */
    private boolean isInteresting(ItemStack stack) {
        // Weapons
        if (isWeaponish(stack)) return true;
        // Armor
        Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            return true;
        }
        // Shields
        if (stack.getItem() instanceof ShieldItem) return true;
        // Food — always interesting! Guards need food to heal.
        if (stack.has(net.minecraft.core.component.DataComponents.FOOD)) return true;
        return false;
    }

    /**
     * Try to equip an item if it's better than what the guard currently has.
     * Returns true if the item was picked up (equipped).
     */
    private boolean tryEquipBetter(ItemStack ground) {
        // 1. Try weapon slot (mainhand = slot 5)
        if (isWeaponish(ground)) {
            ItemStack current = guard.guardInventory.getItem(5);
            if (current.isEmpty() || getWeaponTier(ground) > getWeaponTier(current)) {
                dropOld(current);
                guard.setItemSlot(EquipmentSlot.MAINHAND, ground.copy());
                return true;
            }
        }

        // 2. Try armor slots (0-3)
        Equippable equippable = ground.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                int invIndex = Guard.slotToInventoryIndex(slot);
                ItemStack current = guard.guardInventory.getItem(invIndex);
                if (current.isEmpty() || getArmorTier(ground) > getArmorTier(current)) {
                    dropOld(current);
                    guard.setItemSlot(slot, ground.copy());
                    return true;
                }
            }
        }

        // 3. Handle offhand items (shield and food)
        boolean isShield = ground.getItem() instanceof ShieldItem;
        boolean isFood = ground.has(net.minecraft.core.component.DataComponents.FOOD);

        if (isShield || isFood) {
            ItemStack currentOffhand = guard.guardInventory.getItem(4);

            // Case A: Empty offhand — always pick up
            if (currentOffhand.isEmpty()) {
                guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                return true;
            }

            // Case B: We found a shield
            if (isShield) {
                boolean currentIsShield = currentOffhand.getItem() instanceof ShieldItem;
                if (!currentIsShield) {
                    // Currently holding food — swap for shield (shields save lives in combat)
                    // But keep the food by dropping it gently (another guard might pick it up)
                    dropOld(currentOffhand);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }
                // Both shields — pick up the new one if enchanted and old one isn't
                if (ground.isEnchanted() && !currentOffhand.isEnchanted()) {
                    dropOld(currentOffhand);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }
                // Don't pick up a duplicate shield
                return false;
            }

            // Case C: We found food
            if (isFood) {
                boolean currentIsShield = currentOffhand.getItem() instanceof ShieldItem;
                boolean currentIsFood = isFoodItem(currentOffhand);

                // If guard is wounded (below 75% health) and current offhand is a shield,
                // temporarily swap: drop shield, pick up food (health is more important!)
                if (currentIsShield && guard.getHealth() < guard.getMaxHealth() * 0.75D) {
                    dropOld(currentOffhand);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }

                // If current offhand is food, swap for better food
                if (currentIsFood) {
                    int groundFood = getFoodValue(ground);
                    int currentFood = getFoodValue(currentOffhand);
                    if (groundFood > currentFood) {
                        dropOld(currentOffhand);
                        guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                        return true;
                    }
                    // Same or worse food — skip
                    return false;
                }

                // If current offhand is something else weird, replace with food
                if (!currentIsShield && !currentIsFood) {
                    dropOld(currentOffhand);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }
            }
        }

        return false;
    }

    private void dropOld(ItemStack old) {
        if (!old.isEmpty() && guard.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            guard.spawnAtLocation(sl, old);
        }
    }

    /**
     * Check if an item is food using DataComponents.FOOD.
     */
    private static boolean isFoodItem(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.FOOD);
    }

    private boolean isWeaponish(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ProjectileWeaponItem || item instanceof AxeItem || item instanceof TridentItem) return true;
        if (item instanceof MaceItem) return true;
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        return itemId.contains("_sword");
    }

    /**
     * Get the hunger restoration value of a food item for comparison.
     */
    private int getFoodValue(ItemStack stack) {
        net.minecraft.world.food.FoodProperties food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        if (food != null) {
            return food.nutrition();
        }
        return 0;
    }

    /**
     * Get the tier level of a weapon based on its material.
     */
    private int getWeaponTier(ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (itemId.contains("netherite_sword") || itemId.contains("netherite_axe")) return 6;
        if (itemId.contains("diamond_sword") || itemId.contains("diamond_axe")) return 5;
        if (itemId.contains("iron_sword") || itemId.contains("iron_axe")) return 4;
        if (itemId.contains("copper_sword") || itemId.contains("copper_axe")) return 4;
        if (itemId.contains("stone_sword") || itemId.contains("stone_axe")) return 3;
        if (itemId.contains("golden_sword") || itemId.contains("golden_axe")) return 2;
        if (itemId.contains("wooden_sword") || itemId.contains("wooden_axe")) return 2;
        if (stack.getItem() instanceof MaceItem) return 5;
        if (stack.getItem() instanceof TridentItem) return 5;
        if (stack.getItem() instanceof CrossbowItem) return 4;
        if (stack.getItem() instanceof BowItem) return 3;
        return stack.getRarity().ordinal() + (stack.isEnchanted() ? 1 : 0);
    }

    /**
     * Get the tier level of armor based on its material.
     */
    private int getArmorTier(ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
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
