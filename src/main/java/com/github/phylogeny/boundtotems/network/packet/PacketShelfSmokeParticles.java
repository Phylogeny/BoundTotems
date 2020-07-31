package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.client.ClientEvents;
import com.github.phylogeny.boundtotems.util.PacketBufferUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketShelfSmokeParticles
{
    private final AxisAlignedBB box;
    private final boolean addFlames;

    public PacketShelfSmokeParticles(AxisAlignedBB box, boolean addFlames)
    {
        this.box = box;
        this.addFlames = addFlames;
    }

    public static void encode(PacketShelfSmokeParticles msg, PacketBuffer buf)
    {
        PacketBufferUtil.writeAABB(buf, msg.box);
        buf.writeBoolean(msg.addFlames);
    }

    public static PacketShelfSmokeParticles decode(PacketBuffer buf)
    {
        return new PacketShelfSmokeParticles(PacketBufferUtil.readAABB(buf), buf.readBoolean());
    }

    public static class Handler
    {
        public static void handle(PacketShelfSmokeParticles msg, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> BlockTotemShelf.spawnShelfSmokeParticles(ClientEvents.getWorld(), msg.box, msg.addFlames));
            ctx.get().setPacketHandled(true);
        }
    }
}
