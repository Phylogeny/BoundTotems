package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.client.ModPropertyGetters;
import com.github.phylogeny.boundtotems.util.PacketBufferUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PacketUpdateBoundCompass {
    private final Set<Vector3d> positions;
    private final UUID id;

    public PacketUpdateBoundCompass(Set<Vector3d> positions, UUID id) {
        this.positions = positions;
        this.id = id;
    }

    public static void encode(PacketUpdateBoundCompass msg, PacketBuffer buf) {
        buf.writeInt(msg.positions.size());
        msg.positions.forEach(pos -> PacketBufferUtil.writeVec(buf, pos));
        buf.writeUUID(msg.id);
    }

    public static PacketUpdateBoundCompass decode(PacketBuffer buf) {
        return new PacketUpdateBoundCompass(IntStream.range(0, buf.readInt()).mapToObj(i -> PacketBufferUtil.readVec(buf)).collect(Collectors.toSet()), buf.readUUID());
    }

    public static class Handler {
        public static void handle(PacketUpdateBoundCompass msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> ModPropertyGetters.addShelfPositions(msg.id, msg.positions));
            ctx.get().setPacketHandled(true);
        }
    }
}