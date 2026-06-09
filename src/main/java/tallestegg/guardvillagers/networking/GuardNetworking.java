package tallestegg.guardvillagers.networking;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import tallestegg.guardvillagers.GuardVillagers;

public final class GuardNetworking {
    private GuardNetworking() {}

    public static void registerServerReceivers() {
        PayloadTypeRegistry.playS2C().register(GuardOpenInventoryPacket.ID, GuardOpenInventoryPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(GuardFollowPacket.ID, GuardFollowPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(GuardSetPatrolPosPacket.ID, GuardSetPatrolPosPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(GuardFollowPacket.ID, GuardFollowPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(GuardSetPatrolPosPacket.ID, GuardSetPatrolPosPacket::handle);
    }
}
