package com.github.phylogeny.boundtotems.tileentity;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.Config;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf.BindingState;
import com.github.phylogeny.boundtotems.block.PositionsTotemShelf;
import com.github.phylogeny.boundtotems.block.ShelfDropRemovalModifier;
import com.github.phylogeny.boundtotems.init.SoundsMod;
import com.github.phylogeny.boundtotems.init.TileEntitiesMod;
import com.github.phylogeny.boundtotems.item.ItemBoundTotem;
import com.github.phylogeny.boundtotems.item.ItemRitualDagger;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketAddOrRemoveKnife;
import com.github.phylogeny.boundtotems.util.CapabilityUtil;
import com.github.phylogeny.boundtotems.util.EntityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class TileEntityTotemShelf extends TileEntity
{
    public static final int SIZE_INVENTORY = 6;
    private final LazyOptional<ItemStackHandler> inventory = LazyOptional.of(this::createInventory);
    protected ItemStack knife = ItemStack.EMPTY;
    private Vec3d knifePos, knifeDirection;
    private UUID boundEntityID;

    public TileEntityTotemShelf()
    {
        super(TileEntitiesMod.TOTEM_SHELF.get());
    }

    public TileEntityTotemShelf(TileEntityType<?> tileEntityType)
    {
        super(tileEntityType);
    }

    public ItemStack getKnife()
    {
        return knife;
    }

    @Nullable
    public UUID getBoundEntityID()
    {
        return boundEntityID;
    }

    @Nullable
    public Vec3d getKnifePos()
    {
        return knifePos == null ? null : knifePos.add(knifeDirection.scale(-0.15));
    }

    @Nullable
    public Vec3d getKnifeDirection()
    {
        return knifeDirection;
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt)
    {
        super.write(nbt);
        NBTUtil.writeObjectToSubTag(nbt, NBTUtil.KNIFE, nbtSub -> knife.write(nbtSub));
        writeVec(nbt, knifePos, NBTUtil.POSITION);
        writeVec(nbt, knifeDirection, NBTUtil.DIRECTION);
        NBTUtil.writeNullableObject(boundEntityID, () -> nbt.put("bound_entity_id", NBTUtil.writeUniqueId(boundEntityID)));
        nbt.put("items", getInventory().serializeNBT());
        return nbt;
    }

    private void writeVec(CompoundNBT nbt, Vec3d vec, String key)
    {
        NBTUtil.writeNullableObject(vec, () -> NBTUtil.writeObjectToSubTag(nbt, key, nbtSub ->
        {
            nbtSub.putDouble(NBTUtil.X, vec.x);
            nbtSub.putDouble(NBTUtil.Y, vec.y);
            nbtSub.putDouble(NBTUtil.Z, vec.z);
        }));
    }

    @Override
    public void read(CompoundNBT nbt)
    {
        super.read(nbt);
        knife = NBTUtil.readObjectFromSubTag(nbt, NBTUtil.KNIFE, ItemStack::read);
        knifePos = readVec(nbt, NBTUtil.POSITION);
        knifeDirection = readVec(nbt, NBTUtil.DIRECTION);
        boundEntityID = NBTUtil.readNullableObject(nbt, "bound_entity_id", () -> NBTUtil.readUniqueId(nbt.getCompound("bound_entity_id")));
        if (nbt.contains("items"))
            getInventory().deserializeNBT((CompoundNBT) nbt.get("items"));
    }

    private Vec3d readVec(CompoundNBT nbt, String key)
    {
        return NBTUtil.readNullableObject(nbt, key, () -> NBTUtil.readObjectFromSubTag(nbt, key, nbtSub ->
                new Vec3d(nbtSub.getDouble(NBTUtil.X), nbtSub.getDouble(NBTUtil.Y), nbtSub.getDouble(NBTUtil.Z))));
    }

    private boolean isInterior(@Nullable Vec3d hit)
    {
        return hit != null && (int) hit.x != hit.x && (int) hit.y != hit.y && (int) hit.z != hit.z;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side)
    {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? inventory.cast() : super.getCapability(cap, side);
    }

    public ItemStackHandler getInventory()
    {
        return inventory.orElseThrow(BoundTotems.EMPTY_OPTIONAL_EXP);
    }

    private ItemStackHandler createInventory()
    {
        return new ItemStackHandler(SIZE_INVENTORY)
        {
            @Override
            protected void onContentsChanged(int slot)
            {
                Block block = getBlockState().getBlock();
                if (world != null)
                {
                    world.addBlockEvent(pos, block, 1, 0);
                    world.notifyNeighborsOfStateChange(pos, block);
                    world.notifyBlockUpdate(pos, getBlockState(), getBlockState(), Constants.BlockFlags.DEFAULT);
                }
                markDirty();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack)
            {
                return stack.isEmpty() || knifePos == null && boundEntityID != null && stack.getItem() instanceof ItemBoundTotem
                        && boundEntityID.equals(NBTUtil.getBoundEntityId(stack)) && !world.getBlockState(pos).get(BlockTotemShelf.CHARRED);
            }
        };
    }

    public boolean giveOrTakeKnife(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack, BlockRayTraceResult result)
    {
        if (result != null)
        {
            if (state.get(BlockTotemShelf.BINDING_STATE) == BindingState.NOT_BOUND && knife.isEmpty() && stack.getItem() instanceof ItemRitualDagger
                    && NBTUtil.hasBoundEntity(stack) && isInterior(result.getHitVec()))
            {
                if (!world.isRemote)
                {
                    addKnife(result.getHitVec(), player.getLookVec(), stack);
                    player.setHeldItem(hand, ItemStack.EMPTY);
                    world.playSound(null, pos, SoundType.WOOD.getPlaceSound(), SoundCategory.MASTER, 1, 1);
                    PacketNetwork.sendToAllAround(new PacketAddOrRemoveKnife(pos, knifePos, knife, knifeDirection), world, player.getLookVec());
                    setBindingState(state, BindingState.HEATING, Constants.BlockFlags.DEFAULT);
                }
                return true;
            }
            else if (state.get(BlockTotemShelf.BINDING_STATE) == BindingState.BOUND && !knife.isEmpty() && stack.isEmpty()
                    && getObservedKnifeShape(pos, result, getKnifePos()) != null)
            {
                if (!world.isRemote)
                {
                    player.setHeldItem(hand, knife);
                    world.playSound(null, pos, SoundType.WOOD.getPlaceSound(), SoundCategory.MASTER, 0.5F, 2);
                    world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.MASTER, 0.25F, 2);
                    Vec3d vec = knifePos.add(knifeDirection.scale(-0.05));
                    removeKnife();
                    PacketNetwork.sendToAllAround(new PacketAddOrRemoveKnife(pos, vec, knife), world, vec);
                }
                return true;
            }
        }
        return false;
    }

    public void addKnife(Vec3d knifePos, Vec3d knifeDirection, ItemStack knifeStack)
    {
        this.knifePos = knifePos;
        this.knifeDirection = knifeDirection;
        knife = knifeStack;
    }

    public void removeKnife()
    {
        knife = ItemStack.EMPTY;
        knifePos = knifeDirection = null;
    }

    @Nullable
    public static VoxelShape getObservedKnifeShape(BlockPos pos, BlockRayTraceResult target, Vec3d knifePos)
    {
        return knifePos == null ? null : getHitShape(BlockTotemShelf.SHAPE_KNIFE.withOffset(knifePos.x - pos.getX(), knifePos.y - pos.getY(), knifePos.z - pos.getZ()), pos, target);
    }

    @Nullable
    public static VoxelShape getHitShape(VoxelShape shape, BlockPos pos, BlockRayTraceResult target)
    {
        return shape.getBoundingBox().grow(0.001, 0.001, 0.001).contains(target.getHitVec().subtract(pos.getX(), pos.getY(), pos.getZ())) ? shape : null;
    }

    public void setBindingState(BlockState state, BindingState bindingState, int flags)
    {
        if (world == null)
            return;

        CompoundNBT nbt = getUpdateTag();
        world.removeTileEntity(pos);
        world.setBlockState(pos, state.with(BlockTotemShelf.BINDING_STATE, bindingState), flags);
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityTotemShelf)
        {
            te.read(nbt);
            markDirty();
            world.notifyBlockUpdate(pos, state, state, Constants.BlockFlags.DEFAULT);
            if (!world.isRemote && bindingState.hasNext())
                world.getPendingBlockTicks().scheduleTick(pos, state.getBlock(), 80);

            if (bindingState == BindingState.COOLING)
                ((TileEntityTotemShelf) te).bindKnifeAndShelf(state);
        }

        if (world.isRemote)
            return;

        if (bindingState == BindingState.HEATING)
            world.playSound(null, pos, SoundsMod.BIND_SHELF.get(), SoundCategory.MASTER, 1, 1);
    }

    private void bindKnifeAndShelf(BlockState state)
    {
        if (!(world instanceof ServerWorld))
            return;

        LivingEntity entity = NBTUtil.getBoundEntity(knife, (ServerWorld) world);
        if (entity == null || entity.getDistanceSq(pos.getX(), pos.getY(), pos.getZ()) > Math.pow(Config.SERVER.maxDistanceToShelf.get(), 2))
        {
            ShelfDropRemovalModifier.setRemoval(true);
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            ShelfDropRemovalModifier.setRemoval(false);
            BlockTotemShelf.addShelfBreakingEffects(world, pos, state, false);
            return;
        }
        AtomicInteger count = new AtomicInteger();
        visitTotemShelves(entity, (world, shelf) ->
        {
            count.incrementAndGet();
            return new ShelfVisitationResult(false, true);
        });
        while (count.get() >= Config.SERVER.maxBoundShelves.get())
        {
            int burnIndex = world.rand.nextInt(count.get()) + 1;
            AtomicInteger countBurn = new AtomicInteger();
            visitTotemShelves(entity, (world, shelf) ->
            {
                boolean forceRemoval = countBurn.incrementAndGet() == burnIndex;
                if (forceRemoval)
                {
                    BlockState statePrimary = world.getBlockState(shelf.pos);
                    PositionsTotemShelf positions = BlockTotemShelf.getTotemShelfPositions(statePrimary, world, shelf.pos);
                    world.setBlockState(shelf.pos, statePrimary.with(BlockTotemShelf.CHARRED, true));
                    if (positions != null)
                    {
                        BlockPos posSecondary = positions.getPosOffset();
                        world.setBlockState(posSecondary, world.getBlockState(posSecondary).with(BlockTotemShelf.CHARRED, true));
                    }
                    EntityUtil.spawnLightning(statePrimary, world, shelf.pos);
                    BlockTotemShelf.addShelfBreakingEffects(world, shelf.pos, statePrimary, true);
                    count.decrementAndGet();
                }
                return new ShelfVisitationResult(forceRemoval, !forceRemoval);
            });
        }
        NBTUtil.bindKnife(knife.getOrCreateTag());
        boundEntityID = entity.getUniqueID();
        ResourceLocation dimension = NBTUtil.getDimensionKey(world);
        Hashtable<ResourceLocation, Set<BlockPos>> positionTable = CapabilityUtil.getShelfPositions(entity).getPositions();
        Set<BlockPos> positions = positionTable.get(dimension);
        if (positions == null)
            positions = new HashSet<>();

        positions.add(pos);
        positionTable.put(dimension, positions);
    }

    public static void visitTotemShelves(LivingEntity entity, BiFunction<ServerWorld, TileEntityTotemShelf, ShelfVisitationResult> action)
    {
        MinecraftServer server = entity.world.getServer();
        if (server == null)
            return;

        Hashtable<ResourceLocation, Set<BlockPos>> positionTable = CapabilityUtil.getShelfPositions(entity).getPositions();
        Set<ResourceLocation> dimensions = positionTable.keySet();
        Iterator<ResourceLocation> iteratorDim = dimensions.iterator();
        while (iteratorDim.hasNext())
        {
            ResourceLocation dimensionKey = iteratorDim.next();
            DimensionType dimension = NBTUtil.getDimension(dimensionKey);
            if (dimension == null)
                continue;

            ServerWorld world = server.getWorld(dimension);
            if (world == null)
                continue;

            Set<BlockPos> positions = positionTable.get(dimensionKey);
            if (positions == null)
            {
                iteratorDim.remove();
                continue;
            }
            Iterator<BlockPos> iteratorPos = positions.iterator();
            while (iteratorPos.hasNext())
            {
                BlockPos pos = iteratorPos.next();
                TileEntity te = world.getTileEntity(pos);
                boolean foundShelf = false;
                if (te instanceof TileEntityTotemShelf)
                {
                    TileEntityTotemShelf totemShelf = (TileEntityTotemShelf) te;
                    if (entity.getUniqueID().equals(totemShelf.getBoundEntityID()) && !world.getBlockState(pos).get(BlockTotemShelf.CHARRED))
                    {
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

    public static class ShelfVisitationResult
    {
        private final boolean forceRemoval, continueVisiting;

        public ShelfVisitationResult(boolean forceRemoval, boolean continueVisiting)
        {
            this.forceRemoval = forceRemoval;
            this.continueVisiting = continueVisiting;
        }

        public boolean forceRemoval()
        {
            return forceRemoval;
        }

        public boolean continueVisiting()
        {
            return continueVisiting;
        }
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt)
    {
        read(pkt.getNbtCompound());
        if (world == null || !world.isRemote)
            return;

        Minecraft.getInstance().execute(() ->
        {
            BlockState state = getBlockState();
            if (state.get(BlockTotemShelf.HALF) != DoubleBlockHalf.UPPER)
                return;

            BindingState bindingState = state.get(BlockTotemShelf.BINDING_STATE);
            if (bindingState == BindingState.HEATING != this instanceof TileEntityTotemShelfBinding)
                setBindingState(state, bindingState, Constants.BlockFlags.DEFAULT);
        });
    }

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        return new SUpdateTileEntityPacket(pos, 0, getUpdateTag());
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        return write(new CompoundNBT());
    }
}