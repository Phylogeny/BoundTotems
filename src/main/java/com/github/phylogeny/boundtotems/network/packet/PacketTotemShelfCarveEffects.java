package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.client.ClientEvents;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTotemShelfCarveEffects {
    private final int stageNext;
    private final BlockPos pos;
    private final Direction facing;

    public PacketTotemShelfCarveEffects(int stageNext, BlockPos pos, Direction facing) {
        this.stageNext = stageNext;
        this.pos = pos;
        this.facing = facing;
    }

    public static void encode(PacketTotemShelfCarveEffects msg, PacketBuffer buf) {
        buf.writeInt(msg.stageNext);
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.facing.get3DDataValue());
    }

    public static PacketTotemShelfCarveEffects decode(PacketBuffer buf) {
        return new PacketTotemShelfCarveEffects(buf.readInt(), buf.readBlockPos(), Direction.from3DDataValue(buf.readInt()));
    }

    public static class Handler {
        public static void handle(PacketTotemShelfCarveEffects msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                if (ctx.get().getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
                    ClientEvents.addTotemShelfCarveEffects(msg.pos, msg.stageNext, msg.facing);
                    PacketNetwork.sendToServer(new PacketTotemShelfCarveEffects(msg.stageNext, msg.pos, msg.facing));
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}