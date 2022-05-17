package com.github.phylogeny.boundtotems.block;

import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelf;
import com.github.phylogeny.boundtotems.blockentity.BlockEntityTotemShelfBinding;
import com.github.phylogeny.boundtotems.init.BlockEntitiesMod;
import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.init.SoundsMod;
import com.github.phylogeny.boundtotems.item.ItemCarvingKnife;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketShelfSmokeParticles;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.EntityUtil;
import com.github.phylogeny.boundtotems.util.VoxelShapeUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RewindableStream;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BlockTotemShelf extends Block implements EntityBlock, SimpleWaterloggedBlock {
    public static final String NAME = "totem_shelf";
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<BindingState> BINDING_STATE = EnumProperty.create("binding_state", BindingState.class);
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 10);
    public static final BooleanProperty CHARRED = BooleanProperty.create("charred");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final VoxelShape SHAPE_KNIFE = Shapes.box(-0.15, -0.15, -0.15, 0.15, 0.15, 0.15);
    public static final EnumMap<Direction, EnumMap<DoubleBlockHalf, VoxelShape[]>> SHAPES_TOTEMS;
    public final ImmutableMap<BlockState, VoxelShape> SHAPES, SHAPES_RAYTRACE;
    private boolean preventPostPlacementCheck;

    static {
        VoxelShape shape = Block.box(0, 0, 0, 5.5, 6, 3).move(0.125 + 0.03125, 0.375, 0.0625);
        SHAPES_TOTEMS = createEnumMap(Direction.class, dir -> createEnumMap(DoubleBlockHalf.class, half ->
                IntStream.range(0, BlockEntityTotemShelf.SIZE_INVENTORY).mapToObj(i ->
                        dir.getAxis() == Axis.Y ? null : VoxelShapeUtil.rotateShape(shape.move((i % 2) * (0.375 - 0.03125), -(i / 2) * 0.625 + half.ordinal(), 0),
                                dir.getCounterClockWise())).toArray(VoxelShape[]::new)));
    }

    private static <K extends Enum<K>, V> EnumMap<K, V> createEnumMap(Class<K> classEnum, Function<? super K, ? extends V> valueMapper) {
        return Arrays.stream(classEnum.getEnumConstants()).collect(Collectors.toMap(enumValue -> enumValue, valueMapper,
                (v1, v2) -> v2, () -> new EnumMap<>(classEnum)));
    }

    public BlockTotemShelf(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(BINDING_STATE, BindingState.NOT_BOUND)
                .setValue(STAGE, 10)
                .setValue(CHARRED, false)
                .setValue(WATERLOGGED, false));
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
        SHAPES_RAYTRACE = VoxelShapeUtil.getTransformedShapes(SHAPES, shape -> Shapes.create(shape.bounds()));
    }

    public enum BindingState implements StringRepresentable {
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
    public static PositionsTotemShelf getTotemShelfPositions(BlockState state, Level world, BlockPos pos) {
        return getTotemShelfPositions(state, world, pos, null);
    }

    @Nullable
    public static PositionsTotemShelf getTotemShelfPositions(BlockState state, LevelAccessor world, BlockPos pos, @Nullable Player player) {
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
    public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        if (!(context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity().isPresent()
                && entityContext.getEntity().get() instanceof LivingEntity entity))
            return SHAPES.get(state);

        if (context.isHoldingItem(ItemsMod.PLANK.get())) {
            int stage = state.getValue(STAGE);
            if (stage > 5 && stage < 10)
                return SHAPES.get(state.setValue(STAGE, stage + 1));
        }
        BlockEntity te = reader.getBlockEntity(pos);
        BlockPos posTE = pos;
        DoubleBlockHalf half = state.getValue(HALF);
        if (half == DoubleBlockHalf.LOWER) {
            posTE = pos.above();
            te = reader.getBlockEntity(posTE);
        }
        if (te instanceof BlockEntityTotemShelf totemShelf) {
            Vec3 startPos = entity.getEyePosition(1);
            Vec3 endPos = startPos.add(entity.getLookAngle().scale(10));
            VoxelShape shape = SHAPES.get(state);
            BlockHitResult targetShelf = shape.clip(startPos, endPos, pos);
            VoxelShape shapeClosest = null;
            double distanceShortest = Double.MAX_VALUE;
            Vec3 knifePos = totemShelf.getKnifePos();
            if (knifePos != null) {
                VoxelShape shapeKnife = SHAPE_KNIFE.move(knifePos.x - pos.getX(), knifePos.y - pos.getY(), knifePos.z - pos.getZ());
                BlockHitResult targetKnife = shapeKnife.clip(startPos, endPos, pos);
                if (targetKnife != null) {
                    distanceShortest = targetKnife.getLocation().distanceToSqr(startPos);
                    shapeClosest = shapeKnife;
                }
            } else if (context.isHoldingItem(ItemsMod.BOUND_TOTEM.get()) || context.isHoldingItem(ItemsMod.BOUND_TOTEM_TELEPORTING.get()) || context.isHoldingItem(Items.AIR)) {
                IItemHandler inventory = CapabilityUtil.getInventory(totemShelf);
                if (inventory.isItemValid(0, entity.getMainHandItem())) {
                    for (int i = 0; i < inventory.getSlots(); i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (context.isHoldingItem(Items.AIR) != stack.isEmpty()) {
                            EnumMap<DoubleBlockHalf, VoxelShape[]> shapes = SHAPES_TOTEMS.get(state.getValue(FACING));
                            BlockHitResult targetTotem = shapes.get(DoubleBlockHalf.UPPER)[i].clip(startPos, endPos, posTE);
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
                return Shapes.joinUnoptimized(shape, shapeClosest, BooleanOp.OR);
        }
        return SHAPES.get(state);
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return SHAPES_RAYTRACE.get(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state);
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, @Nullable Entity entity) {
        return state.getValue(STAGE) < 6 ? BlockStrippedOakLog.getSoundType(state, entity) : super.getSoundType(state, world, pos, entity);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedPos().getY() < 255 && context.getLevel().getBlockState(context.getClickedPos().above()).canBeReplaced(context))
            return Objects.requireNonNull(super.getStateForPlacement(context)).setValue(FACING, context.getHorizontalDirection().getOpposite());

        return null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        world.setBlockAndUpdate(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER));
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        super.playerDestroy(world, player, pos, Blocks.AIR.defaultBlockState(), te, stack);
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean isLower = half == DoubleBlockHalf.LOWER;
        BlockPos posOffset = isLower ? pos.above() : pos.below();
        BlockState stateOffset = world.getBlockState(posOffset);
        if (stateOffset.getBlock() == this && stateOffset.getValue(HALF) != half) {
            preventPostPlacementCheck = true;
            world.setBlock(posOffset, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
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

    public static void addShelfBreakingEffects(Level world, BlockPos pos, BlockState state, boolean charShelf) {
        world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundsMod.EXHALE.get(), SoundSource.MASTER, 1F, 2.4F - world.random.nextFloat());
        PacketNetwork.sendToAllAround(new PacketShelfSmokeParticles(world, pos, state, charShelf), world, pos);
    }

    public static void spawnShelfSmokeParticles(Level world, AABB box) {
        spawnShelfSmokeParticles(world, Shapes.empty(), box, false);
    }

    public static void spawnShelfSmokeParticles(Level world, VoxelShape shape) {
        spawnShelfSmokeParticles(world, shape, shape.bounds().inflate(0.1), true);
    }

    private static void spawnShelfSmokeParticles(Level world, VoxelShape shape, AABB box, boolean addFlames) {
        RewindableStream<AABB> shapeStream = new RewindableStream<>(shape.toAabbs().stream());
        for (int i = 0; i < (addFlames ? 36 : 24); i++) {
            AtomicReference<Vec3> vec = new AtomicReference<>();
            do {
                vec.set(new Vec3(box.minX + (box.maxX - box.minX) * world.random.nextDouble(),
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
        builder.add(FACING, HALF, BINDING_STATE, STAGE, CHARRED, WATERLOGGED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(STAGE) < 10 ? null : (state.getValue(HALF) == DoubleBlockHalf.UPPER && state.getValue(BINDING_STATE).isTransitioning() ? new BlockEntityTotemShelfBinding(pos, state) : new BlockEntityTotemShelf(pos, state));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> blockEntityType) {
        return !world.isClientSide() ? null : createTickerHelper(blockEntityType, BlockEntitiesMod.TOTEM_SHELF_BINDING.get(), BlockEntityTotemShelfBinding::tick);
    }

    /**
     * Copy of {@link BaseEntityBlock#createTickerHelper}
     */
    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(BlockEntityType<A> targetType, BlockEntityType<E> type, BlockEntityTicker<? super E> ticker) {
        return type == targetType ? (BlockEntityTicker<A>)ticker : null;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random random) {
        BindingState bindingState = state.getValue(BINDING_STATE);
        if (bindingState == BindingState.HEATING)
            EntityUtil.spawnLightning(state, world, pos);

        if (bindingState.hasNext() && world.getBlockEntity(pos) instanceof BlockEntityTotemShelf shelf)
            shelf.setBindingState(world.getBlockState(pos), bindingState.getNext(), Block.UPDATE_ALL);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult result) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() == ItemsMod.PLANK.get()) {
            int stage = state.getValue(STAGE);
            if (stage > 5 && stage < 10) {
                PositionsTotemShelf positions = getTotemShelfPositions(state, world, pos);
                if (positions != null) {
                    if (world instanceof ServerLevel serverWorld) {
                        positions.advanceStage(serverWorld);
                        stack.shrink(1);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            pos = pos.above();
            state = world.getBlockState(pos);
        }
        BlockEntity te = world.getBlockEntity(pos);
        if (!(te instanceof BlockEntityTotemShelf))
            return InteractionResult.PASS;

        BlockEntityTotemShelf totemShelf = (BlockEntityTotemShelf) te;
        if (totemShelf.giveOrTakeKnife(state, world, pos, player, hand, stack, result))
            return InteractionResult.SUCCESS;

        IItemHandler inventory = CapabilityUtil.getInventory(totemShelf);
        ItemStack stackMain = player.getMainHandItem();
        if (inventory.isItemValid(0, stackMain)) {
            VoxelShape[] totemShapes = SHAPES_TOTEMS.get(state.getValue(FACING)).get(DoubleBlockHalf.UPPER);
            for (int i = 0; i < inventory.getSlots(); i++) {
                if (BlockEntityTotemShelf.getHitShape(totemShapes[i], pos, result) != null
                        && stackMain.isEmpty() != transferTotem(inventory, stackMain, i, true).isEmpty()) {
                    if (!world.isClientSide) {
                        player.setItemInHand(InteractionHand.MAIN_HAND, transferTotem(inventory, stackMain, i, false));
                        float modifier = stackMain.isEmpty() ? 2 : 1;
                        world.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.MASTER, 2, 1 * modifier);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    public ItemStack transferTotem(IItemHandler inventory, ItemStack stack, int index, boolean simulate) {
        ItemStack stackShelf = inventory.extractItem(index, 1, simulate);
        inventory.insertItem(index, stack, simulate);
        return stackShelf;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER || world.getBlockState(pos.below()).getBlock() == this;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() == newState.getBlock())
            return;

        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof BlockEntityTotemShelf shelf) {
            ItemStack knife = shelf.getKnife();
            if (!knife.isEmpty())
                Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), knife);

            IItemHandler inventory = CapabilityUtil.getInventory(shelf);
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty())
                    Containers.dropItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
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
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness == -1 || player.getMainHandItem().getItem() instanceof ItemCarvingKnife && state.hasProperty(STAGE) && state.getValue(STAGE) > 5)
            return 0;

        float speed = ForgeHooks.isCorrectToolForDrops(state, player) ? 30 : 100;
        return player.getDigSpeed(state, pos) / hardness / speed;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos);
        if (!(te instanceof BlockEntityTotemShelf shelf))
            return 0;

        IItemHandler inventory = CapabilityUtil.getInventory(shelf);
        double input = 0;
        double size = inventory.getSlots();
        for (int i = 0; i < size; i++)
            input += inventory.getStackInSlot(i).isEmpty() ? 0 : 15 / size;

        return Mth.floor(input);
    }
}