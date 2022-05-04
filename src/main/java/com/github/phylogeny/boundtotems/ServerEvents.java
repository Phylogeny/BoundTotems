package com.github.phylogeny.boundtotems;

import com.github.phylogeny.boundtotems.Config.Server.InventorySearch;
import com.github.phylogeny.boundtotems.block.BlockStrippedOakLog;
import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.block.PositionsTotemShelf;
import com.github.phylogeny.boundtotems.item.ItemBoundTotem;
import com.github.phylogeny.boundtotems.item.ItemBoundTotemTeleporting;
import com.github.phylogeny.boundtotems.item.ItemCarvingKnife;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketTotemAnimation;
import com.github.phylogeny.boundtotems.network.packet.PacketTotemParticlesAndSound;
import com.github.phylogeny.boundtotems.tileentity.TileEntityTotemShelf;
import com.github.phylogeny.boundtotems.util.LangUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import com.github.phylogeny.boundtotems.util.ReflectionUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.stats.Stats;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@EventBusSubscriber
public class ServerEvents
{
    private static final Field LAST_SENT_STATE = ObfuscationReflectionHelper.findField(PlayerInteractionManager.class, "field_73094_o");
    private static final Hashtable<BlockPos, Boolean> BREAKING_SHELVES = new Hashtable<>();
    private static final Set<Runnable> END_OF_TICK_TASKS = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void addTotemShelfBreakingOverlay(PlayerInteractEvent.LeftClickBlock event)
    {
        PositionsTotemShelf positions = getPositionsTotemShelf(event.getPlayer(), event.getWorld().getBlockState(event.getPos()), event.getPos());
        if (positions == null)
            return;

        BlockPos pos = positions.getPosOffset();
        Boolean initialized = BREAKING_SHELVES.get(pos);
        if (initialized == null)
            BREAKING_SHELVES.put(pos, false);
        else
            BREAKING_SHELVES.remove(pos);

        event.getPlayer().level.destroyBlockProgress(-1, pos, -1);
    }

    @SubscribeEvent
    public static void addTotemShelfBreakingOverlay(PlayerEvent.BreakSpeed event)
    {
        PositionsTotemShelf positions = getPositionsTotemShelf(event.getPlayer(), event.getState(), event.getPos());
        if (positions == null)
            return;

        BlockPos pos = positions.getPosOffset();
        Boolean initialized = BREAKING_SHELVES.get(pos);
        if (initialized == null)
            return;

        if (!initialized)
        {
            BREAKING_SHELVES.put(pos, true);
            return;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        int progress = (int) ReflectionUtil.getValue(LAST_SENT_STATE, player.gameMode);
        player.level.destroyBlockProgress(-1, pos, progress - 1);
    }

    @Nullable
    public static PositionsTotemShelf getPositionsTotemShelf(PlayerEntity player, BlockState state, BlockPos pos2)
    {
        if (!(player instanceof ServerPlayerEntity) || !(state.getBlock() instanceof BlockTotemShelf || state.getBlock() instanceof BlockStrippedOakLog))
            return null;

        return BlockTotemShelf.getTotemShelfPositions(state, player.level, pos2);
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void sculptTotemShelf(BreakEvent event)
    {
        ItemStack knife = event.getPlayer().getMainHandItem();
        if (!(knife.getItem() instanceof ItemCarvingKnife) || !(event.getWorld() instanceof ServerWorld))
            return;

        PositionsTotemShelf positions = BlockTotemShelf.getTotemShelfPositions(event.getState(), event.getWorld(), event.getPos(), event.getPlayer());
        if (positions == null)
            return;

        Integer stage = positions.getNextStage();
        if (stage == null || stage > 6)
            return;

        event.setCanceled(true);
        positions.advanceStage((ServerWorld) event.getWorld());
        knife.hurtAndBreak(1, event.getPlayer(), p -> p.broadcastBreakEvent(Hand.MAIN_HAND));
    }

    @SubscribeEvent
    public static void performEndOfTickTasks(TickEvent.ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && !END_OF_TICK_TASKS.isEmpty())
        {
            END_OF_TICK_TASKS.forEach(Runnable::run);
            END_OF_TICK_TASKS.clear();
        }
    }

    /**
     * Mimics LivingEntity#checkTotemDeathProtection to allow totems of
     * undying to prevent death from anywhere in a player's inventory.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDeath(LivingDeathEvent event)
    {
        LivingEntity entity = event.getEntityLiving();
        DamageSource source = event.getSource();
        if (!Config.SERVER.preventCreativeModeDeath.get() && source.isBypassInvul())
            return;

        ItemStack totem = findBoundTotem(entity);
        if (totem.isEmpty())
            return;

        ServerPlayerEntity player = entity instanceof ServerPlayerEntity ? (ServerPlayerEntity) entity : null;
        if (player != null)
        {
            player.awardStat(Stats.ITEM_USED.get(totem.getItem()));
            CriteriaTriggers.USED_TOTEM.trigger(player, totem);
        }
        float heath = Config.SERVER.health.get().floatValue();
        entity.setHealth(Config.SERVER.setHealthToPercentageOfMax.get() ? heath * entity.getMaxHealth() : heath);
        if (Config.SERVER.clearPotionEffects.get())
            entity.removeAllEffects();

        for (String potionEffect : Config.SERVER.potionEffects.get())
        {
            try
            {
                addPotionEffect(entity, potionEffect);
            }
            catch (IllegalPotionEffectArgumentException e)
            {
                entity.sendMessage(new TranslationTextComponent(LangUtil.getKey("potion_effect", "error"), potionEffect, e.getMessage()), Util.NIL_UUID);
            }
        }
        boolean teleporting = totem.getItem() instanceof ItemBoundTotemTeleporting;
        if (teleporting)
            END_OF_TICK_TASKS.add(() -> NBTUtil.teleportEntity(totem, entity));

        if (Config.SERVER.spawnParticles.get() || Config.SERVER.playSound.get())
            PacketNetwork.sendToAllTrackingAndSelf(new PacketTotemParticlesAndSound(entity), entity);

        if (player != null && Config.SERVER.playAnimation.get())
            PacketNetwork.sendTo(new PacketTotemAnimation(teleporting), player);

        event.setCanceled(true);
    }

    /**
     * Mimics part of CommandEffect#execute in MC 1.12 to convert a config-specified string into a potion effect that is then applied to the player.
     */
    private static void addPotionEffect(LivingEntity entity, String potionEffect) throws IllegalPotionEffectArgumentException
    {
        String[] args = potionEffect.split(" ");
        Effect potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(args[0]));
        if (potion == null)
            throw new IllegalPotionEffectArgumentException("not_found", args[0]);

        int duration = potion.isInstantenous() ? 1 : 600;
        if (args.length >= 2)
        {
            duration = parseInt(args[1], 0, 1000000);
            if (!potion.isInstantenous())
                duration *= 20;
        }
        int amplifier = args.length >= 3 ? parseInt(args[2], 0, 255) : 0;
        if (duration > 0)
            entity.addEffect(new EffectInstance(potion, duration, amplifier, false, !(args.length >= 4 && "true".equalsIgnoreCase(args[3]))));
    }

    public static int parseInt(String input, int min, int max) throws IllegalPotionEffectArgumentException
    {
        int i;
        try
        {
            i = Integer.parseInt(input);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalPotionEffectArgumentException("num.invalid", input);
        }
        if (i < min)
            throw new IllegalPotionEffectArgumentException("num.too_small", i, min);
        else if (i > max)
            throw new IllegalPotionEffectArgumentException("num.too_big", i, max);

        return i;
    }

    private static class IllegalPotionEffectArgumentException extends IllegalArgumentException
    {
        private final TranslationTextComponent message;

        public IllegalPotionEffectArgumentException(String translationKeySuffix, Object... args)
        {
            message = new TranslationTextComponent(LangUtil.getKey("potion_effect", translationKeySuffix), args);
        }

        @Override
        public String getMessage()
        {
            return message.getString();
        }
    }

    private static boolean isValidTotem(ItemStack stack, LivingEntity entity)
    {
        return stack.getItem() instanceof ItemBoundTotem && NBTUtil.matchesBoundEntity(stack, entity);
    }

    /**
     * Searches the player's inventory slots -- as well as the slots of shulker boxes (using code from 
     * {@link net.minecraft.block.ShulkerBoxBlock#appendHoverText appendHoverText}
     * in ShulkerBoxBlock) and other inventory-containing items -- for totems of undying.
     * 
     * @return if found, the totem removed from the players's inventory; if not, an empty stack
     */
    private static ItemStack findBoundTotem(LivingEntity entity)
    {
        ItemStack totem;
        if (!(entity instanceof ServerPlayerEntity) || Config.SERVER.inventorySearch.get() == InventorySearch.HELD_ONLY)
            totem = getHeldTotem(entity);
        else
            totem = getTotemFromInventory((ServerPlayerEntity) entity);

        if (totem.isEmpty())
            totem = getTotemFromShelf(entity);

        return totem;
    }

    private static ItemStack getTotemFromInventory(ServerPlayerEntity player)
    {
        boolean hotbarOnly = Config.SERVER.inventorySearch.get() == InventorySearch.HOTBAR_ONLY;
        ItemStack totem = ItemStack.EMPTY;
        ItemStack stack;
        // Check inventory for totems in slots
        for (int i = 0; i < player.inventory.getContainerSize(); i++)
        {
            if (hotbarOnly && !PlayerInventory.isHotbarSlot(i))
                continue;

            stack = player.inventory.getItem(i);
            if (isValidTotem(stack, player))
            {
                totem = stack.copy();
                player.inventory.setItem(i, ItemStack.EMPTY);
                return totem;
            }
        }
        // Check inventory for totems in stacks with inventories
        for (int i = 0; i < player.inventory.getContainerSize(); i++)
        {
            if (hotbarOnly && !PlayerInventory.isHotbarSlot(i))
                continue;

            totem = getTotemFromStack(player, player.inventory.getItem(i));
            if (!totem.isEmpty())
                return totem;
        }
        return totem;
    }

    private static ItemStack getHeldTotem(LivingEntity entity)
    {
        // Check inventory for totems in slots
        for (Hand hand : Hand.values())
        {
            ItemStack stack = entity.getItemInHand(hand);
            if (isValidTotem(stack, entity))
            {
                ItemStack totem = stack.copy();
                stack.shrink(1);
                return totem;
            }
        }
        // Check inventory for totems in stacks with inventories
        for (Hand hand : Hand.values())
        {
            ItemStack totem = getTotemFromStack(entity, entity.getItemInHand(hand));
            if (!totem.isEmpty())
                return totem;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack getTotemFromStack(LivingEntity entity, ItemStack stack)
    {
        CompoundNBT nbt;
        NonNullList<ItemStack> stacks;
        ItemStack stack2;
        // Check inventory for totems in shulker boxes
        if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock)
        {
            nbt = stack.getTagElement("BlockEntityTag");
            if (nbt != null && nbt.contains("Items", NBT.TAG_LIST))
            {
                stacks = NonNullList.withSize(27, ItemStack.EMPTY);
                ItemStackHelper.loadAllItems(nbt, stacks);
                for (int j = 0; j < stacks.size(); j++)
                {
                    stack2 = stacks.get(j);
                    if (isValidTotem(stack2, entity))
                    {
                        ItemStack totem = stack2.copy();
                        stacks.set(j, ItemStack.EMPTY);
                        boolean isEmpty = true;
                        for (int k = 0; k < stacks.size(); k++)
                        {
                            if (!stacks.get(k).isEmpty())
                            {
                                ItemStackHelper.saveAllItems(nbt, stacks, false);
                                isEmpty = false;
                                break;
                            }
                        }
                        if (isEmpty)
                            nbt.remove("Items");

                        return totem;
                    }
                }
            }
        }
        // Check inventory for totems in mod-added items with inventories
        IItemHandler itemInventory = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElse(null);
        if (itemInventory != null)
        {
            for (int j = 0; j < itemInventory.getSlots(); j++)
            {
                stack2 = itemInventory.getStackInSlot(j);
                if (isValidTotem(stack2, entity))
                {
                    ItemStack totem = stack2.copy();
                    stack2 = itemInventory.extractItem(j, 1, false);
                    if (!stack2.isEmpty())
                        return totem;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack getTotemFromShelf(LivingEntity entity)
    {
        AtomicReference<ItemStack> stackRef = new AtomicReference<>(ItemStack.EMPTY);
        boolean performAudit = entity.level.random.nextInt(20) == 0;
        TileEntityTotemShelf.visitTotemShelves(entity, (world, shelf) ->
        {
            if (stackRef.get().isEmpty())
            {
                ItemStackHandler inventory = shelf.getInventory();
                ItemStack stack;
                for (int i = 0; i < inventory.getSlots(); i++)
                {
                    stack = inventory.extractItem(i, 1, true);
                    if (stack.getItem() instanceof ItemBoundTotem && entity.getUUID().equals(NBTUtil.getBoundEntityId(stack)))
                    {
                        stackRef.set(inventory.extractItem(i, 1, false));
                        if (!performAudit)
                            return new TileEntityTotemShelf.ShelfVisitationResult(false, false);
                    }
                }
            }
            return new TileEntityTotemShelf.ShelfVisitationResult(false, true);
        });
        return stackRef.get();
    }
}