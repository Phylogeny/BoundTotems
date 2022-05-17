package com.github.phylogeny.boundtotems;

import com.github.phylogeny.boundtotems.client.ModPropertyGetters;
import com.github.phylogeny.boundtotems.init.*;
import com.github.phylogeny.boundtotems.network.PacketNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BoundTotems.MOD_ID)
@EventBusSubscriber(bus = Bus.MOD)
public class BoundTotems {
    public static final String MOD_ID = "boundtotems";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final NonNullSupplier<RuntimeException> EMPTY_OPTIONAL_EXP = () -> new RuntimeException("Optional cannot be empty.");

    public BoundTotems() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Config.register();
        BlocksMod.BLOCKS.register(bus);
        ItemsMod.ITEMS.register(bus);
        BlockEntitiesMod.BLOCK_ENTITIES.register(bus);
        RecipesMod.RECIPES.register(bus);
        SoundsMod.SOUNDS.register(bus);
        bus.addListener(this::clientSetup);
    }

    public static ResourceLocation getResourceLoc(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        PacketNetwork.registerPackets();
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        ModPropertyGetters.register();
    }
}