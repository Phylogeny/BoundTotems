package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.client.ClientEvents;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelf;
import com.github.phylogeny.boundtotems.util.PacketBufferUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketAddOrRemoveKnife
{
    private final BlockPos pos;
    private final ItemStack knifeStack;
    private final Vec3d knifePos, knifeDirection;

    public PacketAddOrRemoveKnife(BlockPos pos, Vec3d knifePos, ItemStack knifeStack)
    {
        this(pos, knifePos, knifeStack, null);
    }

    public PacketAddOrRemoveKnife(BlockPos pos, Vec3d knifePos, ItemStack knifeStack, Vec3d knifeDirection)
    {
        this.pos = pos;
        this.knifePos = knifePos;
        this.knifeStack = knifeStack;
        this.knifeDirection = knifeDirection;
    }

    public static void encode(PacketAddOrRemoveKnife msg, PacketBuffer buf)
    {
        buf.writeBlockPos(msg.pos);
        PacketBufferUtil.writeVec(buf, msg.knifePos);
        buf.writeItemStack(msg.knifeStack);
        PacketBufferUtil.writeNullableObject(buf, msg.knifeDirection, () -> PacketBufferUtil.writeVec(buf, msg.knifeDirection));
    }

    public static PacketAddOrRemoveKnife decode(PacketBuffer buf)
    {
        return new PacketAddOrRemoveKnife(buf.readBlockPos(), PacketBufferUtil.readVec(buf), buf.readItemStack(),
                PacketBufferUtil.readNullableObject(buf, () -> PacketBufferUtil.readVec(buf)));
    }

    public static class Handler
    {
        public static void handle(PacketAddOrRemoveKnife msg, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() ->
            {
                PlayerEntity player = ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER ? ctx.get().getSender() : ClientEvents.getPlayer();
                if (player == null)
                    return;

                TileEntity te = player.world.getTileEntity(msg.pos);
                if (te instanceof TileEntityTotemShelf)
                {
                    BlockState state = player.world.getBlockState(msg.pos);
                    if (state.getBlock() instanceof BlockTotemShelf)
                    {
                        if (msg.knifeDirection != null)
                            ((TileEntityTotemShelf) te).addKnife(msg.knifePos, msg.knifeDirection, msg.knifeStack);
                        else
                        {
                            ClientEvents.addKnifeRemovalEffects(msg.knifePos, state);
                            ((TileEntityTotemShelf) te).removeKnife();
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
