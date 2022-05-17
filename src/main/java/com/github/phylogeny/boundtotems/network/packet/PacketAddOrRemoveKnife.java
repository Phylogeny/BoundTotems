package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.client.ClientEvents;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelf;
import com.github.phylogeny.boundtotems.util.PacketBufferUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
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
    private final Vec3 knifePos, direction;

    public PacketAddOrRemoveKnife(BlockPos pos, Vec3 knifePos, Player player) {
        this(pos, knifePos, ItemStack.EMPTY, getParticleMotion(player));
    }

    public PacketAddOrRemoveKnife(BlockPos pos, Vec3 knifePos, ItemStack knifeStack, Vec3 direction) {
        this.pos = pos;
        this.knifePos = knifePos;
        this.knifeStack = knifeStack;
        this.direction = direction;
    }

    public static void encode(PacketAddOrRemoveKnife msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        PacketBufferUtil.writeVec(buf, msg.knifePos);
        buf.writeItem(msg.knifeStack);
        PacketBufferUtil.writeVec(buf, msg.direction);
    }

    private static Vec3 getParticleMotion(Player player) {
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        float radToDeg = (float) Math.PI / 180F;
        float motionX = -Mth.sin(yaw * radToDeg) * Mth.cos(pitch * radToDeg);
        float motionY = -Mth.sin(pitch * radToDeg);
        float motionZ = Mth.cos(yaw * radToDeg) * Mth.cos(pitch * radToDeg);
        float f = Mth.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        return new Vec3(motionX / f, motionY / f, motionZ / f);
    }

    public static PacketAddOrRemoveKnife decode(FriendlyByteBuf buf) {
        return new PacketAddOrRemoveKnife(buf.readBlockPos(), PacketBufferUtil.readVec(buf), buf.readItem(), PacketBufferUtil.readVec(buf));
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
                        if (msg.knifeStack.isEmpty())
                            shelf.addKnife(msg.knifePos, msg.direction, msg.knifeStack);
                        else {
                            ClientEvents.addKnifeRemovalEffects(msg.knifePos, msg.direction, state);
                            shelf.removeKnife();
                        }
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}