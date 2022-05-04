package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.client.ClientEvents;
import com.github.phylogeny.boundtotems.util.PacketBufferUtil;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class PacketShelfSmokeParticles
{
    @Nullable
    private final AxisAlignedBB box;
    private final BlockPos pos;

    public PacketShelfSmokeParticles(World world, BlockPos pos, BlockState state, boolean charShelf)
    {
        box = charShelf ? null : getOffsetShape(world, pos, state).bounds().inflate(0.1);
        this.pos = pos;
    }

    private PacketShelfSmokeParticles(@Nullable AxisAlignedBB box, BlockPos pos)
    {
        this.box = box;
        this.pos = pos;
    }

    private static VoxelShape getOffsetShape(World world, BlockPos pos, BlockState state) {
        return state.getCollisionShape(world, pos).move(pos.getX(), pos.getY(), pos.getZ());
    }

    public static void encode(PacketShelfSmokeParticles msg, PacketBuffer buf)
    {
        PacketBufferUtil.writeNullableObject(buf, msg.box, o -> PacketBufferUtil.writeAABB(buf, o));
        buf.writeBlockPos(msg.pos);
    }

    public static PacketShelfSmokeParticles decode(PacketBuffer buf)
    {
        return new PacketShelfSmokeParticles(PacketBufferUtil.readNullableObject(buf, PacketBufferUtil::readAABB), buf.readBlockPos());
    }

    public static class Handler
    {
        public static void handle(PacketShelfSmokeParticles msg, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                World world = ClientEvents.getWorld();
                if (msg.box != null)
                    BlockTotemShelf.spawnShelfSmokeParticles(world, msg.box);
                else
                    BlockTotemShelf.spawnShelfSmokeParticles(world, getOffsetShape(world, msg.pos, world.getBlockState(msg.pos)));
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
