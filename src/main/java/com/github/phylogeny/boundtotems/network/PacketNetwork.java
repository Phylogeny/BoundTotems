package com.github.phylogeny.boundtotems.network;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.network.packet.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketNetwork
{
    private static int packetId = 0;
    private static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(BoundTotems.getResourceLoc("channel_main"))
            .clientAcceptedVersions(version -> true)
            .serverAcceptedVersions(version -> true)
            .networkProtocolVersion(() -> "1.0")
            .simpleChannel();

    public static void registerPackets()
    {
        // Client side
        registerMessage(PacketTotemParticlesAndSound.class, PacketTotemParticlesAndSound::encode, PacketTotemParticlesAndSound::decode, PacketTotemParticlesAndSound.Handler::handle);
        registerMessage(PacketTotemAnimation.class, PacketTotemAnimation::encode, PacketTotemAnimation::decode, PacketTotemAnimation.Handler::handle);
        registerMessage(PacketAddGhost.class, PacketAddGhost::encode, PacketAddGhost::decode, PacketAddGhost.Handler::handle);
        registerMessage(PacketTotemShelfCarveEffects.class, PacketTotemShelfCarveEffects::encode, PacketTotemShelfCarveEffects::decode, PacketTotemShelfCarveEffects.Handler::handle);
        registerMessage(PacketShelfSmokeParticles.class, PacketShelfSmokeParticles::encode, PacketShelfSmokeParticles::decode, PacketShelfSmokeParticles.Handler::handle);
        registerMessage(PacketSyncShelfCap.class, PacketSyncShelfCap::encode, PacketSyncShelfCap::decode, PacketSyncShelfCap.Handler::handle);

        // Both sides
        registerMessage(PacketAddOrRemoveKnife.class, PacketAddOrRemoveKnife::encode, PacketAddOrRemoveKnife::decode, PacketAddOrRemoveKnife.Handler::handle);
    }

    public static <MSG> void sendToAllTrackingAndSelf(MSG msg, Entity entity)
    {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), msg);
    }

    public static <MSG> void sendToAllTracking(MSG msg, Entity entity)
    {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), msg);
    }

    public static <MSG> void sendTo(MSG msg, ServerPlayerEntity player)
    {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static <MSG> void sendToAll(MSG msg)
    {
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }

    public static <MSG> void sendToAllAround(MSG msg, World world, BlockPos pos)
    {
        sendToAllAround(msg, world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static <MSG> void sendToAllAround(MSG msg, World world, Vec3d vec)
    {
        sendToAllAround(msg, world, vec.x, vec.y, vec.z);
    }

    public static <MSG> void sendToAllAround(MSG msg, World world, double x, double y, double z)
    {
        INSTANCE.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(x, y, z, 128, world.dimension.getType())), msg);
    }

    public static <MSG> void sendToDimension(MSG msg, DimensionType dimensionType)
    {
        INSTANCE.send(PacketDistributor.DIMENSION.with(() -> dimensionType), msg);
    }

    public static <MSG> void sendToServer(MSG msg)
    {
        INSTANCE.send(PacketDistributor.SERVER.noArg(), msg);
    }

    private static <MSG> void registerMessage(Class<MSG> messageType, BiConsumer<MSG, PacketBuffer> encoder,
            Function<PacketBuffer, MSG> decoder, BiConsumer<MSG, Supplier<NetworkEvent.Context>> messageConsumer)
    {
        INSTANCE.registerMessage(packetId++, messageType, encoder, decoder, messageConsumer);
    }
}