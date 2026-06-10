package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.Equippable;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.Comparator;
import java.util.List;

/**
 * Goal that allows guards to pick up better equipment from the ground.
 * <p>
 * v3.2.1 changes:
 * - CRITICAL FIX: Moved scan-and-equip logic from tick() to start().
 *   The previous version put logic in tick() but the Goal system never called it
 *   because canContinueToUse() defaulted to canUse() which returned false immediately
 *   (due to the tickCount % 60 check), causing the goal to start and stop before
 *   tick() was ever executed.
 * - Now uses a one-shot pattern: canUse() detects items, start() picks them up,
 *   canContinueToUse() returns false so the goal releases immediately.
 * - Reduced scan interval from 60 to 20 ticks (3s → 1s) for more responsive pickup.
 * - Removed combat restriction: guards now pick up items even during combat
 *   (they need shields/food most during fights!).
 */
public class PickupBetterEquipmentGoal extends Goal {
    private final Guard guard;
    private int cooldown = 0;

    public PickupBetterEquipmentGoal(Guard guard) {
        this.guard = guard;
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
        // Check every 20 ticks (1 second) for responsive pickup
        if (guard.tickCount % 20 != 0) return false;
        // Pre-scan: only return true if there are items nearby to consider
        double range = GuardConfig.COMMON.equipmentPickupRange;
        List<ItemEntity> items = guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(range),
                ie -> !ie.getItem().isEmpty() && ie.isAlive()
        );
        return !items.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        // One-shot goal: start() does all the work, then we're done.
        return false;
    }

    @Override
    public void start() {
        double range = GuardConfig.COMMON.equipmentPickupRange;
        List<ItemEntity> items = guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(range),
                ie -> !ie.getItem().isEmpty() && ie.isAlive()
        );

        // Sort by distance so we pick up the closest item first
        items.sort(Comparator.comparingDouble(guard::distanceToSqr));

        for (ItemEntity itemEntity : items) {
            ItemStack ground = itemEntity.getItem();
            if (tryEquipBetter(ground)) {
                itemEntity.discard();
                this.cooldown = 20; // 1 second cooldown after successful pickup
                return;
            }
        }
        this.cooldown = 40; // 2 second cooldown when nothing was picked up
    }

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

        // 3. Try offhand (slot 4) — shield or food
        // Guards ALWAYS want a shield if they don't have one.
        // If they already have food but find better food (more hunger), swap it.
        // If they have no offhand item, pick up anything useful.
        boolean isShield = ground.getItem() instanceof ShieldItem;
        boolean isFood = ground.has(net.minecraft.core.component.DataComponents.FOOD);

        if (isShield || isFood) {
            ItemStack current = guard.guardInventory.getItem(4);
            if (current.isEmpty()) {
                // Empty offhand — always pick up shield or food
                guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                return true;
            }
            // Shield logic: always prefer shield over food in offhand
            if (isShield) {
                boolean currentIsShield = current.getItem() instanceof ShieldItem;
                if (!currentIsShield) {
                    // Currently holding food, swap for shield (shields are more useful)
                    dropOld(current);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }
                // Both are shields — pick up the new one if it's enchanted or the old one is damaged
                if (ground.isEnchanted() && !current.isEnchanted()) {
                    dropOld(current);
                    guard.setItemSlot(EquipmentSlot.OFFHAND, ground.copy());
                    return true;
                }
            }
            // Food logic: swap for better food if current offhand is also food
            if (isFood && !(current.getItem() instanceof ShieldItem)) {
                int groundFood = getFoodValue(ground);
                int currentFood = getFoodValue(current);
                if (groundFood > currentFood) {
                    dropOld(current);
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

    private boolean isWeaponish(ItemStack stack) {
        Item item = stack.getItem();
        // MC 26.1.2: SwordItem class was removed — swords are now plain Item instances.
        // Detect swords by registry name instead of instanceof.
        if (item instanceof ProjectileWeaponItem || item instanceof AxeItem || item instanceof TridentItem) return true;
        // MaceItem is a new weapon type in MC 26.1.2
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
     * Uses registry name mapping for reliability across MC versions.
     * Falls back to rarity + enchantment for modded items.
     */
    private int getWeaponTier(ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        // Netherite weapons (highest tier)
        if (itemId.contains("netherite_sword") || itemId.contains("netherite_axe")) return 6;
        // Diamond weapons
        if (itemId.contains("diamond_sword") || itemId.contains("diamond_axe")) return 5;
        // Iron weapons
        if (itemId.contains("iron_sword") || itemId.contains("iron_axe")) return 4;
        // Copper weapons (MC 26.1.2 adds copper tier)
        if (itemId.contains("copper_sword") || itemId.contains("copper_axe")) return 4;
        // Stone weapons
        if (itemId.contains("stone_sword") || itemId.contains("stone_axe")) return 3;
        // Golden weapons (low tier despite gold rarity)
        if (itemId.contains("golden_sword") || itemId.contains("golden_axe")) return 2;
        // Wooden weapons (lowest tier)
        if (itemId.contains("wooden_sword") || itemId.contains("wooden_axe")) return 2;
        // Mace — high-tier weapon (comparable to diamond)
        if (stack.getItem() instanceof MaceItem) return 5;
        // Special ranged weapons — tier based on general effectiveness
        if (stack.getItem() instanceof TridentItem) return 5;
        if (stack.getItem() instanceof CrossbowItem) return 4;
        if (stack.getItem() instanceof BowItem) return 3;
        // Fallback for modded items: use rarity + enchantment bonus
        return stack.getRarity().ordinal() + (stack.isEnchanted() ? 1 : 0);
    }

    /**
     * Get the tier level of armor based on its material.
     * Uses registry name mapping for reliability across MC versions.
     * Falls back to rarity + enchantment for modded items.
     */
    private int getArmorTier(ItemStack stack) {
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        // Netherite armor (highest tier)
        if (itemId.contains("netherite")) return 6;
        // Diamond armor
        if (itemId.contains("diamond")) return 5;
        // Iron armor
        if (itemId.contains("iron")) return 4;
        // Copper armor (MC 26.1.2 adds copper tier, comparable to iron)
        if (itemId.contains("copper")) return 4;
        // Chainmail armor
        if (itemId.contains("chainmail") || itemId.contains("chain")) return 3;
        // Turtle helmet (comparable to iron)
        if (itemId.contains("turtle")) return 3;
        // Golden armor (low tier despite gold rarity)
        if (itemId.contains("golden")) return 2;
        // Leather armor (lowest tier)
        if (itemId.contains("leather")) return 1;
        // Fallback for modded items: use rarity + enchantment bonus
        return stack.getRarity().ordinal() + (stack.isEnchanted() ? 1 : 0);
    }
}
