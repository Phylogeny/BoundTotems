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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketAddOrRemoveKnife {
    private final BlockPos pos;
    private final ItemStack knifeStack;
    private final Vector3d knifePos, direction;

    public PacketAddOrRemoveKnife(BlockPos pos, Vector3d knifePos, PlayerEntity player) {
        this(pos, knifePos, ItemStack.EMPTY, getParticleMotion(player));
    }

    public PacketAddOrRemoveKnife(BlockPos pos, Vector3d knifePos, ItemStack knifeStack, Vector3d direction) {
        this.pos = pos;
        this.knifePos = knifePos;
        this.knifeStack = knifeStack;
        this.direction = direction;
    }

    public static void encode(PacketAddOrRemoveKnife msg, PacketBuffer buf) {
        buf.writeBlockPos(msg.pos);
        PacketBufferUtil.writeVec(buf, msg.knifePos);
        buf.writeItem(msg.knifeStack);
        PacketBufferUtil.writeVec(buf, msg.direction);
    }

    private static Vector3d getParticleMotion(PlayerEntity player) {
        float pitch = player.xRot;
        float yaw = player.yRot;
        float radToDeg = (float) Math.PI / 180F;
        double motionX = -MathHelper.sin(yaw * radToDeg) * MathHelper.cos(pitch * radToDeg);
        double motionY = -MathHelper.sin(pitch * radToDeg);
        double motionZ = MathHelper.cos(yaw * radToDeg) * MathHelper.cos(pitch * radToDeg);
        float f = MathHelper.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        return new Vector3d(motionX / f, motionY / f, motionZ / f);
    }

    public static PacketAddOrRemoveKnife decode(PacketBuffer buf) {
        return new PacketAddOrRemoveKnife(buf.readBlockPos(), PacketBufferUtil.readVec(buf), buf.readItem(), PacketBufferUtil.readVec(buf));
    }

    public static class Handler {
        public static void handle(PacketAddOrRemoveKnife msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                PlayerEntity player = ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER ? ctx.get().getSender() : ClientEvents.getPlayer();
                if (player == null)
                    return;

                TileEntity te = player.level.getBlockEntity(msg.pos);
                if (te instanceof TileEntityTotemShelf) {
                    BlockState state = player.level.getBlockState(msg.pos);
                    if (state.getBlock() instanceof BlockTotemShelf) {
                        if (!msg.knifeStack.isEmpty())
                            ((TileEntityTotemShelf) te).addKnife(msg.knifePos, msg.direction, msg.knifeStack);
                        else {
                            ClientEvents.addKnifeRemovalEffects(msg.knifePos, msg.direction, state);
                            ((TileEntityTotemShelf) te).removeKnife();
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}