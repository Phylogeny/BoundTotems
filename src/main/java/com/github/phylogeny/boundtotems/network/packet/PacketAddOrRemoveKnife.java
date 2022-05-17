package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.client.ClientEvents;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelf;
import com.github.phylogeny.boundtotems.util.PacketBufferUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketAddOrRemoveKnife {
    private final BlockPos pos;
    private final ItemStack knifeStack;
    private final Vec3 knifePos, knifeDirection;

    public PacketAddOrRemoveKnife(BlockPos pos, Vec3 knifePos, ItemStack knifeStack) {
        this(pos, knifePos, knifeStack, null);
    }

    public PacketAddOrRemoveKnife(BlockPos pos, Vec3 knifePos, ItemStack knifeStack, Vec3 knifeDirection) {
        this.pos = pos;
        this.knifePos = knifePos;
        this.knifeStack = knifeStack;
        this.knifeDirection = knifeDirection;
    }

    public static void encode(PacketAddOrRemoveKnife msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        PacketBufferUtil.writeVec(buf, msg.knifePos);
        buf.writeItem(msg.knifeStack);
        PacketBufferUtil.writeNullableObject(buf, msg.knifeDirection, o -> PacketBufferUtil.writeVec(buf, o));
    }

    public static PacketAddOrRemoveKnife decode(FriendlyByteBuf buf) {
        return new PacketAddOrRemoveKnife(buf.readBlockPos(), PacketBufferUtil.readVec(buf), buf.readItem(),
                PacketBufferUtil.readNullableObject(buf, PacketBufferUtil::readVec));
    }

    public static class Handler {
        public static void handle(PacketAddOrRemoveKnife msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                Player player = ctx.get().getDirection() == NetworkDirection.PLAY_TO_SERVER ? ctx.get().getSender() : ClientEvents.getPlayer();
                if (player == null)
                    return;

                BlockEntity te = player.level.getBlockEntity(msg.pos);
                if (te instanceof BlockEntityTotemShelf shelf) {
                    BlockState state = player.level.getBlockState(msg.pos);
                    if (state.getBlock() instanceof BlockTotemShelf) {
                        if (msg.knifeDirection != null)
                            shelf.addKnife(msg.knifePos, msg.knifeDirection, msg.knifeStack);
                        else {
                            ClientEvents.addKnifeRemovalEffects(msg.knifePos, state);
                            shelf.removeKnife();
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}