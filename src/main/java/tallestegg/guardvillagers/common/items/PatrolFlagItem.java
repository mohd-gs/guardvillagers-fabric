package tallestegg.guardvillagers.common.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Patrol Flag item - allows players to define patrol waypoints for guards.
 *
 * - Right-click on a block: Sets that block as a patrol waypoint (stored in item NBT via CustomData)
 * - Sneak + right-click on a guard: Shows the guard's current patrol route info
 * - Right-click on a guard with the flag: Assigns the stored waypoints to the guard
 */
public class PatrolFlagItem extends Item {

    public PatrolFlagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = player.getItemInHand(hand);

        // Right-click on a block: add waypoint from item's stored waypoints
        HitResult hitResult = player.pick(5.0, 1.0F, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hitResult).getBlockPos();
            List<BlockPos> waypoints = getWaypoints(stack);
            if (waypoints.size() >= GuardConfig.COMMON.maxPatrolWaypoints) {
                player.sendSystemMessage(Component.literal("Patrol route full! Max " + GuardConfig.COMMON.maxPatrolWaypoints + " waypoints.")
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS_SERVER;
            }
            waypoints.add(pos);
            setWaypoints(stack, waypoints);
            player.sendSystemMessage(Component.literal("Added waypoint #" + waypoints.size() + " at [" +
                    pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
                    .withStyle(ChatFormatting.GREEN));
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Guard guard)) return InteractionResult.PASS;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;

        if (player.isCrouching()) {
            // Sneak + right-click on a guard: show patrol route info
            List<BlockPos> guardWaypoints = guard.getPatrolWaypoints();
            if (guardWaypoints.isEmpty()) {
                player.sendSystemMessage(Component.literal("This guard has no patrol route.")
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                player.sendSystemMessage(Component.literal("Guard patrol route (" + guardWaypoints.size() + " waypoints):")
                        .withStyle(ChatFormatting.AQUA));
                for (int i = 0; i < guardWaypoints.size(); i++) {
                    BlockPos wp = guardWaypoints.get(i);
                    player.sendSystemMessage(Component.literal("  " + (i + 1) + ": [" +
                            wp.getX() + ", " + wp.getY() + ", " + wp.getZ() + "]")
                            .withStyle(ChatFormatting.WHITE));
                }
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        // Right-click on a guard: assign stored waypoints
        List<BlockPos> waypoints = getWaypoints(stack);
        if (waypoints.isEmpty()) {
            player.sendSystemMessage(Component.literal("No waypoints stored on this flag! Right-click blocks first to add waypoints.")
                    .withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS_SERVER;
        }

        guard.setPatrolWaypoints(new ArrayList<>(waypoints));
        guard.setPatrolling(true);
        guard.setCurrentWaypointIndex(0);
        // Set the first waypoint as the patrol position for the existing WalkBackToCheckPointGoal
        if (!waypoints.isEmpty()) {
            guard.setPatrolPos(waypoints.get(0));
        }
        player.sendSystemMessage(Component.literal("Assigned " + waypoints.size() + " patrol waypoints to guard.")
                .withStyle(ChatFormatting.GREEN));
        guard.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        return InteractionResult.SUCCESS_SERVER;
    }

    // === DataComponent Helper Methods ===

    public static List<BlockPos> getWaypoints(ItemStack stack) {
        List<BlockPos> waypoints = new ArrayList<>();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) return waypoints;
        CompoundTag tag = customData.copyTag();
        ListTag list = tag.getListOrEmpty("waypoints");
        for (int i = 0; i < list.size(); i++) {
            int[] arr = list.getIntArray(i).orElse(null);
            if (arr != null && arr.length >= 3) {
                waypoints.add(new BlockPos(arr[0], arr[1], arr[2]));
            }
        }
        return waypoints;
    }

    public static void setWaypoints(ItemStack stack, List<BlockPos> waypoints) {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (BlockPos pos : waypoints) {
            list.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tag.put("waypoints", list);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        List<BlockPos> waypoints = getWaypoints(stack);
        if (waypoints.isEmpty()) {
            tooltip.accept(Component.literal("No waypoints set").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.literal("Waypoints: " + waypoints.size() + "/" + GuardConfig.COMMON.maxPatrolWaypoints)
                    .withStyle(ChatFormatting.AQUA));
            for (int i = 0; i < waypoints.size(); i++) {
                BlockPos pos = waypoints.get(i);
                tooltip.accept(Component.literal("  " + (i + 1) + ": [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]")
                        .withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        tooltip.accept(Component.literal("Right-click block: Add waypoint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.literal("Right-click guard: Assign route").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.literal("Sneak+Right-click guard: View route").withStyle(ChatFormatting.DARK_GRAY));
    }
}
