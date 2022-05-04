package com.github.phylogeny.boundtotems.block;

import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.init.SoundsMod;
import com.github.phylogeny.boundtotems.item.ItemCarvingKnife;
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
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockTotemShelf extends BlockWaterLoggable {
    public static final String NAME = "totem_shelf";
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<BindingState> BINDING_STATE = EnumProperty.create("binding_state", BindingState.class);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 10);
    public static final BooleanProperty CHARRED = BooleanProperty.create("charred");
    public static final VoxelShape SHAPE_KNIFE = VoxelShapes.box(-0.15, -0.15, -0.15, 0.15, 0.15, 0.15);
    public static final EnumMap<Direction, EnumMap<DoubleBlockHalf, VoxelShape[]>> SHAPES_TOTEMS;
    public final ImmutableMap<BlockState, VoxelShape> SHAPES, SHAPES_RAYTRACE;
    private boolean preventPostPlacementCheck;

    static {
        VoxelShape shape = Block.box(0, 0, 0, 5.5, 6, 3).move(0.125 + 0.03125, 0.375, 0.0625);
        SHAPES_TOTEMS = createEnumMap(Direction.class, dir -> createEnumMap(DoubleBlockHalf.class, half ->
                IntStream.range(0, TileEntityTotemShelf.SIZE_INVENTORY).mapToObj(i ->
                        dir.getAxis() == Axis.Y ? null : VoxelShapeUtil.rotateShape(shape.move((i % 2) * (0.375 - 0.03125), -(i / 2) * 0.625 + half.ordinal(), 0),
                                dir.getCounterClockWise())).toArray(VoxelShape[]::new)));
    }

    private static <K extends Enum<K>, V> EnumMap<K, V> createEnumMap(Class<K> classEnum, Function<? super K, ? extends V> valueMapper) {
        return Arrays.stream(classEnum.getEnumConstants()).collect(Collectors.toMap(enumValue -> enumValue, valueMapper,
                (v1, v2) -> v2, () -> new EnumMap<>(classEnum)));
    }

    public BlockTotemShelf(Properties properties) {
        super(properties);
        registerDefaultState(getBaseState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(BINDING_STATE, BindingState.NOT_BOUND)
                .setValue(STAGE, 10)
                .setValue(CHARRED, false));
        SHAPES = VoxelShapeUtil.generateShapes(getStateDefinition().getPossibleStates(), FACING, state -> {
            assert state != null;
            int stage = state.getValue(STAGE);
            boolean isUpper = state.getValue(HALF) == DoubleBlockHalf.UPPER;
            if (stage < 5)
                return Collections.singletonList(Block.box(0, -16, 0, 16, 16, 14 - 2 * stage).move(0, isUpper ? 0 : 1, 0));

            List<VoxelShape> shapes = Lists.newArrayList(Block.box(0, -16, 0, 16, 14, stage == 5 ? 4 : 1),
                    Block.box(0, -16, 1, 2, 14, 5), Block.box(14, -16, 1, 16, 14, 5));
            if (stage > 6)
                shapes.add(Block.box(0, -16, 0, 16, -14, 5));

            if (stage > 7)
                shapes.add(Block.box(0, 14, 0, 16, 16, 5));

            if (stage > 8)
                shapes.add(Block.box(0, -6, 0, 16, -4, 5));

            if (stage == 10)
                shapes.add(Block.box(0, 4, 0, 16, 6, 5));

            if (!isUpper)
                shapes = shapes.stream().map(shape -> shape.move(0, 1, 0)).collect(Collectors.toList());

            return shapes;
        });
        SHAPES_RAYTRACE = VoxelShapeUtil.getTransformedShapes(SHAPES, shape -> VoxelShapes.create(shape.bounds()));
    }

    public enum BindingState implements IStringSerializable {
        NOT_BOUND,
        HEATING,
        COOLING,
        BOUND;

        public boolean isTransitioning() {
            return this == HEATING || this == COOLING;
        }

        public boolean hasNext() {
            return ordinal() < values().length - 1;
        }

        @Nullable
        public BindingState getNext() {
            return hasNext() ? values()[ordinal() + 1] : null;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }

        @Override
        public String toString() {
            return getSerializedName();
        }
    }

    @Nullable
    public static PositionsTotemShelf getTotemShelfPositions(BlockState state, IWorld world, BlockPos pos) {
        return getTotemShelfPositions(state, world, pos, null);
    }

    @Nullable
    public static PositionsTotemShelf getTotemShelfPositions(BlockState state, IWorld world, BlockPos pos, @Nullable PlayerEntity player) {
        boolean isTotemShelf = state.getBlock() instanceof BlockTotemShelf;
        if (!isTotemShelf && isInvalid(state))
            return null;

        if (isTotemShelf) {
            boolean isUpper = state.getValue(HALF) == DoubleBlockHalf.UPPER;
            BlockPos posLower = pos.below();
            BlockPos posOffset = posLower;
            if (!isUpper) {
                posLower = pos;
                pos = posOffset = pos.above();
            }
            return !(world.getBlockState(posOffset).getBlock() instanceof BlockTotemShelf) ? null : new PositionsTotemShelf(state, pos, posLower, !isUpper, null);
        }
        boolean isReversed = false;
        BlockPos posLower = pos.below();
        BlockState stateLower = world.getBlockState(posLower);
        if (isInvalid(stateLower)) {
            isReversed = true;
            posLower = pos;
            pos = posLower.above();
            state = world.getBlockState(pos);
            if (isInvalid(state))
                return null;
        }
        return new PositionsTotemShelf(state, pos, posLower, isReversed, player);
    }

    private static boolean isInvalid(BlockState state) {
        return (state.getBlock() != Blocks.STRIPPED_OAK_LOG && !(state.getBlock() instanceof BlockStrippedOakLog)) || state.getValue(RotatedPillarBlock.AXIS) != Axis.Y;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        if (!(context.getEntity() instanceof LivingEntity))
            return SHAPES.get(state);

        if (context.isHoldingItem(ItemsMod.PLANK.get())) {
            int stage = state.getValue(STAGE);
            if (stage > 5 && stage < 10)
                return SHAPES.get(state.setValue(STAGE, stage + 1));
        }
        TileEntity te = reader.getBlockEntity(pos);
        BlockPos posTE = pos;
        DoubleBlockHalf half = state.getValue(HALF);
        if (half == DoubleBlockHalf.LOWER) {
            posTE = pos.above();
            te = reader.getBlockEntity(posTE);
        }
        if (te instanceof TileEntityTotemShelf) {
            Vector3d startPos = context.getEntity().getEyePosition(1);
            Vector3d endPos = startPos.add(context.getEntity().getLookAngle().scale(10));
            VoxelShape shape = SHAPES.get(state);
            BlockRayTraceResult targetShelf = shape.clip(startPos, endPos, pos);
            VoxelShape shapeClosest = null;
            double distanceShortest = Double.MAX_VALUE;
            TileEntityTotemShelf totemShelf = (TileEntityTotemShelf) te;
            Vector3d knifePos = totemShelf.getKnifePos();
            if (knifePos != null) {
                VoxelShape shapeKnife = SHAPE_KNIFE.move(knifePos.x - pos.getX(), knifePos.y - pos.getY(), knifePos.z - pos.getZ());
                BlockRayTraceResult targetKnife = shapeKnife.clip(startPos, endPos, pos);
                if (targetKnife != null) {
                    distanceShortest = targetKnife.getLocation().distanceToSqr(startPos);
                    shapeClosest = shapeKnife;
                }
            } else if (context.getEntity() != null && (context.isHoldingItem(ItemsMod.BOUND_TOTEM.get()) || context.isHoldingItem(ItemsMod.BOUND_TOTEM_TELEPORTING.get()) || context.isHoldingItem(Items.AIR))) {
                IItemHandler inventory = CapabilityUtil.getInventory(totemShelf);
                if (inventory.isItemValid(0, ((LivingEntity) context.getEntity()).getMainHandItem())) {
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (context.isHoldingItem(Items.AIR) != stack.isEmpty()) {
                            EnumMap<DoubleBlockHalf, VoxelShape[]> shapes = SHAPES_TOTEMS.get(state.getValue(FACING));
                            BlockRayTraceResult targetTotem = shapes.get(DoubleBlockHalf.UPPER)[i].clip(startPos, endPos, posTE);
                            if (targetTotem != null) {
                                double distance = targetTotem.getLocation().distanceToSqr(startPos);
                                if (distance < distanceShortest) {
                                    distanceShortest = distance;
                                    shapeClosest = shapes.get(half)[i];
                                }
                            }
                        }
                    }
                }
            }
            if (shapeClosest != null && (targetShelf == null || distanceShortest < targetShelf.getLocation().distanceToSqr(startPos)))
                return VoxelShapes.joinUnoptimized(shape, shapeClosest, IBooleanFunction.OR);
        }
        return SHAPES.get(state);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, IBlockReader world, BlockPos pos) {
        return SHAPES_RAYTRACE.get(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        return SHAPES.get(state);
    }

    @Override
    public SoundType getSoundType(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity entity) {
        return state.getValue(STAGE) < 6 ? BlockStrippedOakLog.getSoundType(state, entity) : super.getSoundType(state, world, pos, entity);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos currentPos, BlockPos facingPos) {
        super.updateShape(state, facing, facingState, world, currentPos, facingPos);
        if (preventPostPlacementCheck)
            return state;

        DoubleBlockHalf half = state.getValue(HALF);
        boolean isLower = half == DoubleBlockHalf.LOWER;
        boolean isUp = facing == Direction.UP;
        if (facing.getAxis() == Direction.Axis.Y && isLower == isUp)
            return facingState.getBlock() == this && facingState.getValue(HALF) != half ? state.setValue(FACING, facingState.getValue(FACING))
                    .setValue(BINDING_STATE, facingState.getValue(BINDING_STATE)).setValue(STAGE, facingState.getValue(STAGE)) : Blocks.AIR.defaultBlockState();

        return isLower && !isUp && !state.canSurvive(world, currentPos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        if (context.getClickedPos().getY() < 255 && context.getLevel().getBlockState(context.getClickedPos().above()).canBeReplaced(context))
            return Objects.requireNonNull(super.getStateForPlacement(context)).setValue(FACING, context.getHorizontalDirection().getOpposite());

        return null;
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        world.setBlockAndUpdate(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER));
    }

    @Override
    public void playerDestroy(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack) {
        super.playerDestroy(world, player, pos, Blocks.AIR.defaultBlockState(), te, stack);
    }

    @Override
    public void playerWillDestroy(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean isLower = half == DoubleBlockHalf.LOWER;
        BlockPos posOffset = isLower ? pos.above() : pos.below();
        BlockState stateOffset = world.getBlockState(posOffset);
        if (stateOffset.getBlock() == this && stateOffset.getValue(HALF) != half) {
            preventPostPlacementCheck = true;
            world.setBlock(posOffset, Blocks.AIR.defaultBlockState(), Constants.BlockFlags.DEFAULT | Constants.BlockFlags.NO_NEIGHBOR_DROPS);
            preventPostPlacementCheck = false;
            if (!world.isClientSide && !player.isCreative() && state.getValue(STAGE) == 10 && state.getValue(BINDING_STATE) != BindingState.BOUND) {
                ItemStack stackHeld = player.getMainHandItem();
                dropResources(state, world, pos, null, player, stackHeld);
                dropResources(stateOffset, world, posOffset, null, player, stackHeld);
            }
        }
        if (state.getValue(BINDING_STATE) != BindingState.NOT_BOUND && !world.isClientSide)
            addShelfBreakingEffects(world, pos, state, false);

        super.playerWillDestroy(world, pos, state, player);
    }

    public static void addShelfBreakingEffects(World world, BlockPos pos, BlockState state, boolean charShelf) {
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundsMod.EXHALE.get(), SoundCategory.MASTER, 1F, 2.4F - world.random.nextFloat());
        PacketNetwork.sendToAllAround(new PacketShelfSmokeParticles(world, pos, state, charShelf), world, pos);
    }

    public static void spawnShelfSmokeParticles(World world, AxisAlignedBB box) {
        spawnShelfSmokeParticles(world, VoxelShapes.empty(), box, false);
    }

    public static void spawnShelfSmokeParticles(World world, VoxelShape shape) {
        spawnShelfSmokeParticles(world, shape, shape.bounds().inflate(0.1), true);
    }

    private static void spawnShelfSmokeParticles(World world, VoxelShape shape, AxisAlignedBB box, boolean addFlames) {
        ReuseableStream<AxisAlignedBB> shapeStream = new ReuseableStream<>(shape.toAabbs().stream());
        for (int i = 0; i < (addFlames ? 36 : 24); i++) {
            AtomicReference<Vector3d> vec = new AtomicReference<>();
            do {
                vec.set(new Vector3d(box.minX + (box.maxX - box.minX) * world.random.nextDouble(),
                                     box.minY + (box.maxY - box.minY) * world.random.nextDouble(),
                                     box.minZ + (box.maxZ - box.minZ) * world.random.nextDouble()));
            }
            while (shapeStream.getStream().anyMatch(b -> b.contains(vec.get())));
            world.addParticle(addFlames ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE,
                    vec.get().x, vec.get().y, vec.get().z, 0, 0, 0);
        }
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, HALF, BINDING_STATE, STAGE, CHARRED);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return state.getValue(STAGE) == 10;
    }

    @Override
    @Nullable
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER && state.getValue(BINDING_STATE).isTransitioning() ? new TileEntityTotemShelfBinding() : new TileEntityTotemShelf();
    }

    @Override
    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BindingState bindingState = state.getValue(BINDING_STATE);
        if (bindingState == BindingState.HEATING)
            EntityUtil.spawnLightning(state, world, pos);

        TileEntity te = world.getBlockEntity(pos);
        if (!(te instanceof TileEntityTotemShelf))
            return;

        BindingState bindingStateNext = bindingState.getNext();
        if (bindingStateNext != null)
            ((TileEntityTotemShelf) te).setBindingState(world.getBlockState(pos), bindingStateNext, Constants.BlockFlags.DEFAULT);
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult result) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() == ItemsMod.PLANK.get()) {
            int stage = state.getValue(STAGE);
            if (stage > 5 && stage < 10) {
                PositionsTotemShelf positions = getTotemShelfPositions(state, world, pos);
                if (positions != null) {
                    if (world instanceof ServerWorld) {
                        positions.advanceStage((ServerWorld) world);
                        stack.shrink(1);
                    }
                    return ActionResultType.SUCCESS;
                }
            }
            return ActionResultType.PASS;
        }
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            pos = pos.above();
            state = world.getBlockState(pos);
        }
        TileEntity te = world.getBlockEntity(pos);
        if (!(te instanceof TileEntityTotemShelf))
            return ActionResultType.PASS;

        TileEntityTotemShelf totemShelf = (TileEntityTotemShelf) te;
        if (totemShelf.giveOrTakeKnife(state, world, pos, player, hand, stack, result))
            return ActionResultType.SUCCESS;

        IItemHandler inventory = CapabilityUtil.getInventory(totemShelf);
        ItemStack stackMain = player.getMainHandItem();
        if (inventory.isItemValid(0, stackMain)) {
            VoxelShape[] totemShapes = SHAPES_TOTEMS.get(state.getValue(FACING)).get(DoubleBlockHalf.UPPER);
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (TileEntityTotemShelf.getHitShape(totemShapes[i], pos, result) != null
                        && stackMain.isEmpty() != transferTotem(inventory, stackMain, i, true).isEmpty()) {
                    if (!world.isClientSide) {
                        player.setItemInHand(Hand.MAIN_HAND, transferTotem(inventory, stackMain, i, false));
                        float modifier = stackMain.isEmpty() ? 2 : 1;
                        world.playSound(null, pos, SoundEvents.BOOK_PUT, SoundCategory.MASTER, 2, 1 * modifier);
                    }
                    return ActionResultType.SUCCESS;
                }
            }
        }
        return ActionResultType.PASS;
    }

    public ItemStack transferTotem(IItemHandler inventory, ItemStack stack, int index, boolean simulate) {
        ItemStack stackShelf = inventory.extractItem(index, 1, simulate);
        inventory.insertItem(index, stack, simulate);
        return stackShelf;
    }

    @Override
    public boolean canSurvive(BlockState state, IWorldReader world, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER || world.getBlockState(pos.below()).getBlock() == this;
    }

    @Override
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() == newState.getBlock())
            return;

        TileEntity te = world.getBlockEntity(pos);
        if (te instanceof TileEntityTotemShelf) {
            TileEntityTotemShelf totemShelf = (TileEntityTotemShelf) te;
            ItemStack knife = totemShelf.getKnife();
            if (!knife.isEmpty())
                InventoryHelper.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), knife);

            IItemHandler inventory = CapabilityUtil.getInventory((TileEntityTotemShelf) te);
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty())
                    InventoryHelper.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return mirror == Mirror.NONE ? state : state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public float getDestroyProgress(BlockState state, PlayerEntity player, IBlockReader world, BlockPos pos) {
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness == -1 || player.getMainHandItem().getItem() instanceof ItemCarvingKnife && state.hasProperty(STAGE) && state.getValue(STAGE) > 5)
            return 0;

        float speed = ForgeHooks.canHarvestBlock(state, player, world, pos) ? 30 : 100;
        return player.getDigSpeed(state, pos) / hardness / speed;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, World world, BlockPos pos) {
        TileEntity te = world.getBlockEntity(state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos);
        if (!(te instanceof TileEntityTotemShelf))
            return 0;

        IItemHandler inventory = CapabilityUtil.getInventory((TileEntityTotemShelf) te);
        double input = 0;
        double size = inventory.getSlots();
        for (int i = 0; i < size; i++)
            input += inventory.getStackInSlot(i).isEmpty() ? 0 : 15 / size;

        return MathHelper.floor(input);
    }
}