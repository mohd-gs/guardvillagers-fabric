package tallestegg.guardvillagers.common.items;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import tallestegg.guardvillagers.GuardVillagers;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

public class WarHornItem extends Item {
    public WarHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Play raid horn sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAID_HORN, SoundSource.PLAYERS, 1.0F, 1.0F);

        // Find all guards in range
        double range = GuardConfig.COMMON.warHornRange;
        AABB searchBox = player.getBoundingBox().inflate(range);

        // Also find nearby enemies so guards can be given targets directly
        AABB enemySearchBox = player.getBoundingBox().inflate(32.0D);
        LivingEntity nearestEnemy = null;
        double nearestDist = Double.MAX_VALUE;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, enemySearchBox)) {
            if (mob instanceof Enemy && mob.isAlive() && !GuardConfig.COMMON.isBlackListed(GuardVillagers.getEntityTypeId(mob))) {
                double d = player.distanceToSqr(mob);
                if (d < nearestDist) {
                    nearestDist = d;
                    nearestEnemy = mob;
                }
            }
        }

        for (Guard guard : level.getEntitiesOfClass(Guard.class, searchBox)) {
            guard.setCombatStanceTicks(GuardConfig.COMMON.warHornCombatDurationSeconds * 20);
            // If the guard has no target, try to give it the nearest enemy
            // or have it navigate toward the player
            if (guard.getTarget() == null || !guard.getTarget().isAlive()) {
                if (nearestEnemy != null && guard.canAttack(nearestEnemy)) {
                    guard.setTarget(nearestEnemy);
                } else {
                    guard.getNavigation().moveTo(player, 1.2D);
                }
            }
        }

        // Damage item instead of cooldown
        stack.hurtAndBreak(1, player, hand);
        return InteractionResult.SUCCESS_SERVER;
    }
}
