package tallestegg.guardvillagers.loot_tables;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import tallestegg.guardvillagers.GuardVillagers;
import tallestegg.guardvillagers.loot_tables.functions.ArmorSlotFunction;

import java.util.function.Consumer;

public class GuardLootTables {
    public static final BiMap<Identifier, ContextKeySet> REGISTRY = HashBiMap.create();
    public static final ContextKeySet SLOT = register("slot", (table) -> {
        table.required(LootContextParams.THIS_ENTITY);
    });

    public static final MapCodec<ArmorSlotFunction> ARMOR_SLOT_CODEC = ArmorSlotFunction.CODEC;

    public static void register() {
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "slot"), ArmorSlotFunction.CODEC);
    }

    public static ContextKeySet register(String p_81429_, Consumer<ContextKeySet.Builder> p_81430_) {
        ContextKeySet.Builder lootcontextparamset$builder = new ContextKeySet.Builder();
        p_81430_.accept(lootcontextparamset$builder);
        ContextKeySet lootcontextparamset = lootcontextparamset$builder.build();
        REGISTRY.put(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, p_81429_), lootcontextparamset);
        return lootcontextparamset;
    }
}
