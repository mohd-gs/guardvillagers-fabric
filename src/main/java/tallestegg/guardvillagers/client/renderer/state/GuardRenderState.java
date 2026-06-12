package tallestegg.guardvillagers.client.renderer.state;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemStack;

public class GuardRenderState extends HumanoidRenderState {
    public int kickTicks;
    public boolean aggressive;
    public boolean eating;
    public boolean blocking;
    public double horizontalSpeedSqr;
    public boolean holdingShootable;
    public boolean showQuiver;
    public boolean showShoulderPads;
    public boolean mainHandEmpty;
    public boolean offHandEmpty;
    public ItemUseAnimation mainHandUseAnimation = ItemUseAnimation.NONE;
    public ItemUseAnimation offHandUseAnimation  = ItemUseAnimation.NONE;
    public String variant = "plains";
    /** Banner item from slot 6 — used by GuardBannerLayer to render on back */
    public ItemStack bannerItem = ItemStack.EMPTY;
}
