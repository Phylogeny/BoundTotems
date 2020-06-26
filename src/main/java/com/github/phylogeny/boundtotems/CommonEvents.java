package com.github.phylogeny.boundtotems;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.block.PositionsTotemShelf;
import com.github.phylogeny.boundtotems.init.BlocksMod;
import com.github.phylogeny.boundtotems.item.ItemCarvingKnife;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTables;
import net.minecraft.world.storage.loot.TableLootEntry;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class CommonEvents
{
    private static final ResourceLocation LOOT_TABLE = BoundTotems.getResourceLoc("chests/mod_tools");
    private static final ResourceLocation LOOT_TABLE_MANSION = BoundTotems.getResourceLoc(LootTables.CHESTS_WOODLAND_MANSION.getPath());

    @SubscribeEvent
    public static void swapStrippedOakBlocks(PlayerInteractEvent.LeftClickBlock event)
    {
        if (event.getHand() != Hand.MAIN_HAND|| !(event.getItemStack().getItem() instanceof ItemCarvingKnife))
            return;

        BlockState state = event.getWorld().getBlockState(event.getPos());
        if (state.getBlock() != Blocks.STRIPPED_OAK_LOG)
            return;

        PositionsTotemShelf positions = BlockTotemShelf.getTotemShelfPositions(state, event.getWorld(), event.getPos());
        if (positions != null)
            event.getWorld().setBlockState(event.getPos(), BlocksMod.STRIPPED_OAK_LOG.get().getDefaultState(), 0);
    }

    @SubscribeEvent
    public static void modifyChestLootTables(LootTableLoadEvent event)
    {
        String path = event.getName().getPath();
        if (path.startsWith("chests"))
        {
            ResourceLocation table = path.equals(LOOT_TABLE_MANSION.getPath()) ? LOOT_TABLE_MANSION : LOOT_TABLE;
            event.getTable().addPool(LootPool.builder().addEntry(TableLootEntry.builder(table)).build());
        }
    }
}