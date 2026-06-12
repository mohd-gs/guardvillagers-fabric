package tallestegg.guardvillagers.common.entities.ai.goals;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.phys.AABB;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Smart target prioritization for guards.
 * <p>
 * Instead of just attacking the nearest enemy, guards now consider:
 * <ul>
 *   <li><b>Threat level</b>: Ranged attackers and creepers are higher priority</li>
 *   <li><b>Weapon affinity</b>: Axe guards prioritize shielded enemies;
 *       Spear guards prioritize mounted enemies</li>
 *   <li><b>Raid priority</b>: During raids, raiders are highest priority</li>
 *   <li><b>Proximity</b>: Close enemies are generally higher priority than far ones</li>
 *   <li><b>Health</b>: Nearly-dead enemies are prioritized for quick elimination</li>
 * </ul>
 * <p>
 * This extends TargetGoal directly (not NearestAttackableTargetGoal) to avoid
 * depending on private superclass fields that may change between MC versions.
 */
public class TargetPrioritizationGoal extends TargetGoal {
    private final Guard guard;
    private final int randomInterval;
    private final BiPredicate<LivingEntity, ServerLevel> filter;
    private int intervalCounter;
    private LivingEntity selectedTarget;

    public TargetPrioritizationGoal(Guard guard, int randomInterval, boolean mustSee, BiPredicate<LivingEntity, ServerLevel> filter) {
        super(guard, mustSee, true);
        this.guard = guard;
        this.randomInterval = randomInterval;
        this.filter = filter;
        this.intervalCounter = reducedTickDelay(randomInterval);
    }

    @Override
    public boolean canUse() {
        if (this.intervalCounter > 0) {
            this.intervalCounter--;
            return false;
        }
        this.intervalCounter = reducedTickDelay(this.randomInterval);

        this.selectedTarget = findBestTarget();
        return this.selectedTarget != null;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.guard.getTarget();
        if (target == null || !target.isAlive()) return false;
        // Stop if target is too far away
        return this.guard.distanceToSqr(target) <= this.getFollowDistance() * this.getFollowDistance();
    }

    @Override
    public void start() {
        this.guard.setTarget(this.selectedTarget);
        super.start();
    }

    @Override
    public void stop() {
        this.selectedTarget = null;
        super.stop();
    }

    /**
     * Find the best target using smart prioritization.
     * Does not depend on any private superclass fields.
     */
    private LivingEntity findBestTarget() {
        double followRange = this.getFollowDistance();
        AABB searchBox = this.guard.getBoundingBox().inflate(followRange, 4.0D, followRange);

        // Build targeting conditions
        TargetingConditions conditions = TargetingConditions.forCombat()
                .range(followRange)
                .selector((entity, level) -> {
                    if (!(entity instanceof LivingEntity living)) return false;
                    if (entity == this.guard || !entity.isAlive()) return false;
                    if (!this.guard.canAttack(living)) return false;
                    // Apply the user-provided filter (e.g., Enemy check, blacklist)
                    return this.filter == null || this.filter.test(living, level instanceof ServerLevel sl ? sl : null);
                });

        // Get all potential targets in range
        List<LivingEntity> candidates = this.guard.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> conditions.test((ServerLevel)this.guard.level(), this.guard, e)
        );

        if (candidates.isEmpty()) return null;

        // Sort by priority (lower score = higher priority)
        candidates.sort((a, b) -> {
            double scoreA = calculateTargetPriority(a);
            double scoreB = calculateTargetPriority(b);
            return Double.compare(scoreA, scoreB);
        });

        // Pick the highest-priority target
        return candidates.get(0);
    }

    /**
     * Calculate a priority score for a potential target.
     * Lower score = higher priority (will be attacked first).
     * <p>
     * Score factors:
     * - Distance: closer = higher priority (base score)
     * - Threat: ranged attackers, creepers, and low-health enemies get bonus priority
     * - Weapon affinity: weapon-specific priorities
     * - Raid: raiders during raids get top priority
     */
    private double calculateTargetPriority(LivingEntity target) {
        double distance = guard.distanceTo(target);
        double score = distance; // Base: closer = higher priority

        if (!GuardConfig.COMMON.weaponSpecificBehavior) return score;

        // === Threat-Based Prioritization ===

        // Creepers are EXTREMELY high priority (they can blow up the village)
        if (target instanceof Creeper creeper) {
            if (creeper.getSwellDir() > 0) {
                score -= 50.0D; // Actively swelling — urgent threat
            } else {
                score -= 20.0D; // Creeper nearby — high priority
            }
        }

        // Ranged attackers are high priority (they can chip away at guards from safety)
        if (target instanceof RangedAttackMob) {
            score -= 15.0D;
        }

        // Raiders during raids get top priority
        if (target instanceof Raider raider && raider.hasActiveRaid()) {
            score -= 25.0D;
        }

        // Low-health enemies: prioritize finishing them off
        float healthRatio = target.getHealth() / target.getMaxHealth();
        if (healthRatio < 0.3D) {
            score -= 10.0D; // Nearly dead — finish them
        } else if (healthRatio < 0.5D) {
            score -= 5.0D; // Wounded — good target
        }

        // === Weapon Affinity Prioritization ===

        WeaponBehavior.WeaponType myWeapon = WeaponBehavior.getWeaponType(guard);

        // Axe guards: prioritize shielded enemies (to break their shields)
        if (myWeapon == WeaponBehavior.WeaponType.AXE && Guard.isActivelyBlocking(target)) {
            score -= 12.0D;
        }

        // Spear/Trident guards: prioritize mounted enemies (bonus damage)
        if ((myWeapon == WeaponBehavior.WeaponType.SPEAR || myWeapon == WeaponBehavior.WeaponType.TRIDENT)
                && target.getVehicle() != null) {
            score -= 10.0D;
        }

        // Mace guards: prioritize armored enemies (mace is good vs armor)
        if (myWeapon == WeaponBehavior.WeaponType.MACE) {
            float armorValue = target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) > 0
                    ? (float) target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
                    : 0.0F;
            if (armorValue > 5.0F) {
                score -= 8.0D;
            }
        }

        // Sword guards: prioritize unshielded enemies (easier to hit)
        if (myWeapon == WeaponBehavior.WeaponType.SWORD && !target.isBlocking()) {
            score -= 3.0D;
        }

        // Ranged guards: prioritize flying enemies and other ranged attackers
        if (myWeapon == WeaponBehavior.WeaponType.BOW || myWeapon == WeaponBehavior.WeaponType.CROSSBOW) {
            if (!target.onGround()) {
                score -= 8.0D; // Flying target — ranged can reach it
            }
            if (target instanceof RangedAttackMob) {
                score -= 5.0D; // Counter-snipe
            }
        }

        // Targets currently attacking this guard or a nearby guard: high priority
        if (target instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() instanceof Guard) {
            score -= 7.0D;
        }
        if (target.getLastHurtMob() instanceof Guard) {
            score -= 5.0D;
        }

        return score;
    }
}
