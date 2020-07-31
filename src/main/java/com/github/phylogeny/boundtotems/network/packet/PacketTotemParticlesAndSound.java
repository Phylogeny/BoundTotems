package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.Config;
import com.github.phylogeny.boundtotems.client.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTotemParticlesAndSound
{
    private final boolean particles, sound;
    private final int entityId;

    public PacketTotemParticlesAndSound(Entity entity)
    {
        this(entity.getEntityId(), Config.SERVER.spawnParticles.get(), Config.SERVER.playSound.get());
    }

    public PacketTotemParticlesAndSound(int entityId, boolean particles, boolean sound)
    {
        this.entityId = entityId;
        this.particles = particles;
        this.sound = sound;
    }

    public static void encode(PacketTotemParticlesAndSound msg, PacketBuffer buf)
    {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.particles);
        buf.writeBoolean(msg.sound);
    }

    public static PacketTotemParticlesAndSound decode(PacketBuffer buf)
    {
        return new PacketTotemParticlesAndSound(buf.readInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static class Handler
    {
        public static void handle(PacketTotemParticlesAndSound msg, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() ->
            {
                Entity entity = Minecraft.getInstance().world != null ? Minecraft.getInstance().world.getEntityByID(msg.entityId) : null;
                if (entity == null)
                    return;

                if (msg.particles)
                    Minecraft.getInstance().particles.emitParticleAtEntity(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);

                if (msg.sound)
                    ClientEvents.playSoundAtEntity(entity, SoundEvents.ITEM_TOTEM_USE, 1.0F);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}