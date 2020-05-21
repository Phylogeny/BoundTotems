package com.github.phylogeny.boundtotems.block;

import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.init.SoundsMod;
import com.github.phylogeny.boundtotems.item.ItemPlank;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketShelfSmokeParticles;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelf;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelfBinding;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.EntityUtil;
import com.github.phylogeny.boundtotems.util.VoxelShapeUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockTotemShelf extends BlockWaterLoggable
{
    public static final String NAME = "totem_shelf";
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<BindingState> BINDING_STATE = EnumProperty.create("binding_state", BindingState.class);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 10);
    public static final VoxelShape SHAPE_KNIFE =  VoxelShapes.create(-0.15, -0.15, -0.15, 0.15, 0.15, 0.15);
    public static final EnumMap<Direction, EnumMap<DoubleBlockHalf, VoxelShape[]>> SHAPES_TOTEMS;
    public final ImmutableMap<BlockState, VoxelShape> SHAPES, SHAPES_RAYTRACE;
    private boolean preventPostPlacementCheck;

    static
    {
        VoxelShape shape = Block.makeCuboidShape(0, 0, 0, 5.5, 6, 3).withOffset(0.125 + 0.03125, 0.375, 0.0625);
        SHAPES_TOTEMS = createEnumMap(Direction.class, dir -> createEnumMap(DoubleBlockHalf.class, half ->
                IntStream.range(0, TileEntityTotemShelf.SIZE_INVENTORY).mapToObj(i ->
                        dir.getAxis() == Axis.Y ? null : VoxelShapeUtil.rotateShape(shape.withOffset((i % 2) * (0.375 - 0.03125), -(i / 2) * 0.625 + half.ordinal(), 0),
                                dir.rotateYCCW())).toArray(VoxelShape[]::new)));
    }

    private static <K extends Enum<K>, V> EnumMap<K, V> createEnumMap(Class<K> classEnum, Function<? super K, ? extends V> valueMapper)
    {
        return Arrays.stream(classEnum.getEnumConstants()).collect(Collectors.toMap(enumValue -> enumValue, valueMapper,
                (v1, v2) -> v2, () -> new EnumMap<>(classEnum)));
    }

    public BlockTotemShelf(Properties properties)
    {
        super(properties);
        setDefaultState(getBaseState()
                .with(FACING, Direction.NORTH)
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(BINDING_STATE, BindingState.NOT_BOUND)
                .with(STAGE, 10));
        SHAPES = VoxelShapeUtil.generateShapes(getStateContainer().getValidStates(), FACING, state ->
        {
            assert state != null;
            int stage = state.get(STAGE);
            boolean isUpper = state.get(HALF) == DoubleBlockHalf.UPPER;
            if (stage < 5)
                return Collections.singletonList(Block.makeCuboidShape(0, -16, 0, 16, 16, 14 - 2 * stage).withOffset(0, isUpper ? 0 : 1, 0));

            List<VoxelShape> shapes = Lists.newArrayList(Block.makeCuboidShape(0, -16, 0, 16, 14, stage == 5 ? 4 : 1),
                    Block.makeCuboidShape(0, -16, 1, 2, 14, 5), Block.makeCuboidShape(14, -16, 1, 16, 14, 5));
            if (stage > 6)
                shapes.add(Block.makeCuboidShape(0, -16, 0, 16, -14, 5));

            if (stage > 7)
                shapes.add(Block.makeCuboidShape(0, 14, 0, 16, 16, 5));

            if (stage > 8)
                shapes.add(Block.makeCuboidShape(0, -6, 0, 16, -4, 5));

            if (stage == 10)
                shapes.add(Block.makeCuboidShape(0, 4, 0, 16, 6, 5));

            if (!isUpper)
                shapes = shapes.stream().map(shape -> shape.withOffset(0, 1, 0)).collect(Collectors.toList());

            return shapes;
        });
        SHAPES_RAYTRACE = VoxelShapeUtil.getTransformedShapes(SHAPES, shape -> VoxelShapes.create(shape.getBoundingBox()));
    }

    public enum BindingState implements IStringSerializable
    {
        NOT_BOUND,
        HEATING,
        COOLING,
        BOUND;

        public boolean isTransitioning() { return this == HEATING || this == COOLING; }

        public boolean hasNext()
        {
            return ordinal() < values().length - 1;
        }

        @Nullable
        public BindingState getNext()
        {
            return hasNext() ? values()[ordinal() + 1] : null;
        }

        @Override
        public String getName()
        {
            return name().toLowerCase();
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }

    @Nullable
    public static PositionsTotemShelf getTotemShelfPositions(BlockState state, IWorld world, BlockPos pos)
    {
        return getTotemShelfPositions(state, world, pos, null);
    }

    @Nullable
    public static PositionsTotemShelf getTotemShelfPositions(BlockState state, IWorld world, BlockPos pos, @Nullable PlayerEntity player)
    {
        boolean isTotemShelf = state.getBlock() instanceof BlockTotemShelf;
        if (!isTotemShelf && isInvalid(state))
            return null;

        if (isTotemShelf)
        {
            boolean isUpper = state.get(HALF) == DoubleBlockHalf.UPPER;
            BlockPos posLower = pos.down();
            BlockPos posOffset = posLower;
            if (!isUpper)
            {
                posLower = pos;
                pos = posOffset = pos.up();
            }
            return !(world.getBlockState(posOffset).getBlock() instanceof BlockTotemShelf) ? null : new PositionsTotemShelf(state, pos, posLower, !isUpper, null);
        }
        boolean isReversed = false;
        BlockPos posLower = pos.down();
        BlockState stateLower = world.getBlockState(posLower);
        if (isInvalid(stateLower))
        {
            isReversed = true;
            posLower = pos;
            pos = posLower.up();
            state = world.getBlockState(pos);
            if (isInvalid(state))
                return null;
        }
        return new PositionsTotemShelf(state, pos, posLower, isReversed, player);
    }

    private static boolean isInvalid(BlockState state)
    {
        return (state.getBlock() != Blocks.STRIPPED_OAK_LOG && !(state.getBlock() instanceof BlockStrippedOakLog)) || state.get(LogBlock.AXIS) != Axis.Y;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context)
    {
        if (!(context.getEntity() instanceof LivingEntity))
            return SHAPES.get(state);

        if (context.hasItem(ItemsMod.PLANK.get()))
        {
            int stage = state.get(STAGE);
            if (stage > 5 && stage < 10)
                return SHAPES.get(state.with(STAGE, stage + 1));
        }
        TileEntity te = reader.getTileEntity(pos);
        BlockPos posTE = pos;
        DoubleBlockHalf half = state.get(HALF);
        if (half == DoubleBlockHalf.LOWER)
        {
            posTE = pos.up();
            te = reader.getTileEntity(posTE);
        }
        if (te instanceof TileEntityTotemShelf)
        {
            Vec3d startPos = context.getEntity().getEyePosition(1);
            Vec3d endPos = startPos.add(context.getEntity().getLookVec().scale(10));
            VoxelShape shape = SHAPES.get(state);
            BlockRayTraceResult targetShelf = shape.rayTrace(startPos, endPos, pos);
            VoxelShape shapeClosest = null;
            double distanceShortest = Double.MAX_VALUE;
            TileEntityTotemShelf totemShelf = (TileEntityTotemShelf) te;
            Vec3d knifePos = totemShelf.getKnifePos();
            if (knifePos != null)
            {
                VoxelShape shapeKnife = SHAPE_KNIFE.withOffset(knifePos.x - pos.getX(), knifePos.y - pos.getY(), knifePos.z - pos.getZ());
                BlockRayTraceResult targetKnife = shapeKnife.rayTrace(startPos, endPos, pos);
                if (targetKnife != null)
                {
                    distanceShortest = targetKnife.getHitVec().squareDistanceTo(startPos);
                    shapeClosest = shapeKnife;
                }
            }
            else if (context.getEntity() != null && (context.hasItem(ItemsMod.BOUND_TOTEM.get()) || context.hasItem(ItemsMod.BOUND_TOTEM_TELEPORTING.get()) || context.hasItem(Items.AIR)))
            {
                IItemHandler inventory = CapabilityUtil.getInventory(totemShelf);
                if (inventory.isItemValid(0, ((LivingEntity) context.getEntity()).getHeldItemMainhand()))
                {
                    for (int i = 0; i < inventory.getSlots(); i++)
                    {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (context.hasItem(Items.AIR) != stack.isEmpty())
                        {
                            EnumMap<DoubleBlockHalf, VoxelShape[]> shapes = SHAPES_TOTEMS.get(state.get(FACING));
                            BlockRayTraceResult targetTotem = shapes.get(DoubleBlockHalf.UPPER)[i].rayTrace(startPos, endPos, posTE);
                            if (targetTotem != null)
                            {
                                double distance = targetTotem.getHitVec().squareDistanceTo(startPos);
                                if (distance < distanceShortest)
                                {
                                    distanceShortest = distance;
                                    shapeClosest = shapes.get(half)[i];
                                }
                            }
                        }
                    }
                }
            }
            if (shapeClosest != null && (targetShelf == null || distanceShortest < targetShelf.getHitVec().squareDistanceTo(startPos)))
                return VoxelShapes.combine(shape, shapeClosest, IBooleanFunction.OR);
        }
        return SHAPES.get(state);
    }

    @Override
    public VoxelShape getRaytraceShape(BlockState state, IBlockReader world, BlockPos pos)
    {
        return SHAPES_RAYTRACE.get(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context)
    {
        return SHAPES.get(state);
    }

    @Override
    public SoundType getSoundType(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity)
    {
        return state.get(STAGE) < 6 ? BlockStrippedOakLog.getSoundType(state, entity) : super.getSoundType(state, world, pos, entity);
    }

    @Override
    public BlockState updatePostPlacement(BlockState state, Direction facing,
            BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos)
    {
        super.updatePostPlacement(state, facing, facingState, world, currentPos, facingPos);
        if (preventPostPlacementCheck)
            return state;

        DoubleBlockHalf half = state.get(HALF);
        boolean isLower = half == DoubleBlockHalf.LOWER;
        boolean isUp = facing == Direction.UP;
        if (facing.getAxis() == Direction.Axis.Y && isLower == isUp)
            return facingState.getBlock() == this && facingState.get(HALF) != half ? state.with(FACING, facingState.get(FACING))
                    .with(BINDING_STATE, facingState.get(BINDING_STATE)).with(STAGE, facingState.get(STAGE)) : Blocks.AIR.getDefaultState();

        return isLower && !isUp && !state.isValidPosition(world, currentPos) ? Blocks.AIR.getDefaultState() : state;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context)
    {
        if (context.getPos().getY() < 255 && context.getWorld().getBlockState(context.getPos().up()).isReplaceable(context))
            return super.getStateForPlacement(context).with(FACING, context.getPlacementHorizontalFacing().getOpposite());

        return null;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER));
    }

    @Override
    public void harvestBlock(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack)
    {
        super.harvestBlock(world, player, pos, Blocks.AIR.getDefaultState(), te, stack);
    }

    @Override
    public void onBlockHarvested(World world, BlockPos pos, BlockState state, PlayerEntity player)
    {
        DoubleBlockHalf half = state.get(HALF);
        boolean isLower = half == DoubleBlockHalf.LOWER;
        BlockPos posOffset = isLower ? pos.up() : pos.down();
        BlockState stateOffset = world.getBlockState(posOffset);
        if (stateOffset.getBlock() == this && stateOffset.get(HALF) != half)
        {
            preventPostPlacementCheck = true;
            world.setBlockState(posOffset, Blocks.AIR.getDefaultState(), Constants.BlockFlags.DEFAULT | Constants.BlockFlags.NO_NEIGHBOR_DROPS);
            preventPostPlacementCheck = false;
            if (!world.isRemote && !player.isCreative() && state.get(STAGE) == 10 && state.get(BINDING_STATE) != BindingState.BOUND)
            {
                ItemStack stackHeld = player.getHeldItemMainhand();
                spawnDrops(state, world, pos, null, player, stackHeld);
                spawnDrops(stateOffset, world, posOffset, null, player, stackHeld);
            }
        }
        if (state.get(BINDING_STATE) != BindingState.NOT_BOUND && !world.isRemote)
            addShelfBreakingEffects(world, pos, state);

        super.onBlockHarvested(world, pos, state, player);
    }

    public static void addShelfBreakingEffects(World world, BlockPos pos, BlockState state)
    {
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundsMod.EXHALE.get(), SoundCategory.MASTER, 1F, 2.4F - world.rand.nextFloat() * 1F);
        PacketNetwork.sendToAllAround(new PacketShelfSmokeParticles(state.getCollisionShape(world, pos, null).getBoundingBox().offset(pos)), world, pos);
    }

    public static void spawnShelfSmokeParticles(World world, AxisAlignedBB box)
    {
        for (int i = 0; i < 12; i++)
            world.addParticle(ParticleTypes.LARGE_SMOKE,
                    box.minX + (box.maxX - box.minX) * world.rand.nextDouble(),
                    box.minY + (box.maxY - box.minY) * world.rand.nextDouble(),
                    box.minZ + (box.maxZ - box.minZ) * world.rand.nextDouble(),
                    0, 0, 0);
    }

    @Override
    protected void fillStateContainer(Builder<Block, BlockState> builder)
    {
        super.fillStateContainer(builder);
        builder.add(FACING, HALF, BINDING_STATE, STAGE);
    }

    @Override
    public boolean hasTileEntity(BlockState state)
    {
        return state.get(STAGE) == 10;
    }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world)
    {
        return state.get(HALF) == DoubleBlockHalf.UPPER && state.get(BINDING_STATE).isTransitioning() ? new TileEntityTotemShelfBinding() : new TileEntityTotemShelf();
    }

    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random random)
    {
        BindingState bindingState = state.get(BINDING_STATE);
        if (bindingState == BindingState.HEATING)
            EntityUtil.spawnLightning(state, world, pos);

        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityTotemShelf))
            return;

        BindingState bindingStateNext = bindingState.getNext();
        if (bindingStateNext != null)
            ((TileEntityTotemShelf) te).setBindingState(world.getBlockState(pos), bindingStateNext, Constants.BlockFlags.DEFAULT);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult result)
    {
        ItemStack stack = player.getHeldItem(hand);
        if (stack.getItem() instanceof ItemPlank)
        {
            int stage = state.get(STAGE);
            if (stage > 5 && stage < 10)
            {
                PositionsTotemShelf positions = getTotemShelfPositions(state, world, pos);
                if (positions != null)
                {
                    if (world instanceof ServerWorld)
                    {
                        positions.advanceStage((ServerWorld) world);
                        stack.shrink(1);
                    }
                    return ActionResultType.SUCCESS;
                }
            }
            return ActionResultType.PASS;
        }
        if (state.get(HALF) == DoubleBlockHalf.LOWER)
        {
            pos = pos.up();
            state = world.getBlockState(pos);
        }
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityTotemShelf))
            return ActionResultType.PASS;

        TileEntityTotemShelf totemShelf = (TileEntityTotemShelf) te;
        if (totemShelf.giveOrTakeKnife(state, world, pos, player, hand, stack, result))
            return ActionResultType.SUCCESS;

        IItemHandler inventory = CapabilityUtil.getInventory(totemShelf);
        ItemStack stackMain = player.getHeldItemMainhand();
        if (inventory.isItemValid(0, stackMain))
        {
            VoxelShape[] totemShapes = SHAPES_TOTEMS.get(state.get(FACING)).get(DoubleBlockHalf.UPPER);
            for (int i = 0; i < inventory.getSlots(); i++)
            {
                if (TileEntityTotemShelf.getHitShape(totemShapes[i], pos, result) != null
                        && stackMain.isEmpty() != transferTotem(inventory, stackMain, i, true).isEmpty())
                {
                    if (!world.isRemote)
                    {
                        player.setHeldItem(Hand.MAIN_HAND, transferTotem(inventory, stackMain, i, false));
                        float modifier = stackMain.isEmpty() ? 2 : 1;
                        world.playSound(null, pos, SoundEvents.ITEM_BOOK_PUT, SoundCategory.MASTER, 2, 1 * modifier);
                    }
                    return ActionResultType.SUCCESS;
                }
            }
        }
        return ActionResultType.PASS;
    }

    public ItemStack transferTotem(IItemHandler inventory, ItemStack stack, int index, boolean simulate)
    {
        ItemStack stackShelf = inventory.extractItem(index, 1, simulate);
        inventory.insertItem(index, stack, simulate);
        return stackShelf;
    }

    @Override
    public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos)
    {
        return state.get(HALF) == DoubleBlockHalf.LOWER || world.getBlockState(pos.down()).getBlock() == this;
    }

    @Override
    public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (state.getBlock() == newState.getBlock())
            return;

        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTotemShelf)
        {
            TileEntityTotemShelf totemShelf = (TileEntityTotemShelf) te;
            ItemStack knife = totemShelf.getKnife();
            if (!knife.isEmpty())
                InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), knife);

            IItemHandler inventory = CapabilityUtil.getInventory((TileEntityTotemShelf) te);
            for (int i = 0; i < inventory.getSlots(); i++)
            {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty())
                    InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.onReplaced(state, world, pos, newState, isMoving);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.toRotation(state.get(FACING)));
    }

    @Override
    public float getBlockHardness(BlockState state, IBlockReader world, BlockPos pos)
    {
        return state.get(STAGE) > 5 ? 5F : 1.5F;
    }

    @Override
    public boolean hasComparatorInputOverride(BlockState state)
    {
        return true;
    }

    @Override
    public int getComparatorInputOverride(BlockState state, World world, BlockPos pos)
    {
        TileEntity te = world.getTileEntity(state.get(HALF) == DoubleBlockHalf.LOWER ? pos.up() : pos);
        if (!(te instanceof TileEntityTotemShelf))
            return 0;

        IItemHandler inventory = CapabilityUtil.getInventory((TileEntityTotemShelf) te);
        double input = 0;
        double size = (double) inventory.getSlots();
        for (int i = 0; i < size; i++)
            input += inventory.getStackInSlot(i).isEmpty() ? 0 : 15 / size;

        return MathHelper.floor(input);
    }
}