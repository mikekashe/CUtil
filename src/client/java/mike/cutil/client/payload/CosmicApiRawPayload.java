package mike.cutil.client.payload;

import mike.cutil.client.CutilClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record CosmicApiRawPayload(byte[] payloadBytes) implements CustomPayload {

    public static final CustomPayload.Id<CosmicApiRawPayload> ID = new Id<>(CutilClient.CHANNEL_ID);

    public static final PacketCodec<PacketByteBuf, CosmicApiRawPayload> CODEC =
            PacketCodec.of(
                    (value, buf) -> buf.writeByteArray(value.payloadBytes()),
                    buf -> new CosmicApiRawPayload(buf.readByteArray())
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
