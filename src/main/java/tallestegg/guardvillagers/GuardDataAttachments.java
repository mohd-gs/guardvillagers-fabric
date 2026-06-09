package tallestegg.guardvillagers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces NeoForge AttachmentType system for storing data on Villager entities.
 * Uses a static Map keyed by entity UUID. Data is periodically cleaned up
 * when entities are removed.
 */
public class GuardDataAttachments {
    // Per-villager data stored by entity UUID
    private static final Map<UUID, Integer> TIMES_THROWN_POTION = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> TIMES_HEALED_GOLEM = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> TIMES_REPAIRED_GUARD = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REPAIRED_GOLEM = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_THROWN_POTION = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_REPAIRED_GUARD = new ConcurrentHashMap<>();

    // Times thrown potion
    public static int getTimesThrownPotion(UUID uuid) {
        return TIMES_THROWN_POTION.getOrDefault(uuid, 0);
    }

    public static void setTimesThrownPotion(UUID uuid, int value) {
        TIMES_THROWN_POTION.put(uuid, value);
    }

    public static void incrementTimesThrownPotion(UUID uuid) {
        TIMES_THROWN_POTION.merge(uuid, 1, Integer::sum);
    }

    // Times healed golem
    public static int getTimesHealedGolem(UUID uuid) {
        return TIMES_HEALED_GOLEM.getOrDefault(uuid, 0);
    }

    public static void setTimesHealedGolem(UUID uuid, int value) {
        TIMES_HEALED_GOLEM.put(uuid, value);
    }

    public static void incrementTimesHealedGolem(UUID uuid) {
        TIMES_HEALED_GOLEM.merge(uuid, 1, Integer::sum);
    }

    // Times repaired guard
    public static int getTimesRepairedGuard(UUID uuid) {
        return TIMES_REPAIRED_GUARD.getOrDefault(uuid, 0);
    }

    public static void setTimesRepairedGuard(UUID uuid, int value) {
        TIMES_REPAIRED_GUARD.put(uuid, value);
    }

    public static void incrementTimesRepairedGuard(UUID uuid) {
        TIMES_REPAIRED_GUARD.merge(uuid, 1, Integer::sum);
    }

    // Last repaired golem
    public static long getLastRepairedGolem(UUID uuid) {
        return LAST_REPAIRED_GOLEM.getOrDefault(uuid, 0L);
    }

    public static void setLastRepairedGolem(UUID uuid, long value) {
        LAST_REPAIRED_GOLEM.put(uuid, value);
    }

    // Last thrown potion
    public static long getLastThrownPotion(UUID uuid) {
        return LAST_THROWN_POTION.getOrDefault(uuid, 0L);
    }

    public static void setLastThrownPotion(UUID uuid, long value) {
        LAST_THROWN_POTION.put(uuid, value);
    }

    // Last repaired guard
    public static long getLastRepairedGuard(UUID uuid) {
        return LAST_REPAIRED_GUARD.getOrDefault(uuid, 0L);
    }

    public static void setLastRepairedGuard(UUID uuid, long value) {
        LAST_REPAIRED_GUARD.put(uuid, value);
    }

    // Cleanup when entity is removed
    public static void removeEntity(UUID uuid) {
        TIMES_THROWN_POTION.remove(uuid);
        TIMES_HEALED_GOLEM.remove(uuid);
        TIMES_REPAIRED_GUARD.remove(uuid);
        LAST_REPAIRED_GOLEM.remove(uuid);
        LAST_THROWN_POTION.remove(uuid);
        LAST_REPAIRED_GUARD.remove(uuid);
    }
}
