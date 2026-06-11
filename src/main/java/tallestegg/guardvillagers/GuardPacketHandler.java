package tallestegg.guardvillagers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.common.entities.GuardContainer;
import tallestegg.guardvillagers.networking.GuardOpenInventoryPacket;

public class GuardPacketHandler {
    public static void openGuardInventory(GuardOpenInventoryPacket packet) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Entity entity = player.level().getEntity(packet.entityId());
            if (entity instanceof Guard guard) {
                net.minecraft.client.player.LocalPlayer clientplayerentity = Minecraft.getInstance().player;
                GuardContainer container = new GuardContainer(packet.id(), player.getInventory(), guard.guardInventory, guard);
                clientplayerentity.containerMenu = container;
                Minecraft.getInstance().setScreen(new tallestegg.guardvillagers.client.gui.GuardInventoryScreen(container, player.getInventory(), guard));
            }
        }
    }
}
