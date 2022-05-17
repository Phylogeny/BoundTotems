package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBoundTotem extends Item {
    public ItemBoundTotem(Properties properties) {
        super(properties.rarity(Rarity.UNCOMMON).stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().append(" ").append(Items.TOTEM_OF_UNDYING.getName(stack));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int itemSlot, boolean isSelected) {
        if (!world.isClientSide && entity instanceof LivingEntity)
            NBTUtil.setBoundEntity(stack, (LivingEntity) entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        NBTUtil.addBoundEntityInformation(stack, tooltip);
    }
}