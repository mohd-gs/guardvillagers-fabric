package tallestegg.guardvillagers.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import tallestegg.guardvillagers.GuardVillagers;
import tallestegg.guardvillagers.client.renderer.state.GuardRenderState;

/**
 * Renders the banner from the guard's banner slot (slot 6) on the guard's back.
 * The banner is displayed as a colored flag/standard on the back of the guard,
 * making it immediately obvious which team the guard belongs to.
 *
 * This layer uses the existing Guard model's "banner" child part (positioned on
 * the back) and renders it with the banner's base color. The banner part is
 * defined in GuardModel and GuardSteveModel.
 *
 * Guards without a banner have their banner model part hidden.
 */
public class GuardBannerLayer extends RenderLayer<GuardRenderState, HumanoidModel<GuardRenderState>> {

    private static final Identifier BANNER_TEXTURE = Identifier.fromNamespaceAndPath(
            GuardVillagers.MODID, "textures/entity/guard/banner_overlay.png"
    );

    public GuardBannerLayer(RenderLayerParent<GuardRenderState, HumanoidModel<GuardRenderState>> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight,
                       GuardRenderState state, float yRot, float xRot) {
        ItemStack bannerStack = state.bannerItem;
        if (bannerStack == null || bannerStack.isEmpty()) {
            return; // No banner equipped — don't render anything
        }
        if (!(bannerStack.getItem() instanceof BannerItem bannerItem)) {
            return;
        }

        // Get the banner's base color as an RGB int
        int color = getBannerColor(bannerItem);

        // Render the banner model part with the banner's color
        // Uses the same renderColoredCutoutModel approach as GuardVariantLayer
        renderColoredCutoutModel(
                this.getParentModel(),
                BANNER_TEXTURE,
                poseStack,
                nodeCollector,
                packedLight,
                state,
                color,
                0xFF // alpha (fully opaque)
        );
    }

    /**
     * Get the color of a banner item based on its DyeColor.
     */
    private static int getBannerColor(BannerItem bannerItem) {
        try {
            DyeColor dyeColor = bannerItem.getColor();
            return dyeColor.getTextureDiffuseColor();
        } catch (Exception e) {
            // Fallback: white
            return 0xFFFFFF;
        }
    }
}
