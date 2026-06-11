package tallestegg.guardvillagers.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import tallestegg.guardvillagers.GuardVillagers;

public final class GuardNetworking {
    private GuardNetworking() {}

    public static void registerServerReceivers() {
        PayloadTypeRegistry.clientboundPlay().register(GuardOpenInventoryPacket.ID, GuardOpenInventoryPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GuardFollowPacket.ID, GuardFollowPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GuardSetPatrolPosPacket.ID, GuardSetPatrolPosPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(GuardFollowPacket.ID, GuardFollowPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(GuardSetPatrolPosPacket.ID, GuardSetPatrolPosPacket::handle);
    }
}
