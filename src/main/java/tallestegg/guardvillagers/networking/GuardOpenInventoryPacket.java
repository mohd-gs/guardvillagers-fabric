package tallestegg.guardvillagers.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import tallestegg.guardvillagers.GuardVillagers;

public record GuardOpenInventoryPacket(int id, int size, int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GuardOpenInventoryPacket> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "open_inventory"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuardOpenInventoryPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, GuardOpenInventoryPacket::id,
            ByteBufCodecs.INT, GuardOpenInventoryPacket::size,
            ByteBufCodecs.INT, GuardOpenInventoryPacket::entityId,
            GuardOpenInventoryPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
