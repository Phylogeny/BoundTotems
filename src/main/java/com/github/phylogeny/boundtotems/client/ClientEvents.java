package com.github.phylogeny.boundtotems.client;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.init.BlocksMod;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import javax.annotation.Nullable;
import java.util.*;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents
{
    private static final Map<ResourceLocation, List<Ghost>> GHOSTS = new HashMap<>();

    public static void addGhost(World world, Entity entity, float velocity, @Nullable Vector3d targetPos, @Nullable Entity targetEntity)
    {
        ResourceLocation dimension = NBTUtil.getDimensionKey(world);
        List<Ghost> ghosts = GHOSTS.get(dimension);
        if (ghosts == null)
            ghosts = new ArrayList<>();

        ghosts.add(new Ghost(entity, velocity, targetPos, targetEntity));
        GHOSTS.put(dimension, ghosts);
    }

    @SubscribeEvent
    public static void updateGhosts(ClientTickEvent event)
    {
        if (event.phase != Phase.START || getWorld() == null || Minecraft.getInstance().isPaused())
            return;

        ResourceLocation dimension = NBTUtil.getDimensionKey(getWorld());
        List<Ghost> ghosts = GHOSTS.get(dimension);
        if (ghosts == null)
            return;

        ghosts.removeIf(Ghost::update);
        if (ghosts.isEmpty())
            GHOSTS.remove(dimension);
    }

    @SubscribeEvent
    public static void renderGhosts(RenderWorldLastEvent event)
    {
        List<Ghost> ghosts = GHOSTS.get(NBTUtil.getDimensionKey(getWorld()));
        if (ghosts != null)
            ghosts.forEach(ghost -> ghost.render(event));
    }

    public static void playSoundAtEntity(Entity entity, SoundEvent sound, float pitch)
    {
        getWorld().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), sound, entity.getSoundSource(), 1.0F, pitch, false);
    }

    public static World getWorld()
    {
        return Minecraft.getInstance().level;
    }

    public static PlayerEntity getPlayer()
    {
        return Minecraft.getInstance().player;
    }

    public static void addKnifeRemovalEffects(Vector3d knifePos, BlockState state)
    {
        BlockPos pos = new BlockPos(knifePos);
        double x = knifePos.x - pos.getX();
        double y = knifePos.y - pos.getY();
        double z = knifePos.z - pos.getZ();
        float pitch = getPlayer().xRot;
        float yaw = getPlayer().yRot;
        float radToDeg = (float) Math.PI / 180F;
        double motionX = -MathHelper.sin(yaw * radToDeg) * MathHelper.cos(pitch * radToDeg);
        double motionY = -MathHelper.sin(pitch * radToDeg);
        double motionZ = MathHelper.cos(yaw * radToDeg) * MathHelper.cos(pitch * radToDeg);
        float f = MathHelper.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= f;
        motionY /= f;
        motionZ /= f;
        double inaccuracy = 0.75;
        double velocity = 0.05;
        Random rand = getWorld().random;
        for (int i = 0; i < 4; i++)
            Minecraft.getInstance().particleEngine.add(new DiggingParticleExtended(pos,
                    x + rand.nextDouble() * 0.1 - 0.05,
                    y + rand.nextDouble() * 0.1 - 0.05,
                    z + rand.nextDouble() * 0.1 - 0.05,
                    -(motionX + rand.nextGaussian() * inaccuracy) * velocity,
                    -(motionY + rand.nextGaussian() * inaccuracy) * velocity,
                    -(motionZ + rand.nextGaussian() * inaccuracy) * velocity,
                    state, true).scale(0.5F));
    }

    public static void addTotemShelfCarveEffects(BlockPos pos, int stageNext, Direction facing)
    {
        BlockState stateNew = BlocksMod.TOTEM_SHELF.get().defaultBlockState().setValue(BlockTotemShelf.STAGE, stageNext).setValue(BlockTotemShelf.FACING, facing);
        SoundType sound = BlocksMod.TOTEM_SHELF.get().getSoundType(stateNew);
        boolean placing = stageNext >= 7;
        ((ClientWorld) getWorld()).playLocalSound(pos, placing ? sound.getPlaceSound() : SoundEvents.AXE_STRIP, SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F, false);
        if (!placing)
        {
            VoxelShape shapeOld = (stageNext == 0 ? VoxelShapes.block() : BlocksMod.TOTEM_SHELF.get().SHAPES.get(stateNew.setValue(BlockTotemShelf.STAGE, stageNext - 1)));
            VoxelShape shapeRemoved = VoxelShapes.joinUnoptimized(BlocksMod.TOTEM_SHELF.get().SHAPES.get(stateNew), shapeOld, IBooleanFunction.NOT_SAME);
            addBlockDestroyEffects(pos, stateNew, shapeRemoved);
        }
    }

    /**
     * Mimics {@link net.minecraft.client.particle.ParticleManager#destroy destroy}
     * in ParticleManager to add destruction particles for an arbitrary VoxelShape.
     */
    private static void addBlockDestroyEffects(BlockPos pos, BlockState state, VoxelShape shape)
    {
        if (state.isAir(getWorld(), pos))
            return;

        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
        {
            double dx = Math.min(1.0, maxX - minX);
            double dy = Math.min(2.0, maxY - minY);
            double dz = Math.min(1.0, maxZ - minZ);
            int countX = Math.max(2, MathHelper.ceil(dx / 0.25));
            int countY = Math.max(2, MathHelper.ceil(dy / 0.25));
            int countZ = Math.max(2, MathHelper.ceil(dz / 0.25));
            for (int l = 0; l < countX; ++l)
            {
                for (int i1 = 0; i1 < countY; ++i1)
                {
                    for (int j1 = 0; j1 < countZ; ++j1)
                    {
                        double x = (l + 0.5) / countX;
                        double y = (i1 + 0.5) / countY;
                        double z = (j1 + 0.5) / countZ;
                        double xCoord = x * dx + minX;
                        double yCoord = y * dy + minY;
                        double zCoord = z * dz + minZ;
                        Minecraft.getInstance().particleEngine.add(new DiggingParticleExtended(pos, xCoord, yCoord, zCoord, 0, 0, 0, state, false));
                    }
                }
            }
        });
    }

    private static class DiggingParticleExtended extends DiggingParticle
    {
        public DiggingParticleExtended(BlockPos pos, double xCoord, double yCoord, double zCoord,
                double xSpeed, double ySpeed, double zSpeed, BlockState state, boolean exactVelocity)
        {
            super(Minecraft.getInstance().level, pos.getX() + xCoord, pos.getY() + yCoord, pos.getZ() + zCoord, xSpeed, ySpeed, zSpeed, state);
            init(pos);
            if (exactVelocity)
            {
                xd = xSpeed;
                yd = ySpeed;
                zd = zSpeed;
            }
        }
    }
}