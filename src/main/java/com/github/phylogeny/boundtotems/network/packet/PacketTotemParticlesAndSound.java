package com.github.phylogeny.boundtotems.network.packet;

import com.github.phylogeny.boundtotems.Config;
import com.github.phylogeny.boundtotems.client.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketTotemParticlesAndSound {
    private final boolean particles, sound;
    private final int entityId;

    public PacketTotemParticlesAndSound(Entity entity) {
        this(entity.getId(), Config.SERVER.spawnParticles.get(), Config.SERVER.playSound.get());
    }

    public PacketTotemParticlesAndSound(int entityId, boolean particles, boolean sound) {
        this.entityId = entityId;
        this.particles = particles;
        this.sound = sound;
    }

    public static void encode(PacketTotemParticlesAndSound msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.particles);
        buf.writeBoolean(msg.sound);
    }

    public static PacketTotemParticlesAndSound decode(FriendlyByteBuf buf) {
        return new PacketTotemParticlesAndSound(buf.readInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static class Handler {
        public static void handle(PacketTotemParticlesAndSound msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                Entity entity = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(msg.entityId) : null;
                if (entity == null)
                    return;

                if (msg.particles)
                    Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);

                if (msg.sound)
                    ClientEvents.playSoundAtEntity(entity, SoundEvents.TOTEM_USE, 1.0F);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}