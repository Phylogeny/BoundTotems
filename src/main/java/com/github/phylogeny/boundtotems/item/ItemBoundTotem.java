package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Rarity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBoundTotem extends Item
{
    public ItemBoundTotem(Properties properties)
    {
        super(properties.rarity(Rarity.UNCOMMON).maxStackSize(1));
    }

//    @Override
//    public boolean hasContainerItem(ItemStack stack)
//    {
//        return true;
//    }
//
//    @Override
//    public ItemStack getContainerItem(ItemStack stack)
//    {
//        return stack.copy();
//    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack)
    {
        return super.getDisplayName(stack).deepCopy().appendString(" ").append(Items.TOTEM_OF_UNDYING.getDisplayName(stack));
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected)
    {
        if (!world.isRemote && entity instanceof LivingEntity)
            NBTUtil.setBoundEntity(stack, (LivingEntity) entity);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
        NBTUtil.addBoundEntityInformation(stack, tooltip);
    }
}