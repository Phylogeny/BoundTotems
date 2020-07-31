package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.capability.ShelfPositionsProvider;
import com.github.phylogeny.boundtotems.client.ClientEvents;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncShelfCap
{
    private final CompoundNBT nbt;

    public PacketSyncShelfCap(ServerPlayerEntity player)
    {
        this((CompoundNBT) ShelfPositionsProvider.serializeNBT(CapabilityUtil.getShelfPositions(player)));
    }

    public PacketSyncShelfCap(CompoundNBT nbt)
    {
        this.nbt = nbt;
    }

    public static void encode(PacketSyncShelfCap msg, PacketBuffer buf)
    {
        buf.writeCompoundTag(msg.nbt);
    }

    public static PacketSyncShelfCap decode(PacketBuffer buf)
    {
        return new PacketSyncShelfCap(buf.readCompoundTag());
    }

    public static class Handler
    {
        public static void handle(PacketSyncShelfCap msg, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() ->
            {
                PlayerEntity player = ClientEvents.getPlayer();
                if (player != null)
                    ShelfPositionsProvider.deserializeNBT(msg.nbt, CapabilityUtil.getShelfPositions(player));
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
