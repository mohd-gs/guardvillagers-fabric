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
 * v3.3.4 changes:
 * - CRITICAL FIX: Moved from priority 5 to priority 2 — goals at priority 3-5
 *   (village patrol, stroll, follow shield guard, squad) were constantly overriding
 *   this goal because they share the MOVE flag and have equal/higher activity.
 * - CRITICAL FIX: isReservedByOther now checks if the reserver guard is still alive.
 *   Dead/despawned guards left stale reservations that blocked all other guards forever.
 * - CRITICAL FIX: Removed tickCount % 10 gate in canUse() — it was causing the goal
 *   to only activate on specific ticks. If another goal was running on that tick,
 *   this goal would be skipped entirely for another 10 ticks, creating long gaps
 *   where no pickup happened at all.
 * - SIMPLIFIED: Removed over-complicated isUseful() pre-filter. Now ALL items that
 *   are weapons/armor/shields/food are considered "potentially useful" for walking,
 *   and tryEquipBetter() handles the actual equip decision. This prevents guards
 *   from ignoring items because a state check was stale.
 * - MULTI-PICKUP: When a guard is near items, it picks up ALL useful items in range.
 * - ITEM RESERVATION: Guards claim items to avoid all rushing the same one.
 * - RE-SCAN: If target disappears, immediately look for next item.
 */
public class PickupBetterEquipmentGoal extends Goal {
    private final Guard guard;
    private ItemEntity targetItem = null;
    private int tickCounter = 0;
    private int noItemCooldown = 0;

    // === ITEM RESERVATION SYSTEM ===
    // Key = ItemEntity UUID, Value = Guard entity ID
    private static final Map<UUID, Integer> reservedItems = new ConcurrentHashMap<>();

    private static final double PICKUP_DISTANCE_SQ = 2.5D * 2.5D; // Slightly larger for reliable pickup
    private static final double PEACETIME_SCAN_RANGE = 10.0D;
    private static final double COMBAT_SCAN_RANGE = 3.0D;
    private static final double WALK_SPEED = 0.65D;
    private static final int TIMEOUT_TICKS = 200;

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
        // Don't activate during combat (guards should fight, not loot)
        if (isInCombat()) return false;

        // Scan for items — no tick gating! The cooldown system handles frequency.
        List<ItemEntity> items = scanForItems();
        if (items.isEmpty()) {
            this.noItemCooldown = 30; // 1.5 seconds — no items nearby
            return false;
        }

        // Pick the best target
        this.targetItem = pickBestTarget(items);
        if (this.targetItem == null) {
            this.noItemCooldown = 15; // 0.75 seconds — all reserved, recheck soon
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
        // Stop if combat started
        if (isInCombat()) return false;

        // Target still valid?
        if (this.targetItem != null && this.targetItem.isAlive()) {
            return true;
        }

        // Target disappeared — re-scan immediately
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
        releaseReservation(guard.getId());
        this.targetItem = null;
        this.tickCounter = 0;
        guard.getNavigation().stop();
    }

    @Override
    public void tick() {
        this.tickCounter++;

        // Phase 1: Try to pick up ALL useful items within pickup range (multi-pickup)
        List<ItemEntity> nearbyItems = guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(2.5D),
                ie -> ie.isAlive() && !ie.getItem().isEmpty() && isPotentiallyUseful(ie.getItem())
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

        if (pickedUpAny) {
            // Target was picked up — re-scan for next
            if (this.targetItem != null && !this.targetItem.isAlive()) {
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
            // Don't set any cooldown — keep picking up!
            return;
        }

        // Phase 2: Walk toward target item
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return;
        }

        guard.getLookControl().setLookAt(targetItem.getX(), targetItem.getY(), targetItem.getZ());

        double distSq = guard.distanceToSqr(targetItem);

        // We're close but didn't pick it up — it's not useful for us, skip it
        if (distSq < PICKUP_DISTANCE_SQ) {
            releaseItemReservation(targetItem);
            this.targetItem = null;

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
        if (this.tickCounter % 15 == 0 || guard.getNavigation().isDone()) {
            guard.getNavigation().moveTo(targetItem, WALK_SPEED);
        }
    }

    // ========== ITEM RESERVATION ==========

    private static void reserveItem(ItemEntity item, int guardId) {
        reservedItems.put(item.getUUID(), guardId);
    }

    private static void releaseItemReservation(ItemEntity item) {
        if (item != null) {
            reservedItems.remove(item.getUUID());
        }
    }

    private static void releaseReservation(int guardId) {
        reservedItems.entrySet().removeIf(entry -> entry.getValue() == guardId);
    }

    /**
     * Check if an item is reserved by another guard.
     * Also validates that the reserver guard is still alive in the world.
     * Stale reservations from dead/despawned guards are cleaned up.
     */
    private boolean isReservedByOther(ItemEntity item) {
        Integer reserverId = reservedItems.get(item.getUUID());
        if (reserverId == null) return false;
        if (reserverId == guard.getId()) return false;

        // Check if the reserver guard is still alive and in the world
        net.minecraft.world.entity.Entity other = guard.level().getEntity(reserverId);
        if (other == null || !other.isAlive()) {
            // Stale reservation from a dead/despawned guard — clean it up
            reservedItems.remove(item.getUUID());
            return false;
        }
        return true;
    }

    // ========== SCANNING ==========

    private boolean isInCombat() {
        return guard.getTarget() != null && guard.getTarget().isAlive() && guard.isAggressive();
    }

    /**
     * Scan for items within range. Uses a BROAD filter (isPotentiallyUseful)
     * instead of a strict filter — let tryEquipBetter() decide what's worth equipping.
     */
    private List<ItemEntity> scanForItems() {
        double range = PEACETIME_SCAN_RANGE; // Always full range — combat is blocked in canUse()
        return guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(range),
                ie -> ie.isAlive() && !ie.getItem().isEmpty() && isPotentiallyUseful(ie.getItem())
        );
    }

    /**
     * Broad check: is this item something a guard might want?
     * Returns true for any weapon, armor, shield, or food.
     * The actual "is it better than what I have?" check is in tryEquipBetter().
     *
     * Why broad instead of strict? Because a strict pre-filter can miss valid items:
     * - If the guard's state changes between scan and equip (e.g., another guard
     *   drops an item that makes this one an upgrade), the strict filter would miss it.
     * - The guard should walk toward ANY potentially useful item and decide at pickup time.
     */
    private boolean isPotentiallyUseful(ItemStack stack) {
        // Weapons
        if (isWeaponish(stack)) return true;
        // Armor
        Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) return true;
        // Shields
        if (stack.getItem() instanceof ShieldItem) return true;
        // Food
        if (isFoodItem(stack)) return true;
        return false;
    }

    /**
     * Pick the best target item from a list.
     * - Skip items reserved by other guards
     * - Wounded guards prioritize food
     * - Then closest first
     */
    private ItemEntity pickBestTarget(List<ItemEntity> items) {
        ItemEntity best = null;
        double bestScore = Double.MAX_VALUE;
        boolean isWounded = guard.getHealth() < guard.getMaxHealth() * 0.75D;

        for (ItemEntity item : items) {
            if (isReservedByOther(item)) continue;

            double dist = guard.distanceToSqr(item);
            double score = dist;

            // Wounded guards: food items get priority
            if (isWounded && isFoodItem(item.getItem())) {
                score -= 10000.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                best = item;
            }
        }

        return best;
    }

    // ========== EQUIP LOGIC ==========

    private boolean tryEquipBetter(ItemStack ground) {
        // 1. Weapon (mainhand = slot 5)
        if (isWeaponish(ground)) {
            ItemStack current = guard.guardInventory.getItem(5);
            if (current.isEmpty() || getWeaponTier(ground) > getWeaponTier(current)) {
                dropOld(current);
                guard.setItemSlot(EquipmentSlot.MAINHAND, ground.copy());
                return true;
            }
        }

        // 2. Armor (slots 0-3)
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

        // 3. Offhand (shield and food)
        boolean isShield = ground.getItem() instanceof ShieldItem;
        boolean isFood = isFoodItem(ground);

        if (isShield || isFood) {
            ItemStack currentOffhand = guard.guardInventory.getItem(4);

            // Empty offhand — always pick up
            if (currentOffhand.isEmpty()) {
                guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                return true;
            }

            // Shield
            if (isShield) {
                boolean currentIsShield = currentOffhand.getItem() instanceof ShieldItem;
                if (!currentIsShield) {
                    dropOld(currentOffhand);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }
                if (ground.isEnchanted() && !currentOffhand.isEnchanted()) {
                    dropOld(currentOffhand);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }
                return false;
            }

            // Food
            if (isFood) {
                boolean currentIsShield = currentOffhand.getItem() instanceof ShieldItem;
                boolean currentIsFood = isFoodItem(currentOffhand);

                // Wounded guard: swap shield for food
                if (currentIsShield && guard.getHealth() < guard.getMaxHealth() * 0.75D) {
                    dropOld(currentOffhand);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }

                // Better food
                if (currentIsFood && getFoodValue(ground) > getFoodValue(currentOffhand)) {
                    dropOld(currentOffhand);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }

                // Unknown item in offhand — replace with food
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

    // ========== HELPERS ==========

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
