package tallestegg.guardvillagers;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tallestegg.guardvillagers.GuardSounds;
import tallestegg.guardvillagers.common.commands.GuardCommands;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.configuration.GuardConfig;
import tallestegg.guardvillagers.loot_tables.GuardLootTables;
import tallestegg.guardvillagers.networking.GuardNetworking;

public class GuardVillagers implements ModInitializer {
    public static final String MODID = "guardvillagers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private static final ResourceKey<CreativeModeTab> SPAWN_EGGS_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "spawn_eggs"));

    @Override
    public void onInitialize() {
        GuardConfig.load();

        GuardEntityType.register();
        GuardItems.register();
        GuardSounds.register();
        GuardStats.register();
        GuardLootTables.register();
        GuardNetworking.registerServerReceivers();
        GuardCommands.register();

        FabricDefaultAttributeRegistry.register(GuardEntityType.GUARD, Guard.createAttributes());

        CreativeModeTabEvents.modifyOutputEvent(SPAWN_EGGS_TAB).register(output -> {
            output.accept(GuardItems.GUARD_SPAWN_EGG);
            output.accept(GuardItems.ILLUSIONER_SPAWN_EGG);
        });

        HandlerEvents.register();

        GuardDataAttachments.registerCleanup();

        UseEntityCallback.EVENT.register(HandlerEvents::onEntityInteract);
        // 26.1.x note: Fabric added BlockEvents#USE_ITEM_ON, ItemEvents#USE_ON, ItemEvents#USE
        // as more granular alternatives. UseBlockCallback remains supported but consider
        // migrating to the new events for better vanilla parity in a future update.
        UseBlockCallback.EVENT.register(HandlerEvents::onBlockInteract);
        ServerEntityEvents.ENTITY_LOAD.register(HandlerEvents::onEntityLoad);
    }

    public static boolean hotvChecker(Player player, Guard guard) {
        return player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardConfig.COMMON.giveGuardStuffHOTV
                || !GuardConfig.COMMON.giveGuardStuffHOTV || guard.getPlayerReputation(player) > GuardConfig.COMMON.reputationRequirement && !player.level().isClientSide();
    }

    public static boolean canFollow(Player player) {
        return GuardConfig.COMMON.followHero && player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) || !GuardConfig.COMMON.followHero;
    }

    public static String removeModIdFromVillagerType(String stringWithModId) {
        // Use indexOf instead of split to handle strings with multiple colons correctly
        int idx = stringWithModId.indexOf(':');
        return idx >= 0 ? stringWithModId.substring(idx + 1) : stringWithModId;
    }

    /**
     * Public API replacement for Entity.getEncodeId() which is protected in MC 26.1.x.
     * Returns the entity type ID string (e.g. "minecraft:zombie").
     */
    public static String getEntityTypeId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }
}
