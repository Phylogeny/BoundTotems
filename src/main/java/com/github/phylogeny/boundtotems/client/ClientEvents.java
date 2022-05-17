package com.github.phylogeny.boundtotems.client;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.init.BlocksMod;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import javax.annotation.Nullable;
import java.util.*;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEvents {
    private static final Map<ResourceLocation, List<Ghost>> GHOSTS = new HashMap<>();

    public static void addGhost(Level world, Entity entity, float velocity, @Nullable Vec3 targetPos, @Nullable Entity targetEntity) {
        ResourceLocation dimension = NBTUtil.getDimensionKey(world);
        List<Ghost> ghosts = GHOSTS.get(dimension);
        if (ghosts == null)
            ghosts = new ArrayList<>();

        ghosts.add(new Ghost(entity, velocity, targetPos, targetEntity));
        GHOSTS.put(dimension, ghosts);
    }

    @SubscribeEvent
    public static void updateGhosts(ClientTickEvent event) {
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
    public static void renderGhosts(RenderWorldLastEvent event) {
        List<Ghost> ghosts = GHOSTS.get(NBTUtil.getDimensionKey(getWorld()));
        if (ghosts != null)
            ghosts.forEach(ghost -> ghost.render(event.getMatrixStack(), event.getPartialTicks()));
    }

    public static void playSoundAtEntity(Entity entity, SoundEvent sound, float pitch) {
        getWorld().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), sound, entity.getSoundSource(), 1.0F, pitch, false);
    }

    public static Level getWorld() {
        return Minecraft.getInstance().level;
    }

    public static Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public static void addKnifeRemovalEffects(Vec3 knifePos, BlockState state) {
        BlockPos pos = new BlockPos(knifePos);
        double x = knifePos.x - pos.getX();
        double y = knifePos.y - pos.getY();
        double z = knifePos.z - pos.getZ();
        float pitch = getPlayer().getXRot();
        float yaw = getPlayer().getYRot();
        float radToDeg = (float) Math.PI / 180F;
        double motionX = -Mth.sin(yaw * radToDeg) * Mth.cos(pitch * radToDeg);
        double motionY = -Mth.sin(pitch * radToDeg);
        double motionZ = Mth.cos(yaw * radToDeg) * Mth.cos(pitch * radToDeg);
        float f = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= f;
        motionY /= f;
        motionZ /= f;
        double inaccuracy = 0.75;
        double velocity = 0.05;
        Random rand = getWorld().random;
        for (int i = 0; i < 4; i++)
            Minecraft.getInstance().particleEngine.add(new TerrainParticleExtended(pos,
                    x + rand.nextDouble() * 0.1 - 0.05,
                    y + rand.nextDouble() * 0.1 - 0.05,
                    z + rand.nextDouble() * 0.1 - 0.05,
                    -(motionX + rand.nextGaussian() * inaccuracy) * velocity,
                    -(motionY + rand.nextGaussian() * inaccuracy) * velocity,
                    -(motionZ + rand.nextGaussian() * inaccuracy) * velocity,
                    state, true).scale(0.5F));
    }

    public static void addTotemShelfCarveEffects(BlockPos pos, int stageNext, Direction facing) {
        BlockState stateNew = BlocksMod.TOTEM_SHELF.get().defaultBlockState().setValue(BlockTotemShelf.STAGE, stageNext).setValue(BlockTotemShelf.FACING, facing);
        SoundType sound = BlocksMod.TOTEM_SHELF.get().getSoundType(stateNew);
        boolean placing = stageNext >= 7;
        ((ClientLevel) getWorld()).playLocalSound(pos, placing ? sound.getPlaceSound() : SoundEvents.AXE_STRIP, SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F, false);
        if (!placing) {
            VoxelShape shapeOld = (stageNext == 0 ? Shapes.block() : BlocksMod.TOTEM_SHELF.get().SHAPES.get(stateNew.setValue(BlockTotemShelf.STAGE, stageNext - 1)));
            VoxelShape shapeRemoved = Shapes.joinUnoptimized(BlocksMod.TOTEM_SHELF.get().SHAPES.get(stateNew), shapeOld, BooleanOp.NOT_SAME);
            addBlockDestroyEffects(pos, stateNew, shapeRemoved);
        }
    }

    /**
     * Mimics {@link net.minecraft.client.particle.ParticleEngine#destroy destroy}
     * in ParticleEngine to add destruction particles for an arbitrary VoxelShape.
     */
    private static void addBlockDestroyEffects(BlockPos pos, BlockState state, VoxelShape shape) {
        if (state.isAir())
            return;

        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double dx = Math.min(1.0, maxX - minX);
            double dy = Math.min(2.0, maxY - minY);
            double dz = Math.min(1.0, maxZ - minZ);
            int countX = Math.max(2, Mth.ceil(dx / 0.25));
            int countY = Math.max(2, Mth.ceil(dy / 0.25));
            int countZ = Math.max(2, Mth.ceil(dz / 0.25));
            for (int l = 0; l < countX; ++l) {
                for (int i1 = 0; i1 < countY; ++i1) {
                    for (int j1 = 0; j1 < countZ; ++j1) {
                        double x = (l + 0.5) / countX;
                        double y = (i1 + 0.5) / countY;
                        double z = (j1 + 0.5) / countZ;
                        double xCoord = x * dx + minX;
                        double yCoord = y * dy + minY;
                        double zCoord = z * dz + minZ;
                        Minecraft.getInstance().particleEngine.add(new TerrainParticleExtended(pos, xCoord, yCoord, zCoord, 0, 0, 0, state, false));
                    }
                }
            }
        });
    }

    private static class TerrainParticleExtended extends TerrainParticle {
        public TerrainParticleExtended(BlockPos pos, double xCoord, double yCoord, double zCoord,
                                       double xSpeed, double ySpeed, double zSpeed, BlockState state, boolean exactVelocity) {
            super(Minecraft.getInstance().level, pos.getX() + xCoord, pos.getY() + yCoord, pos.getZ() + zCoord, xSpeed, ySpeed, zSpeed, state);
            updateSprite(state, pos);
            if (exactVelocity) {
                xd = xSpeed;
                yd = ySpeed;
                zd = zSpeed;
            }
        }
    }
}