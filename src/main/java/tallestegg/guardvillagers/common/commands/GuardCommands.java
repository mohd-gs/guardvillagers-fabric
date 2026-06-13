package tallestegg.guardvillagers.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import tallestegg.guardvillagers.GuardVillagers;
import tallestegg.guardvillagers.configuration.GuardConfig;

/**
 * Registers /guardvillagers commands for in-game configuration.
 * Currently supports:
 * - /guardvillagers difficulty <HIGH|LOW>  — changes difficulty without restart
 * - /guardvillagers difficulty              — shows current difficulty
 */
public class GuardCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerDifficultyCommand(dispatcher);
        });
    }

    private static void registerDifficultyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("guardvillagers")
                .requires(source -> source.permissions().hasPermission(
                    new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)
                ))
                .then(
                    Commands.literal("difficulty")
                        .executes(ctx -> showDifficulty(ctx))
                        .then(
                            Commands.literal("HIGH")
                                .executes(ctx -> setDifficulty(ctx, "HIGH"))
                        )
                        .then(
                            Commands.literal("LOW")
                                .executes(ctx -> setDifficulty(ctx, "LOW"))
                        )
                )
        );
    }

    /**
     * Shows the current difficulty level to the player.
     */
    private static int showDifficulty(CommandContext<CommandSourceStack> ctx) {
        String current = GuardConfig.COMMON.guardDifficulty;
        ctx.getSource().sendSuccess(
            () -> Component.translatable("guardvillagers.command.difficulty.current", current),
            false
        );
        return 1;
    }

    /**
     * Sets the difficulty level, saves config, and applies changes immediately.
     * No server restart required — the config is saved to disk and all
     * difficulty multipliers are read dynamically from GuardConfig at runtime.
     */
    private static int setDifficulty(CommandContext<CommandSourceStack> ctx, String difficulty) {
        String oldDifficulty = GuardConfig.COMMON.guardDifficulty;

        if (oldDifficulty.equalsIgnoreCase(difficulty)) {
            ctx.getSource().sendSuccess(
                () -> Component.translatable("guardvillagers.command.difficulty.same", difficulty),
                false
            );
            return 0;
        }

        GuardConfig.COMMON.guardDifficulty = difficulty;
        GuardConfig.save();

        // Apply attribute changes to all existing guards immediately
        applyDifficultyToAllGuards(ctx);

        ctx.getSource().sendSuccess(
            () -> Component.translatable("guardvillagers.command.difficulty.changed", oldDifficulty, difficulty),
            true
        );
        return 1;
    }

    /**
     * Applies the new difficulty settings to all currently loaded guards.
     * This updates their movement speed attribute immediately.
     * Other multipliers (attack cooldown, ranged accuracy, etc.) are read
     * dynamically from GuardConfig at runtime, so they apply automatically.
     */
    private static void applyDifficultyToAllGuards(CommandContext<CommandSourceStack> ctx) {
        var server = ctx.getSource().getServer();
        for (var level : server.getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (entity instanceof tallestegg.guardvillagers.common.entities.Guard guard) {
                    guard.applyDifficultySpeed();
                }
            }
        }
    }
}
