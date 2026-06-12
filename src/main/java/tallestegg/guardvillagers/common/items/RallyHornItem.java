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
 * Rally Horn — Teleports all guards with the same banner as the player
 * to the player's position, regardless of distance.
 *
 * Recipe: Goat Horn surrounded by 8 Gold Ingots
 */
public class RallyHornItem extends Item {
    public RallyHornItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        // Play horn sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAID_HORN, SoundSource.PLAYERS, 1.0F, 1.5F);

        // Get the player's banner team
        String playerTeam = BannerAlliance.getBannerTeam(player);

        if (playerTeam == null) {
            // Player has no banner — cannot rally
            return InteractionResult.FAIL;
        }

        // Find ALL guards in the world with the same banner team and teleport them
        if (level instanceof ServerLevel serverLevel) {
            for (Guard guard : serverLevel.getEntitiesOfClass(Guard.class, new net.minecraft.world.phys.AABB(-30000000, -64, -30000000, 30000000, 320, 30000000))) {
                if (!guard.isAlive()) continue;
                String guardTeam = BannerAlliance.getBannerTeam(guard);
                if (playerTeam.equals(guardTeam)) {
                    // Teleport the guard to the player
                    guard.teleportTo(player.getX(), player.getY(), player.getZ());
                    guard.getNavigation().stop(); // Reset navigation after teleport
                    guard.setTarget(null); // Clear target so they don't run back
                }
            }
        }

        // Damage item
        stack.hurtAndBreak(1, player, hand);
        return InteractionResult.SUCCESS_SERVER;
    }
}
