package com.github.phylogeny.boundtotems.block;

import com.github.phylogeny.boundtotems.init.BlocksMod;
import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketTotemShelfCarveEffects;
import com.github.phylogeny.boundtotems.util.EntityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;

import javax.annotation.Nullable;

public class PositionsTotemShelf {
    private final BlockPos posUpper, posLower;
    private final boolean isReversed;
    private final Integer stageNext;
    private final Direction facingTotemShelf;

    public PositionsTotemShelf(BlockState state, BlockPos posUpper, BlockPos posLower, boolean isReversed, @Nullable Player player) {
        this.posUpper = posUpper;
        this.posLower = posLower;
        this.isReversed = isReversed;
        if (!(state.getBlock() instanceof BlockTotemShelf)) {
            stageNext = 0;
            if (player == null)
                facingTotemShelf = Direction.NORTH;
            else {
                BlockHitResult result = EntityUtil.rayTraceBlocks(player);
                facingTotemShelf = result.getType() == HitResult.Type.MISS ? null : result.getDirection().getAxis() == Axis.Y ? player.getDirection().getOpposite() : result.getDirection();
            }
        } else {
            int stage = state.getValue(BlockTotemShelf.STAGE);
            stageNext = stage == BlockTotemShelf.STAGE.getPossibleValues().size() - 1 ? null : stage + 1;
            facingTotemShelf = state.getValue(BlockTotemShelf.FACING);
        }
    }

    public void advanceStage(ServerLevel world) {
        if (stageNext == null)
            return;

        PacketNetwork.sendToAllAround(new PacketTotemShelfCarveEffects(stageNext, posLower, facingTotemShelf), world, posUpper);
        BlockState stateNew = BlocksMod.TOTEM_SHELF.get().defaultBlockState().setValue(BlockTotemShelf.STAGE, stageNext).setValue(BlockTotemShelf.FACING, facingTotemShelf);
        world.setBlockAndUpdate(posUpper, stateNew.setValue(BlockTotemShelf.HALF, DoubleBlockHalf.UPPER));
        world.setBlockAndUpdate(posLower, stateNew.setValue(BlockTotemShelf.HALF, DoubleBlockHalf.LOWER));
        if (stageNext >= 7 || stageNext % 2 != 0)
            return;

        AABB box = (stageNext == 0 ? Shapes.block() : BlocksMod.TOTEM_SHELF.get().SHAPES.get(stateNew.setValue(BlockTotemShelf.STAGE, stageNext - 1))).bounds();
        Direction facing = facingTotemShelf.getOpposite();
        if (facing.getAxis() == Axis.X)
            box = box.contract(facing.getAxisDirection() == AxisDirection.POSITIVE ? box.maxX - 0.125 : box.minX - 0.875, 0, 0);
        else
            box = box.contract(0, 0, facing.getAxisDirection() == AxisDirection.POSITIVE ? box.maxZ - 0.125 : box.minZ - 0.875);

        double x = posLower.getX() + box.minX + world.random.nextFloat() * (box.maxX - box.minX);
        double y = posLower.getY() + world.random.nextFloat() * 2;
        double z = posLower.getZ() + box.minZ + world.random.nextFloat() * (box.maxZ - box.minZ);
        ItemEntity ItemEntity = new ItemEntity(world, x, y, z, new ItemStack(ItemsMod.PLANK.get()));
        ItemEntity.setDefaultPickUpDelay();
        world.addFreshEntity(ItemEntity);
    }

    public BlockPos getPosOffset() {
        return isReversed ? posUpper : posLower;
    }

    @Nullable
    public Integer getNextStage() {
        return stageNext;
    }
}