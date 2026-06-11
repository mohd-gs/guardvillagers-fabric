package tallestegg.guardvillagers.networking;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import tallestegg.guardvillagers.GuardSounds;
import tallestegg.guardvillagers.GuardVillagers;
import tallestegg.guardvillagers.common.entities.Guard;

public record GuardFollowPacket(int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GuardFollowPacket> ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "following"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuardFollowPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, GuardFollowPacket::entityId,
            GuardFollowPacket::new
    );

    public static void handle(GuardFollowPacket packet, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (player != null && player.level() instanceof ServerLevel) {
            Entity entity = player.level().getEntity(packet.entityId());
            if (entity instanceof Guard guard) {
                guard.setFollowing(!guard.isFollowing());
                guard.setOwnerId(player.getUUID());
                guard.playSound(GuardSounds.GUARD_YES, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
