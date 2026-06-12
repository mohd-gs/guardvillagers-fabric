package tallestegg.guardvillagers.common.entities;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Banner Alliance System — manages banner-based team relationships.
 * <p>
 * Core rules:
 * - Same banner = ALLIES (guards help each other, share targets)
 * - Different banner = NEUTRAL (ignore each other, don't help)
 * - No banner = NEUTRAL (default state)
 * - If a guard from team A attacks a guard from team B → WAR between A and B
 * - During war, guards from team A target guards from team B and vice versa
 * - Peace Horn can end wars
 * <p>
 * Banner identification: Uses the serialized pattern layers as a unique team ID.
 * Two banners with the same colors and patterns = same team.
 * Banner items without patterns (plain banners) are still valid — their team ID
 * is based on the base color alone.
 */
public final class BannerAlliance {

    private BannerAlliance() {} // Utility class

    // === War State Tracking ===
    // Key: "teamA:teamB" (sorted alphabetically so "A:B" == "B:A")
    // Value: game time when the war started (for future timeout support)
    private static final ConcurrentHashMap<String, Long> WARS = new ConcurrentHashMap<>();

    // === Banner Team Identification ===

    /**
     * Get the banner team ID for a guard.
     * Returns the banner's unique pattern string, or null if the guard has no banner.
     */
    public static String getBannerTeam(Guard guard) {
        ItemStack banner = guard.getBannerItem();
        return getBannerTeamFromStack(banner);
    }

    /**
     * Get the banner team ID for a player.
     * Checks both hands and the offhand for a banner item.
     * Returns the banner's unique pattern string, or null if no banner found.
     */
    public static String getBannerTeam(Player player) {
        // Check main hand first, then offhand
        ItemStack mainHand = player.getMainHandItem();
        String team = getBannerTeamFromStack(mainHand);
        if (team != null) return team;

        ItemStack offhand = player.getOffhandItem();
        team = getBannerTeamFromStack(offhand);
        return team;
    }

    /**
     * Get the banner team ID for any LivingEntity (guard or player).
     */
    public static String getBannerTeam(LivingEntity entity) {
        if (entity instanceof Guard guard) return getBannerTeam(guard);
        if (entity instanceof Player player) return getBannerTeam(player);
        return null;
    }

    /**
     * Extract a unique team identifier from a banner ItemStack.
     * Uses the base color + pattern layers as the unique ID.
     * Returns null if the item is not a banner.
     */
    private static String getBannerTeamFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (!(stack.getItem() instanceof BannerItem)) return null;

        // Build team ID from banner base color + pattern layers
        StringBuilder sb = new StringBuilder();
        sb.append(stack.getItem().toString());

        // Include pattern layers for unique identification
        var patterns = stack.get(DataComponents.BANNER_PATTERNS);
        if (patterns != null) {
            sb.append(":").append(patterns.hashCode());
        }

        return sb.toString();
    }

    // === Alliance Checks ===

    /**
     * Check if two entities are allies (same banner team).
     * Returns true only if both entities have banners AND they are the same team.
     */
    public static boolean areAllies(LivingEntity a, LivingEntity b) {
        String teamA = getBannerTeam(a);
        String teamB = getBannerTeam(b);
        if (teamA == null || teamB == null) return false;
        return teamA.equals(teamB);
    }

    /**
     * Check if two entities are neutral (different banners or no banners).
     * Neutral entities don't help each other but also don't fight.
     */
    public static boolean areNeutral(LivingEntity a, LivingEntity b) {
        String teamA = getBannerTeam(a);
        String teamB = getBannerTeam(b);
        // Neutral if: either has no banner, OR different banners and not at war
        if (teamA == null || teamB == null) return true;
        if (teamA.equals(teamB)) return false; // Same team = allies, not neutral
        return !isAtWar(teamA, teamB);
    }

    // === War System ===

    /**
     * Declare war between two banner teams.
     * Called when a guard from one team attacks a guard from another team.
     */
    public static void declareWar(String teamA, String teamB) {
        if (teamA == null || teamB == null) return;
        if (teamA.equals(teamB)) return; // Same team, no war
        String warKey = getWarKey(teamA, teamB);
        WARS.putIfAbsent(warKey, System.currentTimeMillis());
    }

    /**
     * Check if two teams are at war.
     */
    public static boolean isAtWar(String teamA, String teamB) {
        if (teamA == null || teamB == null) return false;
        if (teamA.equals(teamB)) return false;
        return WARS.containsKey(getWarKey(teamA, teamB));
    }

    /**
     * Check if an entity is at war with another entity's team.
     */
    public static boolean isAtWar(LivingEntity a, LivingEntity b) {
        String teamA = getBannerTeam(a);
        String teamB = getBannerTeam(b);
        return isAtWar(teamA, teamB);
    }

    /**
     * Make peace for a specific team — end all wars involving this team.
     * Called by PeaceHornItem.
     */
    public static void makePeaceForTeam(String team) {
        if (team == null) return;
        WARS.keySet().removeIf(key -> key.startsWith(team + ":") || key.endsWith(":" + team));
    }

    /**
     * Make peace between two specific teams.
     */
    public static void makePeace(String teamA, String teamB) {
        if (teamA == null || teamB == null) return;
        WARS.remove(getWarKey(teamA, teamB));
    }

    /**
     * Check if a guard should attack another guard based on banner teams.
     * Returns true if:
     * - They are at war (different banners + war declared), OR
     * - They have no banner relationship (vanilla behavior — attack normal enemies)
     * Returns false if:
     * - Same banner team (allies), OR
     * - Different banners but NOT at war (neutral)
     */
    public static boolean shouldAttackGuard(Guard attacker, Guard target) {
        String attackerTeam = getBannerTeam(attacker);
        String targetTeam = getBannerTeam(target);

        // Both have no banners → vanilla behavior (don't attack other guards)
        if (attackerTeam == null && targetTeam == null) return false;

        // Same team → NEVER attack allies
        if (attackerTeam != null && attackerTeam.equals(targetTeam)) return false;

        // One has banner, other doesn't → neutral, don't attack
        if (attackerTeam == null || targetTeam == null) return false;

        // Different banners → only attack if at war
        return isAtWar(attackerTeam, targetTeam);
    }

    /**
     * Called when a guard is hurt by another entity.
     * If the attacker is a guard from a different banner team, declare war.
     */
    public static void onGuardHurtBy(Guard hurtGuard, LivingEntity attacker) {
        if (!(attacker instanceof Guard attackerGuard)) return;

        String hurtTeam = getBannerTeam(hurtGuard);
        String attackerTeam = getBannerTeam(attackerGuard);

        if (hurtTeam != null && attackerTeam != null && !hurtTeam.equals(attackerTeam)) {
            declareWar(hurtTeam, attackerTeam);
        }
    }

    // === Utility ===

    /**
     * Create a consistent war key from two team IDs (sorted alphabetically).
     */
    private static String getWarKey(String teamA, String teamB) {
        // Sort so "A:B" == "B:A"
        if (teamA.compareTo(teamB) < 0) {
            return teamA + ":" + teamB;
        } else {
            return teamB + ":" + teamA;
        }
    }

    /**
     * Get all teams currently at war with the given team.
     */
    public static Set<String> getEnemyTeams(String team) {
        Set<String> enemies = new HashSet<>();
        if (team == null) return enemies;
        for (String key : WARS.keySet()) {
            String[] parts = key.split(":", 2);
            if (parts.length == 2) {
                if (parts[0].equals(team)) enemies.add(parts[1]);
                else if (parts[1].equals(team)) enemies.add(parts[0]);
            }
        }
        return enemies;
    }

    /**
     * Get the total number of active wars (for debugging/stats).
     */
    public static int getActiveWarCount() {
        return WARS.size();
    }

    /**
     * Clear all wars (for server shutdown or admin command).
     */
    public static void clearAllWars() {
        WARS.clear();
    }
}
