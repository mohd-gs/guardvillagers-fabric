package tallestegg.guardvillagers.common.items;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.common.entities.BannerAlliance;

/**
 * Peace Horn — Ends war with all enemy banner teams.
 * When blown, all wars involving the player's banner team are ended,
 * and all guards return to neutral stance.
 *
 * Recipe: Goat Horn surrounded by 8 Emeralds
 */
public class PeaceHornItem extends Item {
    public PeaceHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Play a peaceful sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 0.8F);

        // Get the player's banner team
        String playerTeam = BannerAlliance.getBannerTeam(player);

        if (playerTeam == null) {
            // Player has no banner — nothing to make peace for
            return InteractionResult.FAIL;
        }

        // End ALL wars involving this team
        BannerAlliance.makePeaceForTeam(playerTeam);

        // Clear targets on all guards that were fighting for this team
        if (level instanceof ServerLevel serverLevel) {
            for (Guard guard : serverLevel.getEntitiesOfClass(Guard.class, new net.minecraft.world.phys.AABB(-30000000, -64, -30000000, 30000000, 320, 30000000))) {
                if (!guard.isAlive()) continue;
                String guardTeam = BannerAlliance.getBannerTeam(guard);
                if (playerTeam.equals(guardTeam)) {
                    // Clear combat target — return to neutral
                    guard.setTarget(null);
                    guard.setAggressive(false);
                }
            }
        }

        // Damage item
        stack.hurtAndBreak(1, player, hand);
        return InteractionResult.SUCCESS_SERVER;
    }
}
