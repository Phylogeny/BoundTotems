package com.github.phylogeny.boundtotems.item;

import com.github.phylogeny.boundtotems.util.NBTUtil;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ResourceLocation;

public class ItemTotemShelf extends BlockItem
{
    public ItemTotemShelf(Block block, Properties properties)
    {
        super(block, properties);
        addPropertyOverride(new ResourceLocation("type"), (stack, world, entity) -> stack.hasTag() && stack.getTag().contains(NBTUtil.GLOWING) ? 1 : 0);
    }
}