package com.github.phylogeny.boundtotems.blockentity;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.Config;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf.BindingState;
import com.github.phylogeny.boundtotems.block.PositionsTotemShelf;
import com.github.phylogeny.boundtotems.init.SoundsMod;
import com.github.phylogeny.boundtotems.init.BlockEntitiesMod;
import com.github.phylogeny.boundtotems.item.ItemBoundTotem;
import com.github.phylogeny.boundtotems.item.ItemRitualDagger;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketAddGhost;
import com.github.phylogeny.boundtotems.network.packet.PacketAddOrRemoveKnife;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.EntityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class BlockEntityTotemShelf extends BlockEntity {
    public static final int SIZE_INVENTORY = 6;
    private final LazyOptional<ItemStackHandler> inventory = LazyOptional.of(this::createInventory);
    protected ItemStack knife = ItemStack.EMPTY;
    private Vec3 knifePos, knifeDirection;
    private UUID boundEntityId, ownerId;

    public BlockEntityTotemShelf(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public BlockEntityTotemShelf(BlockPos pos, BlockState state) {
        this(BlockEntitiesMod.TOTEM_SHELF.get(), pos, state);
    }

    public ItemStack getKnife() {
        return knife;
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    @Nullable
    public Vec3 getKnifePos() {
        return knifePos == null ? null : knifePos.add(knifeDirection.scale(-0.15));
    }

    @Nullable
    public Vec3 getKnifeDirection() {
        return knifeDirection;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        super.save(nbt);
        NBTUtil.writeObjectToSubTag(nbt, NBTUtil.KNIFE, nbtSub -> knife.save(nbtSub));
        writeVec(nbt, knifePos, NBTUtil.POSITION);
        writeVec(nbt, knifeDirection, NBTUtil.DIRECTION);
        NBTUtil.writeNullableObject(boundEntityId, key -> nbt.put("bound_entity_id", NBTUtil.writeUniqueId(key)));
        NBTUtil.writeNullableObject(ownerId, key -> nbt.put("owner_id", NBTUtil.writeUniqueId(key)));
        nbt.put("items", getInventory().serializeNBT());
        return nbt;
    }

    private void writeVec(CompoundTag nbt, Vec3 vec, String key) {
        NBTUtil.writeNullableObject(vec, v -> NBTUtil.writeObjectToSubTag(nbt, key, nbtSub -> {
            nbtSub.putDouble(NBTUtil.X, v.x);
            nbtSub.putDouble(NBTUtil.Y, v.y);
            nbtSub.putDouble(NBTUtil.Z, v.z);
        }));
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        knife = NBTUtil.readObjectFromSubTag(nbt, NBTUtil.KNIFE, ItemStack::of);
        knifePos = readVec(nbt, NBTUtil.POSITION);
        knifeDirection = readVec(nbt, NBTUtil.DIRECTION);
        boundEntityId = NBTUtil.readNullableObject(nbt, "bound_entity_id", key -> NBTUtil.readUniqueId(nbt.getCompound(key)));
        ownerId = NBTUtil.readNullableObject(nbt, "owner_id", key -> NBTUtil.readUniqueId(nbt.getCompound(key)));
        if (nbt.contains("items"))
            getInventory().deserializeNBT((CompoundTag) nbt.get("items"));
    }

    private Vec3 readVec(CompoundTag nbt, String key) {
        return NBTUtil.readNullableObject(nbt, key, k -> NBTUtil.readObjectFromSubTag(nbt, k, nbtSub ->
                new Vec3(nbtSub.getDouble(NBTUtil.X), nbtSub.getDouble(NBTUtil.Y), nbtSub.getDouble(NBTUtil.Z))));
    }

    private boolean isInterior(@Nullable Vec3 hit) {
        return hit != null && (int) hit.x != hit.x && (int) hit.y != hit.y && (int) hit.z != hit.z;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? inventory.cast() : super.getCapability(cap, side);
    }

    public ItemStackHandler getInventory() {
        return inventory.orElseThrow(BoundTotems.EMPTY_OPTIONAL_EXP);
    }

    private ItemStackHandler createInventory() {
        return new ItemStackHandler(SIZE_INVENTORY) {
            @Override
            protected void onContentsChanged(int slot) {
                Block block = getBlockState().getBlock();
                if (level != null) {
                    level.blockEvent(worldPosition, block, 1, 0);
                    level.updateNeighborsAt(worldPosition, block);
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
                }
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return stack.isEmpty() || knifePos == null && boundEntityId != null && stack.getItem() instanceof ItemBoundTotem
                        && boundEntityId.equals(NBTUtil.getBoundEntityId(stack)) && (level == null || !level.getBlockState(worldPosition).getValue(BlockTotemShelf.CHARRED));
            }
        };
    }

    public boolean giveOrTakeKnife(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult result) {
        if (result != null) {
            if (state.getValue(BlockTotemShelf.BINDING_STATE) == BindingState.NOT_BOUND && knife.isEmpty() && stack.getItem() instanceof ItemRitualDagger
                    && NBTUtil.hasBoundEntity(stack) && isInterior(result.getLocation())) {
                if (!world.isClientSide) {
                    ownerId = player.getUUID();
                    addKnife(result.getLocation(), player.getLookAngle(), stack);
                    player.setItemInHand(hand, ItemStack.EMPTY);
                    world.playSound(null, pos, SoundType.WOOD.getPlaceSound(), SoundSource.MASTER, 1, 1);
                    PacketNetwork.sendToAllAround(new PacketAddOrRemoveKnife(pos, knifePos, knife, knifeDirection), world, player.getLookAngle());
                    setBindingState(state, BindingState.HEATING, Block.UPDATE_ALL);
                }
                return true;
            } else if (state.getValue(BlockTotemShelf.BINDING_STATE) == BindingState.BOUND && !knife.isEmpty() && stack.isEmpty()
                    && getObservedKnifeShape(pos, result, getKnifePos()) != null) {
                if (!world.isClientSide) {
                    player.setItemInHand(hand, knife);
                    world.playSound(null, pos, SoundType.WOOD.getPlaceSound(), SoundSource.MASTER, 0.5F, 2);
                    world.playSound(null, pos, SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.MASTER, 0.25F, 2);
                    Vec3 vec = knifePos.add(knifeDirection.scale(-0.05));
                    removeKnife();
                    PacketNetwork.sendToAllAround(new PacketAddOrRemoveKnife(pos, vec, knife), world, vec);
                }
                return true;
            }
        }
        return false;
    }

    public void addKnife(Vec3 knifePos, Vec3 knifeDirection, ItemStack knifeStack) {
        this.knifePos = knifePos;
        this.knifeDirection = knifeDirection;
        knife = knifeStack;
    }

    public void removeKnife() {
        knife = ItemStack.EMPTY;
        knifePos = knifeDirection = null;
    }

    @Nullable
    public static VoxelShape getObservedKnifeShape(BlockPos pos, BlockHitResult target, Vec3 knifePos) {
        return knifePos == null ? null : getHitShape(BlockTotemShelf.SHAPE_KNIFE
                .move(knifePos.x - pos.getX(), knifePos.y - pos.getY(), knifePos.z - pos.getZ()), pos, target);
    }

    @Nullable
    public static VoxelShape getHitShape(VoxelShape shape, BlockPos pos, BlockHitResult target) {
        return shape.bounds().inflate(0.001, 0.001, 0.001).contains(target.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ())) ? shape : null;
    }

    public void setBindingState(BlockState state, BindingState bindingState, int flags) {
        if (level == null)
            return;

        CompoundTag nbt = getUpdateTag();
        level.removeBlockEntity(worldPosition);
        level.setBlock(worldPosition, state.setValue(BlockTotemShelf.BINDING_STATE, bindingState), flags);
        BlockEntity te = level.getBlockEntity(worldPosition);
        if (te instanceof BlockEntityTotemShelf shelf) {
            te.load(nbt);
            setChanged();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
            if (!level.isClientSide && bindingState.hasNext())
                level.getBlockTicks().scheduleTick(worldPosition, state.getBlock(), 80);

            if (bindingState == BindingState.COOLING)
                shelf.bindKnifeAndShelf();
        }

        if (level.isClientSide)
            return;

        if (bindingState == BindingState.HEATING)
            level.playSound(null, worldPosition, SoundsMod.BIND_SHELF.get(), SoundSource.MASTER, 1, 1);
    }

    private void bindKnifeAndShelf() {
        if (!(level instanceof ServerLevel serverWorld))
            return;

        LivingEntity entity = NBTUtil.getBoundEntity(knife, serverWorld);
        if (entity == null || entity.distanceToSqr(worldPosition.getX(), worldPosition.getY(),
                worldPosition.getZ()) > Math.pow(Config.SERVER.maxDistanceToShelf.get(), 2)) {
            charShelf(serverWorld, worldPosition);
            return;
        }
        AtomicInteger count = new AtomicInteger();
        visitTotemShelves(entity, (world, shelf) -> {
            count.incrementAndGet();
            return new ShelfVisitationResult(false, true);
        });
        while (count.get() >= Config.SERVER.maxBoundShelves.get()) {
            int burnIndex = level.random.nextInt(count.get()) + 1;
            AtomicInteger countBurn = new AtomicInteger();
            visitTotemShelves(entity, (world, shelf) -> {
                boolean forceRemoval = countBurn.incrementAndGet() == burnIndex;
                if (forceRemoval) {
                    charShelf(world, shelf.worldPosition);
                    count.decrementAndGet();
                }
                return new ShelfVisitationResult(forceRemoval, !forceRemoval);
            });
        }
        NBTUtil.bindKnife(knife.getOrCreateTag());
        boundEntityId = entity.getUUID();
        ResourceLocation dimension = NBTUtil.getDimensionKey(level);
        Hashtable<ResourceLocation, Set<BlockPos>> positionTable = CapabilityUtil.getShelfPositions(entity).getPositions();
        Set<BlockPos> positions = positionTable.get(dimension);
        if (positions == null)
            positions = new HashSet<>();

        positions.add(worldPosition);
        positionTable.put(dimension, positions);
        PacketNetwork.sendToAllTrackingAndSelf(new PacketAddGhost(entity, 0.2F,
                level.getBlockState(worldPosition).getCollisionShape(level, worldPosition)
                        .bounds().move(worldPosition).getCenter(), null), entity);
    }

    private void charShelf(ServerLevel world, BlockPos pos) {
        BlockState statePrimary = world.getBlockState(pos);
        PositionsTotemShelf positions = BlockTotemShelf.getTotemShelfPositions(statePrimary, world, pos);
        world.setBlockAndUpdate(pos, statePrimary.setValue(BlockTotemShelf.CHARRED, true));
        if (positions != null) {
            BlockPos posSecondary = positions.getPosOffset();
            world.setBlockAndUpdate(posSecondary, world.getBlockState(posSecondary).setValue(BlockTotemShelf.CHARRED, true));
        }
        EntityUtil.spawnLightning(statePrimary, world, pos);
        BlockTotemShelf.addShelfBreakingEffects(world, pos, statePrimary, true);
    }

    public static void visitTotemShelves(LivingEntity entity, BiFunction<ServerLevel, BlockEntityTotemShelf, ShelfVisitationResult> action) {
        MinecraftServer server = entity.level.getServer();
        if (server == null)
            return;

        Hashtable<ResourceLocation, Set<BlockPos>> positionTable = CapabilityUtil.getShelfPositions(entity).getPositions();
        Set<ResourceLocation> dimensions = positionTable.keySet();
        Iterator<ResourceLocation> iteratorDim = dimensions.iterator();
        while (iteratorDim.hasNext()) {
            ResourceLocation dimensionKey = iteratorDim.next();
            ServerLevel world = server.getLevel(NBTUtil.getDimension(dimensionKey));
            if (world == null)
                continue;

            Set<BlockPos> positions = positionTable.get(dimensionKey);
            if (positions == null) {
                iteratorDim.remove();
                continue;
            }
            Iterator<BlockPos> iteratorPos = positions.iterator();
            while (iteratorPos.hasNext()) {
                BlockPos pos = iteratorPos.next();
                BlockEntity te = world.getBlockEntity(pos);
                boolean foundShelf = false;
                if (te instanceof BlockEntityTotemShelf) {
                    BlockEntityTotemShelf totemShelf = (BlockEntityTotemShelf) te;
                    if (entity.getUUID().equals(totemShelf.boundEntityId) && !world.getBlockState(pos).getValue(BlockTotemShelf.CHARRED)) {
                        ShelfVisitationResult result = action.apply(world, totemShelf);
                        foundShelf = !result.forceRemoval();
                        if (!result.continueVisiting())
                            return;
                    }
                }
                if (!foundShelf)
                    iteratorPos.remove();
            }
            if (positions.isEmpty())
                iteratorDim.remove();
        }
    }

    public static class ShelfVisitationResult {
        private final boolean forceRemoval, continueVisiting;

        public ShelfVisitationResult(boolean forceRemoval, boolean continueVisiting) {
            this.forceRemoval = forceRemoval;
            this.continueVisiting = continueVisiting;
        }

        public boolean forceRemoval() {
            return forceRemoval;
        }

        public boolean continueVisiting() {
            return continueVisiting;
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(pkt.getTag());
        if (level == null || !level.isClientSide)
            return;

        Minecraft.getInstance().execute(() -> {
            BlockState state = getBlockState();
            if (state.getValue(BlockTotemShelf.HALF) != DoubleBlockHalf.UPPER)
                return;

            BindingState bindingState = state.getValue(BlockTotemShelf.BINDING_STATE);
            if (bindingState == BindingState.HEATING != this instanceof BlockEntityTotemShelfBinding)
                setBindingState(state, bindingState, Block.UPDATE_ALL);
        });
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return new ClientboundBlockEntityDataPacket(worldPosition, 0, getUpdateTag());
    }

    @Override
    public CompoundTag getUpdateTag() {
        return save(new CompoundTag());
    }
}