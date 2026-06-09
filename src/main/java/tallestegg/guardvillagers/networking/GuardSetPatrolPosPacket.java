package tallestegg.guardvillagers.networking;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import tallestegg.guardvillagers.GuardVillagers;
import tallestegg.guardvillagers.common.entities.Guard;

public record GuardSetPatrolPosPacket(int entityId, boolean pressed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GuardSetPatrolPosPacket> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "set_patrol_pos"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuardSetPatrolPosPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, GuardSetPatrolPosPacket::entityId,
            ByteBufCodecs.BOOL, GuardSetPatrolPosPacket::pressed,
            GuardSetPatrolPosPacket::new
    );

    public static void handle(GuardSetPatrolPosPacket payload, ServerPlayNetworking.Context context) {
        ServerPlayer sp = context.player();
        var entity = sp.level().getEntity(payload.entityId());
        if (entity instanceof Guard guard) {
            guard.setPatrolling(payload.pressed());
            if (payload.pressed()) {
                guard.setPatrolPos(sp.blockPosition());
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
