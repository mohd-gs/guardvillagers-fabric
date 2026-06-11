package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.Equippable;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified goal that handles BOTH walking toward items AND picking them up.
 * <p>
 * v3.3.3 changes:
 * - MERGED with GuardWalkToItemGoal into one unified goal
 * - ITEM RESERVATION: Guards claim items so they don't all rush the same item.
 *   When Guard A is walking toward Item X, Guard B skips it and targets Item Y.
 * - MULTI-PICKUP: When a guard arrives at an item cluster, it picks up ALL
 *   useful items in range, not just one. This means if you throw 5 armor pieces,
 *   one guard will pick up multiple pieces before moving on.
 * - RE-SCAN ON LOSS: If the target item disappears (another guard took it),
 *   the guard immediately re-scans for other items instead of stopping.
 * - NO COOLDOWN AFTER SUCCESS: After picking up an item, immediately look for
 *   more items. Cooldown only applies when there are no useful items nearby.
 * - PEACETIME: Guards walk toward items up to 10 blocks away.
 * - COMBAT: Guards only grab items within 3 blocks (won't leave a fight).
 */
public class PickupBetterEquipmentGoal extends Goal {
    private final Guard guard;
    private ItemEntity targetItem = null;
    private int tickCounter = 0;
    private int noItemCooldown = 0;

    // === ITEM RESERVATION SYSTEM ===
    // Tracks which guard is targeting which item entity.
    // Key = ItemEntity UUID, Value = Guard entity ID
    // This prevents all guards from rushing the same item.
    private static final Map<UUID, Integer> reservedItems = new ConcurrentHashMap<>();

    private static final double PICKUP_DISTANCE_SQ = 1.8D * 1.8D;
    private static final double PEACETIME_SCAN_RANGE = 10.0D;
    private static final double COMBAT_SCAN_RANGE = 3.0D;
    private static final double WALK_SPEED = 0.7D;
    private static final int TIMEOUT_TICKS = 200; // 10 seconds max chase

    public PickupBetterEquipmentGoal(Guard guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.autoEquipmentUpgrade) return false;
        if (this.noItemCooldown > 0) {
            this.noItemCooldown--;
            return false;
        }
        if (guard.isBaby()) return false;
        if (guard.isFollowing()) return false;
        if (guard.isEating()) return false;
        // Check every 10 ticks (0.5 seconds)
        if (guard.tickCount % 10 != 0) return false;

        // Scan for items
        List<ItemEntity> items = scanForItems();
        if (items.isEmpty()) {
            this.noItemCooldown = 40; // 2 seconds — no items nearby
            return false;
        }

        // Pick the best target (closest that isn't reserved by another guard)
        this.targetItem = pickBestTarget(items);
        if (this.targetItem == null) {
            // All nearby items are reserved by other guards
            this.noItemCooldown = 20; // 1 second — recheck soon
            return false;
        }

        // Reserve this item for this guard
        reserveItem(this.targetItem, guard.getId());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (guard.isFollowing() || guard.isEating()) return false;
        if (this.tickCounter > TIMEOUT_TICKS) return false;

        // Target item still exists and is alive
        if (this.targetItem != null && this.targetItem.isAlive()) {
            // If in combat now and item is far away, give up
            if (isInCombat() && guard.distanceToSqr(targetItem) > COMBAT_SCAN_RANGE * COMBAT_SCAN_RANGE) {
                return false;
            }
            return true;
        }

        // Target disappeared! Re-scan for other items
        List<ItemEntity> items = scanForItems();
        if (!items.isEmpty()) {
            ItemEntity newTarget = pickBestTarget(items);
            if (newTarget != null) {
                this.targetItem = newTarget;
                reserveItem(newTarget, guard.getId());
                guard.getNavigation().moveTo(newTarget, WALK_SPEED);
                return true;
            }
        }

        // No more items — stop
        return false;
    }

    @Override
    public void start() {
        this.tickCounter = 0;
        if (this.targetItem != null) {
            guard.getNavigation().moveTo(targetItem, WALK_SPEED);
        }
    }

    @Override
    public void stop() {
        // Release our reservation
        releaseReservation(guard.getId());
        this.targetItem = null;
        this.tickCounter = 0;
        guard.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.tickCounter++;

        // Phase 1: Try to pick up ALL items within pickup range (multi-pickup)
        List<ItemEntity> nearbyItems = guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(2.0D),
                ie -> ie.isAlive() && !ie.getItem().isEmpty() && isUseful(ie.getItem())
        );

        boolean pickedUpAny = false;
        for (ItemEntity item : nearbyItems) {
            ItemStack ground = item.getItem();
            if (tryEquipBetter(ground)) {
                item.discard();
                releaseItemReservation(item);
                pickedUpAny = true;
            }
        }

        // If we picked something up, immediately re-check for more items nearby
        // (NO cooldown after success — keep picking up while items are in range!)
        if (pickedUpAny) {
            // Check if our target item was one of the picked-up items
            if (this.targetItem != null && !this.targetItem.isAlive()) {
                // Target was picked up (by us or someone else)
                // Re-scan for next target
                List<ItemEntity> items = scanForItems();
                if (!items.isEmpty()) {
                    ItemEntity newTarget = pickBestTarget(items);
                    if (newTarget != null) {
                        this.targetItem = newTarget;
                        reserveItem(newTarget, guard.getId());
                        guard.getNavigation().moveTo(newTarget, WALK_SPEED);
                    }
                } else {
                    this.targetItem = null;
                }
            }
            return;
        }

        // Phase 2: Walk toward target item if not close enough yet
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return;
        }

        // Look at the target while walking
        guard.getLookControl().setLookAt(targetItem.getX(), targetItem.getY(), targetItem.getZ());

        double distSq = guard.distanceToSqr(targetItem);

        // Close enough — we should have picked it up in Phase 1
        // If not, it might not be useful. Try next item.
        if (distSq < PICKUP_DISTANCE_SQ) {
            // We're right on top of the item but didn't pick it up
            // It's not useful for us — mark it and move on
            releaseItemReservation(targetItem);
            this.targetItem = null;

            // Look for another target
            List<ItemEntity> items = scanForItems();
            if (!items.isEmpty()) {
                ItemEntity newTarget = pickBestTarget(items);
                if (newTarget != null) {
                    this.targetItem = newTarget;
                    reserveItem(newTarget, guard.getId());
                    guard.getNavigation().moveTo(newTarget, WALK_SPEED);
                }
            }
            return;
        }

        // Continue walking toward the item
        if (this.tickCounter % 20 == 0 || guard.getNavigation().isDone()) {
            guard.getNavigation().moveTo(targetItem, WALK_SPEED);
        }
    }

    // ========== ITEM RESERVATION SYSTEM ==========

    /**
     * Reserve an item for a specific guard.
     * This prevents multiple guards from walking toward the same item.
     */
    private static void reserveItem(ItemEntity item, int guardId) {
        reservedItems.put(item.getUUID(), guardId);
    }

    /**
     * Release the reservation for a specific item.
     */
    private static void releaseItemReservation(ItemEntity item) {
        if (item != null) {
            reservedItems.remove(item.getUUID());
        }
    }

    /**
     * Release ALL reservations held by a specific guard.
     * Called when the goal stops.
     */
    private static void releaseReservation(int guardId) {
        reservedItems.entrySet().removeIf(entry -> entry.getValue() == guardId);
    }

    /**
     * Check if an item is reserved by another guard (not this one).
     */
    private boolean isReservedByOther(ItemEntity item) {
        Integer reserverId = reservedItems.get(item.getUUID());
        if (reserverId == null) return false;
        return reserverId != guard.getId();
    }

    // ========== SCANNING ==========

    private boolean isInCombat() {
        return guard.getTarget() != null && guard.getTarget().isAlive() && guard.isAggressive();
    }

    /**
     * Scan for all items within range that this guard might want.
     */
    private List<ItemEntity> scanForItems() {
        double range = isInCombat() ? COMBAT_SCAN_RANGE : PEACETIME_SCAN_RANGE;
        return guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(range),
                ie -> ie.isAlive() && !ie.getItem().isEmpty() && isUseful(ie.getItem())
        );
    }

    /**
     * Pick the best target item from a list.
     * - Skip items reserved by other guards (anti-crowding)
     * - Wounded guards prioritize food
     * - Otherwise, pick the closest item
     */
    private ItemEntity pickBestTarget(List<ItemEntity> items) {
        ItemEntity best = null;
        double bestScore = Double.MAX_VALUE;

        boolean isWounded = guard.getHealth() < guard.getMaxHealth() * 0.75D;

        for (ItemEntity item : items) {
            // Skip items reserved by other guards
            if (isReservedByOther(item)) continue;

            double dist = guard.distanceToSqr(item);

            // Score: lower is better
            double score = dist;

            // Wounded guards: food items get priority (subtract large value to sort first)
            if (isWounded && isFoodItem(item.getItem())) {
                score -= 10000.0D;
            }

            // Armor pieces the guard needs get slight priority over duplicates
            if (isArmorUpgrade(item.getItem())) {
                score -= 500.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                best = item;
            }
        }

        return best;
    }

    // ========== ITEM EVALUATION ==========

    /**
     * Check if an item is useful for this guard (worth picking up).
     * This is the gatekeeper — if it returns false, the guard ignores the item entirely.
     */
    private boolean isUseful(ItemStack stack) {
        // Weapons — useful if better than current or no weapon
        if (isWeaponish(stack)) {
            ItemStack current = guard.guardInventory.getItem(5);
            return current.isEmpty() || getWeaponTier(stack) > getWeaponTier(current);
        }

        // Armor — useful if better than current in that slot or slot is empty
        Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            int invIndex = Guard.slotToInventoryIndex(equippable.slot());
            ItemStack current = guard.guardInventory.getItem(invIndex);
            return current.isEmpty() || getArmorTier(stack) > getArmorTier(current);
        }

        // Shields — useful if guard doesn't have one
        if (stack.getItem() instanceof ShieldItem) {
            ItemStack offhand = guard.guardInventory.getItem(4);
            return !(offhand.getItem() instanceof ShieldItem);
        }

        // Food — always useful if guard has no food
        // Also useful if current food is worse
        // Also useful if guard is wounded and has a shield (will swap)
        if (isFoodItem(stack)) {
            ItemStack offhand = guard.guardInventory.getItem(4);
            if (offhand.isEmpty()) return true;
            if (isFoodItem(offhand)) return getFoodValue(stack) > getFoodValue(offhand);
            if (offhand.getItem() instanceof ShieldItem && guard.getHealth() < guard.getMaxHealth() * 0.75D) return true;
        }

        return false;
    }

    /**
     * Quick check if an item is an armor upgrade for the guard.
     */
    private boolean isArmorUpgrade(ItemStack stack) {
        Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            int invIndex = Guard.slotToInventoryIndex(equippable.slot());
            ItemStack current = guard.guardInventory.getItem(invIndex);
            return current.isEmpty() || getArmorTier(stack) > getArmorTier(current);
        }
        return false;
    }

    // ========== EQUIP LOGIC ==========

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
        boolean isFood = isFoodItem(ground);

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
                    // Currently holding food — swap for shield
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
                return false;
            }

            // Case C: We found food
            if (isFood) {
                boolean currentIsShield = currentOffhand.getItem() instanceof ShieldItem;
                boolean currentIsFood = isFoodItem(currentOffhand);

                // If guard is wounded (below 75% health) and current offhand is a shield,
                // swap: drop shield, pick up food (health is more important!)
                if (currentIsShield && guard.getHealth() < guard.getMaxHealth() * 0.75D) {
                    dropOld(currentOffhand);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }

                // If current offhand is food, swap for better food
                if (currentIsFood) {
                    if (getFoodValue(ground) > getFoodValue(currentOffhand)) {
                        dropOld(currentOffhand);
                        guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                        return true;
                    }
                    return false;
                }

                // If current offhand is something else, replace with food
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

    // ========== TIER HELPERS ==========

    private static boolean isFoodItem(ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.FOOD);
    }

    private static int getFoodValue(ItemStack stack) {
        net.minecraft.world.food.FoodProperties food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
        return food != null ? food.nutrition() : 0;
    }

    private static boolean isWeaponish(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ProjectileWeaponItem || item instanceof AxeItem || item instanceof TridentItem) return true;
        if (item instanceof MaceItem) return true;
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        return itemId.contains("_sword");
    }

    private static int getWeaponTier(ItemStack stack) {
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

    private static int getArmorTier(ItemStack stack) {
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
