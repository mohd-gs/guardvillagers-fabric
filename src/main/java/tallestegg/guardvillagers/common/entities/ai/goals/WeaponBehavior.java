package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.*;
import tallestegg.guardvillagers.GuardVillagerTags;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

/**
 * Weapon-based behavior system that makes guards fight differently based on their weapon.
 * <p>
 * Instead of all melee guards behaving identically, each weapon type now has
 * distinct combat characteristics:
 * <ul>
 *   <li><b>Sword</b>: Fast attacks, can parry (raise shield between swings),
 *       good all-rounder. Stays close and attacks frequently.</li>
 *   <li><b>Axe</b>: Heavy, slow attacks. Breaks shields. Berserker bonus (no shield).
 *       Should time attacks carefully and not just spam-click.</li>
 *   <li><b>Mace</b>: Heavy weapon with knockback. Good against armored targets.
 *       Smashes through defenses.</li>
 *   <li><b>Spear/Trident</b>: Reach weapon. Can attack from 1 block further.
 *       Retreating combat style — stab and step back.</li>
 *   <li><b>Bow</b>: Ranged, maintains distance, kites enemies.</li>
 *   <li><b>Crossbow</b>: Ranged, slower but more powerful. Stays at medium range.</li>
 * </ul>
 */
public final class WeaponBehavior {

    private WeaponBehavior() {} // Utility class

    // === Weapon Type Classification ===

    public enum WeaponType {
        SWORD,       // Fast melee, parry-capable
        AXE,         // Heavy melee, shield-breaking
        MACE,        // Heavy melee, knockback specialist
        SPEAR,       // Reach melee, hit-and-run
        BOW,         // Ranged, kiting
        CROSSBOW,    // Ranged, powerful shots
        TRIDENT,     // Ranged+melee hybrid
        UNARMED;     // Fallback

        public boolean isMelee() {
            return this == SWORD || this == AXE || this == MACE || this == SPEAR || this == TRIDENT;
        }

        public boolean isRanged() {
            return this == BOW || this == CROSSBOW || this == TRIDENT;
        }

        public boolean isHeavy() {
            return this == AXE || this == MACE;
        }
    }

    /**
     * Determine the weapon type based on the guard's main hand item.
     *
     * FIX (v4.0.2): Added SPEAR detection using ItemTags.SPEARS (vanilla tag
     * that includes trident). Previously, SPEAR was never returned because no
     * vanilla item mapped to it, making PHALANX formation impossible and all
     * spear-related combat behaviors dead code.
     *
     * Detection order matters:
     * 1. Items in ItemTags.SPEARS → SPEAR (trident, modded spears)
     * 2. _sword name pattern → SWORD
     * 3. AxeItem → AXE
     * 4. MaceItem → MACE
     * 5. BowItem → BOW
     * 6. CrossbowItem → CROSSBOW
     *
     * Note: TridentItem is in ItemTags.SPEARS, so it returns SPEAR instead of
     * TRIDENT. This is intentional — tridents work as spears for melee reach
     * and formation behavior. The TRIDENT enum is kept for modded weapons
     * that need the ranged+melee hybrid distinction.
     */
    public static WeaponType getWeaponType(Guard guard) {
        ItemStack mainHand = guard.getMainHandItem();
        Item item = mainHand.getItem();
        // Check ItemTags.SPEARS (vanilla tag, includes trident) and our custom
        // guardvillagers:spear_items tag FIRST — catches trident and any modded spears
        if (mainHand.is(ItemTags.SPEARS) || mainHand.is(GuardVillagerTags.SPEAR_ITEMS)) return WeaponType.SPEAR;
        if (BuiltInRegistries.ITEM.getKey(item).toString().contains("_sword")) return WeaponType.SWORD;
        if (item instanceof AxeItem) return WeaponType.AXE;
        if (item instanceof MaceItem) return WeaponType.MACE;
        if (item instanceof BowItem) return WeaponType.BOW;
        if (item instanceof CrossbowItem) return WeaponType.CROSSBOW;
        return WeaponType.UNARMED;
    }

    // === Attack Speed (ticks between attacks) ===

    /**
     * Get the attack cooldown interval for a weapon type.
     * Lower values = faster attacks.
     * Difficulty modifier: LOW difficulty increases cooldown (slower attacks).
     */
    public static int getAttackCooldown(WeaponType type) {
        if (!GuardConfig.COMMON.weaponSpecificBehavior) return applyDifficultyCooldown(20); // Default
        int baseCooldown = switch (type) {
            case SWORD -> 15;    // Fast - 0.75s
            case AXE -> 30;      // Slow - 1.5s
            case MACE -> 25;     // Medium-slow - 1.25s
            case SPEAR -> 20;    // Medium - 1.0s
            case TRIDENT -> 22;  // Medium - 1.1s
            case BOW -> 20;      // Handled by bow goal
            case CROSSBOW -> 25; // Handled by crossbow goal
            default -> 20;
        };
        return applyDifficultyCooldown(baseCooldown);
    }

    /**
     * Apply difficulty cooldown multiplier. LOW difficulty = longer cooldowns = slower attacks.
     */
    private static int applyDifficultyCooldown(int baseCooldown) {
        double multiplier = GuardConfig.getAttackCooldownMultiplier();
        return Math.max(5, (int) Math.round(baseCooldown * multiplier));
    }

    // === Optimal Combat Distance ===

    /**
     * Get the ideal distance this guard wants to maintain from its target.
     * Used by both melee and ranged goals to determine positioning.
     */
    public static double getOptimalCombatDistance(Guard guard) {
        if (!GuardConfig.COMMON.weaponSpecificBehavior) return 2.0D;
        WeaponType type = getWeaponType(guard);
        return switch (type) {
            case SWORD -> 1.8D;     // Close and personal
            case AXE -> 2.2D;       // Slightly further for wind-up
            case MACE -> 2.0D;      // Standard melee
            case SPEAR -> 3.2D;     // Extended reach - attack from further
            case TRIDENT -> 3.0D;   // Extended reach
            case BOW -> 12.0D;      // Long range
            case CROSSBOW -> 10.0D; // Medium-long range
            default -> 2.0D;
        };
    }

    /**
     * Get the minimum distance this guard will tolerate before retreating.
     * Ranged guards retreat when enemies get closer than this.
     */
    public static double getMinCombatDistance(Guard guard) {
        if (!GuardConfig.COMMON.weaponSpecificBehavior) return 3.0D;
        WeaponType type = getWeaponType(guard);
        return switch (type) {
            case SWORD -> 1.0D;     // Swords fight close, never retreat
            case AXE -> 1.2D;       // Axes fight close
            case MACE -> 1.0D;      // Maces fight close
            case SPEAR -> 2.0D;     // Spears prefer distance
            case TRIDENT -> 2.5D;   // Tridents prefer distance
            case BOW -> 5.0D;       // Bows retreat early
            case CROSSBOW -> 4.0D;  // Crossbows retreat a bit later
            default -> 2.0D;
        };
    }

    // === Movement Speed in Combat ===

    /**
     * Get the movement speed modifier during combat for this weapon type.
     * Heavier weapons slow the guard down; lighter weapons allow faster movement.
     * Difficulty: LOW difficulty further reduces combat speed.
     */
    public static double getCombatSpeedModifier(Guard guard) {
        if (!GuardConfig.COMMON.weaponSpecificBehavior) return GuardConfig.getMovementSpeedMultiplier();
        WeaponType type = getWeaponType(guard);
        double baseMod = switch (type) {
            case SWORD -> 1.2D;     // Fast footwork
            case AXE -> 0.85D;      // Slow, deliberate
            case MACE -> 0.9D;      // Slightly slow
            case SPEAR -> 1.1D;     // Quick positioning
            case TRIDENT -> 1.0D;   // Normal
            case BOW -> 1.0D;       // Normal
            case CROSSBOW -> 0.9D;  // Slightly slow when aiming
            default -> 1.0D;
        };
        return baseMod * GuardConfig.getMovementSpeedMultiplier();
    }

    // === Strategic Behaviors ===

    /**
     * Should this guard strafe (circle) around the target during combat?
     * Sword guards circle to find openings; spear guards sidestep.
     */
    public static boolean shouldStrafe(Guard guard) {
        WeaponType type = getWeaponType(guard);
        return type == WeaponType.SWORD || type == WeaponType.SPEAR;
    }

    /**
     * Should this guard perform hit-and-run tactics?
     * Spear guards stab and retreat; berserker axes charge and stay.
     */
    public static boolean shouldHitAndRun(Guard guard) {
        WeaponType type = getWeaponType(guard);
        return type == WeaponType.SPEAR;
    }

    /**
     * Should this guard raise its shield between attacks?
     * Sword+shield guards do this; berserker axes don't.
     */
    public static boolean shouldShieldBetweenAttacks(Guard guard) {
        if (guard.getOffhandItem().isEmpty() || !guard.getOffhandItem().has(net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS)) {
            return false; // No shield
        }
        WeaponType type = getWeaponType(guard);
        return type == WeaponType.SWORD || type == WeaponType.SPEAR;
    }

    /**
     * Should this guard switch to melee when enemies get too close?
     * Trident guards should punch; crossbow guards should back up instead.
     */
    public static boolean shouldMeleeWhenClose(Guard guard) {
        WeaponType type = getWeaponType(guard);
        return type == WeaponType.TRIDENT; // Tridents work as both melee and ranged
    }

    /**
     * Get the attack reach bonus for this weapon type.
     * Spears get +1.0 block reach; other weapons get 0.
     */
    public static double getAttackReachBonus(Guard guard) {
        if (!GuardConfig.COMMON.weaponSpecificBehavior) return 0.0D;
        WeaponType type = getWeaponType(guard);
        return switch (type) {
            case SPEAR -> 1.0D;    // Extra reach
            case TRIDENT -> 0.5D;  // Slight reach bonus
            default -> 0.0D;
        };
    }

    /**
     * Should this guard specifically target shield-bearing enemies?
     * Axe guards prioritize shielded enemies to break their shields.
     */
    public static boolean shouldTargetShielded(Guard guard) {
        WeaponType type = getWeaponType(guard);
        return type == WeaponType.AXE;
    }

    /**
     * Should this guard prioritize mounted enemies?
     * Spear/trident guards get bonus damage vs mounted and should target them.
     */
    public static boolean shouldTargetMounted(Guard guard) {
        WeaponType type = getWeaponType(guard);
        return type == WeaponType.SPEAR || type == WeaponType.TRIDENT;
    }

    /**
     * Should this guard flee from a charging creeper?
     * All guards should flee from creepers that are about to explode,
     * but melee guards need to be much closer to trigger this.
     */
    public static boolean shouldFleeFromCreeper(Guard guard, Creeper creeper) {
        if (!GuardConfig.COMMON.antiCreeperBehavior) return false;
        double fleeRadius = getWeaponType(guard).isRanged() ? 6.0D : 4.0D;
        return guard.distanceTo(creeper) < fleeRadius && creeper.getSwellDir() > 0;
    }

    /**
     * Determine if a guard should engage a target or hold position.
     * Shield guards in formation should hold the line; berserkers should charge.
     */
    public static boolean shouldCharge(Guard guard) {
        WeaponType type = getWeaponType(guard);
        if (type == WeaponType.AXE && guard.isBerserker()) return true;
        if (type == WeaponType.MACE) return true; // Maces are aggressive
        // Don't charge if holding shield in formation
        if (guard.isShieldGuard() && guard.getTarget() != null) {
            // Shield guards only charge if the enemy is very close
            return guard.distanceTo(guard.getTarget()) < 3.0D;
        }
        return false;
    }

    /**
     * Get the strafe direction for circling behavior.
     * Alternates based on tick count for a natural circling pattern.
     */
    public static float getStrafeDirection(Guard guard) {
        // Alternate direction every 3 seconds (60 ticks) for natural feel
        return (guard.tickCount / 60) % 2 == 0 ? 1.0F : -1.0F;
    }
}
