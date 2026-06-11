package tallestegg.guardvillagers.mixins;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor mixin for ServerPlayer fields.
 * Uses @Invoker from mixin-gen package (Fabric 26.1.2).
 */
@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {

    @Invoker("nextContainerCounter")
    void callNextContainerCounter();

    @Invoker("initMenu")
    void callInitMenu(net.minecraft.world.inventory.AbstractContainerMenu menu);
}
