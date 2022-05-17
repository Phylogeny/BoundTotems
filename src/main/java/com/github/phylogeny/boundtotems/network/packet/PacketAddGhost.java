package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.client.ClientEvents;
import com.github.phylogeny.boundtotems.init.SoundsMod;
import com.github.phylogeny.boundtotems.util.PacketBufferUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class PacketAddGhost {
    private final int entityId;
    private final float velocity;
    private final Vec3 targetPos;
    private final Integer targetEntityId;

    public PacketAddGhost(Entity entity, float velocity, @Nullable Vec3 targetPos, @Nullable Entity targetEntity) {
        this(entity.getId(), velocity, targetPos, targetEntity == null ? null : targetEntity.getId());
    }

    public PacketAddGhost(int entityId, float velocity, @Nullable Vec3 targetPos, @Nullable Integer targetEntityId) {
        this.entityId = entityId;
        this.velocity = velocity;
        this.targetPos = targetPos;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(PacketAddGhost msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeFloat(msg.velocity);
        PacketBufferUtil.writeNullableObject(buf, msg.targetPos, o -> PacketBufferUtil.writeVec(buf, o));
        PacketBufferUtil.writeNullableObject(buf, msg.targetEntityId, buf::writeInt);
    }

    public static PacketAddGhost decode(FriendlyByteBuf buf) {
        return new PacketAddGhost(buf.readInt(), buf.readFloat(),
                PacketBufferUtil.readNullableObject(buf, PacketBufferUtil::readVec), PacketBufferUtil.readNullableObject(buf, FriendlyByteBuf::readInt));
    }

    public static class Handler {
        public static void handle(PacketAddGhost msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                Level world = ClientEvents.getWorld();
                Entity entity = world.getEntity(msg.entityId);
                if (entity == null)
                    return;

                ClientEvents.playSoundAtEntity(entity, SoundsMod.EXHALE.get(), 1.4F - world.random.nextFloat() * 0.8F);
                ClientEvents.addGhost(world, entity, msg.velocity, msg.targetPos,
                        msg.targetEntityId == null ? null : world.getEntity(msg.targetEntityId));
            });
            ctx.get().setPacketHandled(true);
        }
    }
}