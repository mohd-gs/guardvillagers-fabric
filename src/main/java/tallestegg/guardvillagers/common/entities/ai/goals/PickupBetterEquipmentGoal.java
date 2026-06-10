package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.Equippable;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.List;

public class PickupBetterEquipmentGoal extends Goal {
    private final Guard guard;
    private int cooldown = 0;

    public PickupBetterEquipmentGoal(Guard guard) {
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        if (!GuardConfig.COMMON.autoEquipmentUpgrade) return false;
        if (this.cooldown > 0) { this.cooldown--; return false; }
        if (guard.isBaby()) return false;
        // Don't try to pick up equipment while in active combat.
        if (guard.getTarget() != null && guard.isAggressive()) return false;
        // Don't pick up equipment while following (player might be leading them away)
        if (guard.isFollowing()) return false;
        // PERFORMANCE: Only check every 120 ticks (6 seconds) instead of 80.
        // Equipment on the ground doesn't move, so checking less frequently
        // doesn't risk missing items while saving significant scan overhead.
        if (guard.tickCount % 120 != 0) return false;
        return true;
    }

    @Override
    public void tick() {
        double range = GuardConfig.COMMON.equipmentPickupRange;
        List<ItemEntity> items = guard.level().getEntitiesOfClass(
                ItemEntity.class, guard.getBoundingBox().inflate(range),
                ie -> !ie.getItem().isEmpty() && ie.isAlive()
        );
        // PERFORMANCE: Limit to checking first 5 items to avoid processing
        // massive item stacks (e.g., after a raid with lots of drops)
        int checked = 0;
        for (ItemEntity itemEntity : items) {
            if (checked >= 5) break;
            ItemStack ground = itemEntity.getItem();
            if (tryEquipBetter(ground)) {
                itemEntity.discard();
                this.cooldown = 40; // 2 second cooldown after successful pickup
                return;
            }
            checked++;
        }
        this.cooldown = 40; // Brief cooldown even when nothing was picked up
    }

    private boolean tryEquipBetter(ItemStack ground) {
        // Try weapon slot (mainhand = slot 5)
        if (isWeaponish(ground)) {
            ItemStack current = guard.guardInventory.getItem(5);
            if (current.isEmpty() || getTierLevel(ground) > getTierLevel(current)) {
                dropOld(current);
                guard.guardInventory.setItem(5, ground.copy());
                return true;
            }
        }
        // Try armor slots (0-3)
        Equippable equippable = ground.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                int invIndex = Guard.slotToInventoryIndex(slot);
                ItemStack current = guard.guardInventory.getItem(invIndex);
                if (current.isEmpty() || getArmorDefense(ground) > getArmorDefense(current)) {
                    dropOld(current);
                    guard.guardInventory.setItem(invIndex, ground.copy());
                    return true;
                }
            }
        }
        // Try offhand (slot 4) - shield or food
        if (ground.getItem() instanceof ShieldItem || ground.has(net.minecraft.core.component.DataComponents.FOOD)) {
            ItemStack current = guard.guardInventory.getItem(4);
            if (current.isEmpty()) {
                guard.guardInventory.setItem(4, ground.copy());
                return true;
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
        return item instanceof ProjectileWeaponItem || item instanceof AxeItem || item instanceof TridentItem;
    }

    private int getTierLevel(ItemStack stack) {
        // Use rarity as a proxy since getTier() may not be available
        return stack.getRarity().ordinal() + (stack.isEnchanted() ? 2 : 0);
    }

    private float getArmorDefense(ItemStack stack) {
        Equippable eq = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (eq != null) {
            return stack.getRarity().ordinal();
        }
        return 0;
    }
}
