package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.client.ClientEvents;
import com.github.phylogeny.boundtotems.init.SoundsMod;
import com.github.phylogeny.boundtotems.util.PacketBufferUtil;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class PacketAddGhost
{
    private final int entityId;
    private final float velocity;
    private final Vector3d targetPos;
    private final Integer targetEntityId;

    public PacketAddGhost(Entity entity, float velocity, @Nullable Vector3d targetPos, @Nullable Entity targetEntity)
    {
        this(entity.getEntityId(), velocity, targetPos, targetEntity == null ? null : targetEntity.getEntityId());
    }

    public PacketAddGhost(int entityId, float velocity, @Nullable Vector3d targetPos, @Nullable Integer targetEntityId)
    {
        this.entityId = entityId;
        this.velocity = velocity;
        this.targetPos = targetPos;
        this.targetEntityId = targetEntityId;
    }

    public static void encode(PacketAddGhost msg, PacketBuffer buf)
    {
        buf.writeInt(msg.entityId);
        buf.writeFloat(msg.velocity);
        PacketBufferUtil.writeNullableObject(buf, msg.targetPos, o -> PacketBufferUtil.writeVec(buf, o));
        PacketBufferUtil.writeNullableObject(buf, msg.targetEntityId, buf::writeInt);
    }

    public static PacketAddGhost decode(PacketBuffer buf)
    {
        return new PacketAddGhost(buf.readInt(), buf.readFloat(),
                PacketBufferUtil.readNullableObject(buf, PacketBufferUtil::readVec), PacketBufferUtil.readNullableObject(buf, PacketBuffer::readInt));
    }

    public static class Handler
    {
        public static void handle(PacketAddGhost msg, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() ->
            {
                World world = ClientEvents.getWorld();
                Entity entity = world.getEntityByID(msg.entityId);
                if (entity == null)
                    return;

                ClientEvents.playSoundAtEntity(entity, SoundsMod.EXHALE.get(),1.4F - world.rand.nextFloat() * 0.8F);
                ClientEvents.addGhost(world, entity, msg.velocity, msg.targetPos,
                        msg.targetEntityId == null ? null : world.getEntityByID(msg.targetEntityId));
            });
            ctx.get().setPacketHandled(true);
        }
    }
}