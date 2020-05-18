package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.BoundTotems;
import com.github.phylogeny.boundtotems.init.ItemsMod;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import com.github.phylogeny.boundtotems.network.packet.PacketAddGhost;
import com.github.phylogeny.boundtotems.util.EntityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import com.google.common.collect.Multimap;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Items;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemRitualDagger extends Item
{
    public static final String NAME = "ritual_dagger";

    public ItemRitualDagger(Properties properties)
    {
        super(properties.maxStackSize(1));
        addPropertyOverride(new ResourceLocation("state"), (stack, world, entity) ->
                stack.hasTag() && stack.getTag().contains(NBTUtil.GLOWING) ? 3 : State.get(stack).ordinal());
    }

    public enum State
    {
        BASE, BLOODY, BOUND;

        private final String langKey;

        State()
        {
            langKey = String.join(".", "item", BoundTotems.MOD_ID, NAME, name().toLowerCase());
        }

        public String getLangKey()
        {
            return langKey;
        }

        public static State get(ItemStack stack)
        {
            if (!NBTUtil.hasBoundEntity(stack.getTag()))    
                return BASE;

            return NBTUtil.isKnifeBound(stack.getTag()) ? BOUND : BLOODY;
        }
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EquipmentSlotType slot, ItemStack stack)
    {
        Multimap<String, AttributeModifier> map = super.getAttributeModifiers(slot, stack);
        if (slot == EquipmentSlotType.MAINHAND && State.get(stack) == State.BOUND)
            map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 5, AttributeModifier.Operation.ADDITION));

        return map;
    }

    @Override
    public String getTranslationKey(ItemStack stack)
    {
        return State.get(stack).getLangKey();
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
        NBTUtil.addBoundEntityInformation(stack, tooltip);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
    {
        ActionResultType result = ActionResultType.PASS;
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking())
        {
            setBoundEntity(stack, player, player, true);
            result = ActionResultType.SUCCESS;
        }
        else if (NBTUtil.hasBoundEntity(stack))
        {
            ItemEntity entityTotem = EntityUtil.rayTraceEntities(world, player, ItemEntity.class, box -> box.expand(0, 0.25, 0));
            if (entityTotem != null && entityTotem.getItem().getItem() == Items.TOTEM_OF_UNDYING)
            {
                if (!world.isRemote)
                {
                    entityTotem.setItem(NBTUtil.copyBoundEntity(stack, new ItemStack(ItemsMod.BOUND_TOTEM.get())));
                    sendGhostPacket(player, entityTotem);
                }
                result = ActionResultType.SUCCESS;
                player.swingArm(hand);
            }
        }
        return new ActionResult<>(result, stack);
    }

    private boolean setBoundEntity(ItemStack stack, PlayerEntity player, Entity entity, boolean attackSelf)
    {
        if (!(player instanceof ServerPlayerEntity) || !(entity instanceof LivingEntity))
            return false;

        if (attackSelf)
        {
            player.attackTargetEntityWithCurrentItem(player);
            player.resetCooldown();
            return true;
        }
        return NBTUtil.setBoundEntity(stack, (LivingEntity) entity);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, PlayerEntity player, Entity entity)
    {
        if (setBoundEntity(stack, player, entity, false))
            sendGhostPacket(player, entity);

        return false;
    }

    private void sendGhostPacket(PlayerEntity player, Entity entity)
    {
        PacketNetwork.sendToAllTrackingAndSelf(new PacketAddGhost(entity, 0.1F, 40, null, player), entity);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context)
    {
        if (State.get(context.getItem()) == State.BLOODY && NBTUtil.hasBoundEntity(context.getItem()))
        {
            BlockState state = context.getWorld().getBlockState(context.getPos());
            if (state.getBlock() instanceof CauldronBlock)
            {
                int level = state.get(CauldronBlock.LEVEL);
                if (level > 0)
                {
                    ((CauldronBlock) state.getBlock()).setWaterLevel(context.getWorld(), context.getPos(), state, level - 1);
                    context.getItem().getOrCreateTag().remove(NBTUtil.BOUND_ENTITY);
                    context.getWorld().playSound(null, context.getPos(), SoundEvents.ENTITY_BOAT_PADDLE_WATER, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    return ActionResultType.SUCCESS;
                }
            }
        }
        return ActionResultType.PASS;
    }
}