package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.init.ItemsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTotemAnimation {
    private final boolean teleporting;

    public PacketTotemAnimation(boolean teleporting) {
        this.teleporting = teleporting;
    }

    public static void encode(PacketTotemAnimation msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.teleporting);
    }

    public static PacketTotemAnimation decode(FriendlyByteBuf buf) {
        return new PacketTotemAnimation(buf.readBoolean());
    }

    public static class Handler {
        public static void handle(PacketTotemAnimation msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> Minecraft.getInstance().gameRenderer
                    .displayItemActivation(new ItemStack(msg.teleporting ? ItemsMod.BOUND_TOTEM_TELEPORTING.get() : ItemsMod.BOUND_TOTEM.get())));
            ctx.get().setPacketHandled(true);
        }
    }
}