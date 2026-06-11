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
 * v3.3.5 changes:
 * - CRITICAL FIX: Guards now properly return to wandering after picking up items.
 *   The broad isPotentiallyUseful() filter caused the goal to keep finding items
 *   that weren't upgrades, keeping the goal active and blocking all wandering goals.
 *   Now uses isUpgrade() for scanning/walking decisions — only walks toward items
 *   that are ACTUAL upgrades. The broad filter is only used for the nearby pickup
 *   scan (Phase 1) to catch any items that became upgrades after a previous pickup.
 * - FOOD PRIORITY: Wounded guards (below 75% HP) always walk toward food first.
 *   Healthy guards with shields skip food items (can't pick them up anyway).
 * - ITEM RESERVATION with stale cleanup.
 * - MULTI-PICKUP: Picks up all useful items in range when near.
 * - After all upgrades are picked up → goal stops → wandering/patrol goals activate.
 */
public class PickupBetterEquipmentGoal extends Goal {
    private final Guard guard;
    private ItemEntity targetItem = null;
    private int tickCounter = 0;
    private int noUpgradeCooldown = 0;

    // === ITEM RESERVATION SYSTEM ===
    private static final Map<UUID, Integer> reservedItems = new ConcurrentHashMap<>();

    private static final double PICKUP_DISTANCE_SQ = 2.5D * 2.5D;
    private static final double SCAN_RANGE = 10.0D;
    private static final double WALK_SPEED = 0.65D;
    private static final int TIMEOUT_TICKS = 200;

    public PickupBetterEquipmentGoal(Guard guard) {
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.autoEquipmentUpgrade) return false;
        if (this.noUpgradeCooldown > 0) {
            this.noUpgradeCooldown--;
            return false;
        }
        if (guard.isBaby()) return false;
        if (guard.isFollowing()) return false;
        if (guard.isEating()) return false;
        if (isInCombat()) return false;

        // Scan for UPGRADE items only — not just any items
        List<ItemEntity> upgrades = scanForUpgrades();
        if (upgrades.isEmpty()) {
            this.noUpgradeCooldown = 60; // 3 seconds — no upgrades nearby, go back to wandering
            return false;
        }

        // Pick the best upgrade target
        this.targetItem = pickBestTarget(upgrades);
        if (this.targetItem == null) {
            // All items are reserved by other guards
            this.noUpgradeCooldown = 20; // 1 second — recheck soon
            return false;
        }

        reserveItem(this.targetItem, guard.getId());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (guard.isFollowing() || guard.isEating()) return false;
        if (this.tickCounter > TIMEOUT_TICKS) return false;
        if (isInCombat()) return false;

        // Target still valid?
        if (this.targetItem != null && this.targetItem.isAlive()) {
            return true;
        }

        // Target disappeared — re-scan for UPGRADES only
        List<ItemEntity> upgrades = scanForUpgrades();
        if (!upgrades.isEmpty()) {
            ItemEntity newTarget = pickBestTarget(upgrades);
            if (newTarget != null) {
                this.targetItem = newTarget;
                reserveItem(newTarget, guard.getId());
                guard.getNavigation().moveTo(newTarget, WALK_SPEED);
                return true;
            }
        }

        // No more upgrades — stop the goal so wandering can take over
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

        // Phase 1: Pick up ALL useful items already within reach (broad filter)
        // This catches items that became upgrades after a previous pickup
        // (e.g., guard picks up diamond chestplate → their old iron chestplate
        // is dropped → another guard nearby can now pick up that iron chestplate)
        List<ItemEntity> nearbyItems = guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(2.5D),
                ie -> ie.isAlive() && !ie.getItem().isEmpty() && isUpgrade(ie.getItem())
        );

        boolean pickedUpAny = false;
        for (ItemEntity item : nearbyItems) {
            if (tryEquipBetter(item.getItem())) {
                item.discard();
                releaseItemReservation(item);
                pickedUpAny = true;
            }
        }

        if (pickedUpAny) {
            // Re-scan for more UPGRADES after picking up
            List<ItemEntity> upgrades = scanForUpgrades();
            if (!upgrades.isEmpty()) {
                ItemEntity newTarget = pickBestTarget(upgrades);
                if (newTarget != null) {
                    this.targetItem = newTarget;
                    reserveItem(newTarget, guard.getId());
                    guard.getNavigation().moveTo(newTarget, WALK_SPEED);
                } else {
                    this.targetItem = null;
                }
            } else {
                // No more upgrades — the goal will stop on next canContinueToUse()
                this.targetItem = null;
            }
            // No cooldown — keep picking up while upgrades exist!
            return;
        }

        // Phase 2: Walk toward the target upgrade item
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return;
        }

        guard.getLookControl().setLookAt(targetItem.getX(), targetItem.getY(), targetItem.getZ());

        double distSq = guard.distanceToSqr(targetItem);

        // We arrived but couldn't equip it — it's not actually an upgrade for us
        // (shouldn't happen since we filter with isUpgrade, but just in case)
        if (distSq < PICKUP_DISTANCE_SQ) {
            releaseItemReservation(targetItem);
            this.targetItem = null;

            // Look for another upgrade
            List<ItemEntity> upgrades = scanForUpgrades();
            if (!upgrades.isEmpty()) {
                ItemEntity newTarget = pickBestTarget(upgrades);
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

    private boolean isReservedByOther(ItemEntity item) {
        Integer reserverId = reservedItems.get(item.getUUID());
        if (reserverId == null) return false;
        if (reserverId == guard.getId()) return false;

        // Validate that the reserver guard is still alive
        net.minecraft.world.entity.Entity other = guard.level().getEntity(reserverId);
        if (other == null || !other.isAlive()) {
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
     * Scan for items that are ACTUAL UPGRADES for this guard.
     * Only returns items that the guard would actually equip.
     * This prevents the guard from walking toward items it can't use,
     * which would block wandering/patrol goals.
     */
    private List<ItemEntity> scanForUpgrades() {
        return guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(SCAN_RANGE),
                ie -> ie.isAlive() && !ie.getItem().isEmpty() && isUpgrade(ie.getItem())
        );
    }

    /**
     * Check if an item is an ACTUAL UPGRADE for this guard right now.
     * This is the key filter that decides whether to walk toward an item.
     * Only returns true if the guard would actually equip this item.
     */
    private boolean isUpgrade(ItemStack stack) {
        // Weapon — better than current?
        if (isWeaponish(stack)) {
            ItemStack current = guard.guardInventory.getItem(5);
            return current.isEmpty() || getWeaponTier(stack) > getWeaponTier(current);
        }

        // Armor — better than current in that slot?
        Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            int invIndex = Guard.slotToInventoryIndex(equippable.slot());
            ItemStack current = guard.guardInventory.getItem(invIndex);
            return current.isEmpty() || getArmorTier(stack) > getArmorTier(current);
        }

        // Shield — need one?
        if (stack.getItem() instanceof ShieldItem) {
            ItemStack offhand = guard.guardInventory.getItem(4);
            if (offhand.isEmpty()) return true;
            if (!(offhand.getItem() instanceof ShieldItem)) return true; // Have food, want shield
            if (stack.isEnchanted() && !offhand.isEnchanted()) return true;
            return false;
        }

        // Food — can we use it?
        if (isFoodItem(stack)) {
            ItemStack offhand = guard.guardInventory.getItem(4);
            if (offhand.isEmpty()) return true; // No offhand item — want food
            // Already have food — is this better?
            if (isFoodItem(offhand)) return getFoodValue(stack) > getFoodValue(offhand);
            // Have shield — only want food if wounded
            if (offhand.getItem() instanceof ShieldItem) {
                return guard.getHealth() < guard.getMaxHealth() * 0.75D;
            }
            // Something else in offhand — replace with food
            return true;
        }

        return false;
    }

    /**
     * Pick the best target item from a list of upgrades.
     * - Skip items reserved by other guards
     * - Wounded guards prioritize food
     * - Otherwise closest first
     */
    private ItemEntity pickBestTarget(List<ItemEntity> items) {
        ItemEntity best = null;
        double bestScore = Double.MAX_VALUE;
        boolean isWounded = guard.getHealth() < guard.getMaxHealth() * 0.75D;

        for (ItemEntity item : items) {
            if (isReservedByOther(item)) continue;

            double dist = guard.distanceToSqr(item);
            double score = dist;

            // Wounded guards: food items get highest priority
            if (isWounded && isFoodItem(item.getItem())) {
                score -= 10000.0D;
            }
            // Armor upgrades get slight priority over weapon upgrades
            // (a full armor set is more important than a slightly better sword)
            if (isArmorUpgradeFor(item.getItem())) {
                score -= 500.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                best = item;
            }
        }

        return best;
    }

    /**
     * Quick check: is this item an armor upgrade for the guard?
     */
    private boolean isArmorUpgradeFor(ItemStack stack) {
        Equippable equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            int invIndex = Guard.slotToInventoryIndex(equippable.slot());
            ItemStack current = guard.guardInventory.getItem(invIndex);
            return current.isEmpty() || getArmorTier(stack) > getArmorTier(current);
        }
        return false;
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
