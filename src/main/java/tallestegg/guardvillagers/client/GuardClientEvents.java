package tallestegg.guardvillagers.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.resources.Identifier;
import tallestegg.guardvillagers.GuardEntityType;
import tallestegg.guardvillagers.GuardVillagers;
import tallestegg.guardvillagers.client.models.GuardArmorModel;
import tallestegg.guardvillagers.client.models.GuardModel;
import tallestegg.guardvillagers.client.models.GuardSteveModel;
import tallestegg.guardvillagers.client.renderer.GuardRenderer;
import tallestegg.guardvillagers.client.network.GuardPacketHandler;
import tallestegg.guardvillagers.networking.GuardOpenInventoryPacket;

public final class GuardClientEvents implements ClientModInitializer {
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GuardVillagers.MODID, path);
    }

    public static final ModelLayerLocation GUARD = new ModelLayerLocation(id("guard"), "main");
    public static final ModelLayerLocation GUARD_STEVE = new ModelLayerLocation(id("guard_steve"), "main");
    public static final ModelLayerLocation GUARD_ARMOR_HEAD = new ModelLayerLocation(id("guard_armor/head"), "main");
    public static final ModelLayerLocation GUARD_ARMOR_CHEST = new ModelLayerLocation(id("guard_armor/chest"), "main");
    public static final ModelLayerLocation GUARD_ARMOR_LEGS = new ModelLayerLocation(id("guard_armor/legs"), "main");
    public static final ModelLayerLocation GUARD_ARMOR_FEET = new ModelLayerLocation(id("guard_armor/feet"), "main");
    public static final ArmorModelSet<ModelLayerLocation> GUARD_ARMOR = new ArmorModelSet<>(GUARD_ARMOR_HEAD, GUARD_ARMOR_CHEST, GUARD_ARMOR_LEGS, GUARD_ARMOR_FEET);
    public static final ModelLayerLocation GUARD_STEVE_ARMOR_HEAD = new ModelLayerLocation(id("guard_steve_armor/head"), "main");
    public static final ModelLayerLocation GUARD_STEVE_ARMOR_CHEST = new ModelLayerLocation(id("guard_steve_armor/chest"), "main");
    public static final ModelLayerLocation GUARD_STEVE_ARMOR_LEGS = new ModelLayerLocation(id("guard_steve_armor/legs"), "main");
    public static final ModelLayerLocation GUARD_STEVE_ARMOR_FEET = new ModelLayerLocation(id("guard_steve_armor/feet"), "main");
    public static final ArmorModelSet<ModelLayerLocation> GUARD_STEVE_ARMOR = new ArmorModelSet<>(GUARD_STEVE_ARMOR_HEAD, GUARD_STEVE_ARMOR_CHEST, GUARD_STEVE_ARMOR_LEGS, GUARD_STEVE_ARMOR_FEET);

    @Override
    public void onInitializeClient() {
        // Register layer definitions
        ModelLayerRegistry.registerModelLayer(GUARD, GuardModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(GUARD_STEVE, GuardSteveModel::createMesh);
        registerArmorLayerDefs(GUARD_ARMOR, GuardArmorModel.createArmorMeshSet(new CubeDeformation(0.5F), new CubeDeformation(1.0F)));
        registerArmorLayerDefs(GUARD_STEVE_ARMOR, HumanoidModel.createArmorMeshSet(new CubeDeformation(0.5F), new CubeDeformation(1.0F)));

        // Register entity renderer
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(GuardEntityType.GUARD, GuardRenderer::new);

        // Register client packet receiver
        ClientPlayNetworking.registerGlobalReceiver(GuardOpenInventoryPacket.ID, (payload, context) -> {
            context.client().execute(() -> {
                GuardPacketHandler.openGuardInventory(payload);
            });
        });
    }

    private static void registerArmorLayerDefs(ArmorModelSet<ModelLayerLocation> targets, ArmorModelSet<MeshDefinition> meshes) {
        ModelLayerRegistry.registerModelLayer(targets.head(), () -> LayerDefinition.create(meshes.head(), 64, 32));
        ModelLayerRegistry.registerModelLayer(targets.chest(), () -> LayerDefinition.create(meshes.chest(), 64, 32));
        ModelLayerRegistry.registerModelLayer(targets.legs(), () -> LayerDefinition.create(meshes.legs(), 64, 32));
        ModelLayerRegistry.registerModelLayer(targets.feet(), () -> LayerDefinition.create(meshes.feet(), 64, 32));
    }
}
