package tallestegg.guardvillagers.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import tallestegg.guardvillagers.GuardVillagers;

public record GuardOpenInventoryPacket(int id, int size, int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GuardOpenInventoryPacket> ID =
            new CustomPacketPayload.Type<>(Identifier.of(GuardVillagers.MODID, "open_inventory"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuardOpenInventoryPacket> CODEC = StreamCodec.composite(
            StreamCodec.of(RegistryFriendlyByteBuf::writeInt, RegistryFriendlyByteBuf::readInt, GuardOpenInventoryPacket::id),
            StreamCodec.of(RegistryFriendlyByteBuf::writeInt, RegistryFriendlyByteBuf::readInt, GuardOpenInventoryPacket::size),
            StreamCodec.of(RegistryFriendlyByteBuf::writeInt, RegistryFriendlyByteBuf::readInt, GuardOpenInventoryPacket::entityId),
            GuardOpenInventoryPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
