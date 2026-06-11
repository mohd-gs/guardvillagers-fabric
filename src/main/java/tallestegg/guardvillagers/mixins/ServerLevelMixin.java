package tallestegg.guardvillagers.mixins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tallestegg.guardvillagers.HandlerEvents;

/**
 * Mixin to inject event handling when entities are added to the server level.
 * Uses onEntityLoad which is the correct method name in HandlerEvents.
 */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "addFreshEntity", at = @At("HEAD"))
    private void guardvillagers$onAddEntity(net.minecraft.world.entity.Entity entity, CallbackInfo ci) {
        if (entity instanceof Mob mob) {
            HandlerEvents.onEntityLoad(entity, (ServerLevel) (Object) this);
        }
    }
}
