package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.util.EntityUtil;
import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBoundTotemTeleporting extends ItemBoundTotem
{
    public ItemBoundTotemTeleporting(Properties properties)
    {
        super(properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand)
    {
        ItemStack stack = player.getHeldItem(hand);
        if (!world.isRemote)
        {
            LivingEntity entity = EntityUtil.rayTraceEntities(world, player, LivingEntity.class);
            NBTUtil.setBoundLocation(stack, entity != null ? entity : player);
        }
        return new ActionResult<>(ActionResultType.SUCCESS, stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected)
    {
        super.inventoryTick(stack, world, entity, itemSlot, isSelected);
        if (!world.isRemote && !NBTUtil.hasBoundLocation(stack.getTag()) && entity instanceof LivingEntity)
            NBTUtil.setBoundLocation(stack, (LivingEntity) entity);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
        super.addInformation(stack, world, tooltip, flag);
        NBTUtil.addBoundLocationInformation(stack, tooltip, flag == TooltipFlags.ADVANCED);
    }
}