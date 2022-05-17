package com.github.phylogeny.boundtotems;

import com.github.phylogeny.boundtotems.block.BlockTotemShelf;
import com.github.phylogeny.boundtotems.block.PositionsTotemShelf;
import com.github.phylogeny.boundtotems.command.LocateCommand;
import com.github.phylogeny.boundtotems.init.BlocksMod;
import com.github.phylogeny.boundtotems.item.ItemCarvingKnife;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class CommonEvents {
    private static final ResourceLocation LOOT_TABLE = BoundTotems.getResourceLoc("chests/mod_tools");
    private static final ResourceLocation LOOT_TABLE_MANSION = BoundTotems.getResourceLoc(BuiltInLootTables.WOODLAND_MANSION.getPath());

    @SubscribeEvent
    public static void swapStrippedOakBlocks(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getItemStack().getItem() instanceof ItemCarvingKnife))
            return;

        BlockState state = event.getWorld().getBlockState(event.getPos());
        if (state.getBlock() != Blocks.STRIPPED_OAK_LOG)
            return;

        PositionsTotemShelf positions = BlockTotemShelf.getTotemShelfPositions(state, event.getWorld(), event.getPos());
        if (positions != null)
            event.getWorld().setBlock(event.getPos(), BlocksMod.STRIPPED_OAK_LOG.get().defaultBlockState(), 0);
    }

    @SubscribeEvent
    public static void modifyChestLootTables(LootTableLoadEvent event) {
        String path = event.getName().getPath();
        if (path.startsWith("chests")) {
            ResourceLocation table = path.equals(LOOT_TABLE_MANSION.getPath()) ? LOOT_TABLE_MANSION : LOOT_TABLE;
            event.getTable().addPool(LootPool.lootPool().name(path + "_" + BoundTotems.MOD_ID).add(LootTableReference.lootTableReference(table)).build());
        }
    }

    @SubscribeEvent
    public static void onServerStart(RegisterCommandsEvent event) {
        LocateCommand.register(event.getDispatcher());
    }
}