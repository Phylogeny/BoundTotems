package com.github.phylogeny.boundtotems;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.List;

public class Config
{
    private static final String LANG_KEY = "config." + BoundTotems.MOD_ID;

    public static class Client extends ConfigBase
    {
        private static final String LANG_KEY = Config.LANG_KEY + ".client.";

        Client()
        {
            super("Client", "Contains configs only accessed by the client", ModConfig.Type.CLIENT);
            build();
        }
    }

    public static class Server extends ConfigBase
    {
        private static final String LANG_KEY = Config.LANG_KEY + ".server.";
        public final ConfigValue<InventorySearch> inventorySearch;
        public final ConfigValue<List<? extends String>> potionEffects;
        public final DoubleValue health, maxDistanceToShelf;
        public final BooleanValue preventCreativeModeDeath, clearPotionEffects, spawnParticles, playSound, playAnimation, setHealthToPercentageOfMax;
        public final IntValue maxBoundShelves;

        public enum InventorySearch
        {
            WHOLE_INVENTORY, HOTBAR_ONLY, HELD_ONLY
        }

        Server()
        {
            super("Server", "Contains configs only accessed by the server", ModConfig.Type.SERVER);

            inventorySearch = builder
                    .comment("1) If set to WHOLE_INVENTORY, the entire inventory will be searched for totems; 2) If set to HOTBAR_ONLY, only the hotbar of the "
                            + "inventory will be searched for totems; 3) If set to HELD_ONLY, only totems that the player is holding will save them from death.")
                    .translation(LANG_KEY + "search")
                    .defineEnum("Inventory Search Location", InventorySearch.WHOLE_INVENTORY);

            preventCreativeModeDeath = builder
                    .comment("If set to true, non-held bound totems of undying can save the player from death even " +
                            "from damage sources that harm players in creative mode (such as void damage and /kill command damage).")
                    .translation(LANG_KEY + "prevent.death.all")
                    .define("Prevent All Death", true);

            clearPotionEffects = builder
                     .comment(format("If set to true, all pre-existing potion effects will be cleared from the player %s."))
                     .translation(LANG_KEY + "effects.clear")
                     .define("Clear Potion Effects", true);

            potionEffects = builder
                     .comment(format("These potion effects will be applied to the player %s. Each string specifies "
                             + "a potion effect in exactly the same way the /effect command does (e.i. the required "
                             + "first argument specifies a potion by id or by modId:name, and the second/third "
                             + "optional arguments are duration in seconds and amplification). These effects will "
                             + "be applied whether or not pre-existing effects were previously cleared."))
                     .translation(LANG_KEY + "effects.apply")
                     .defineList("Apply Potion Effects", ImmutableList.of("minecraft:regeneration 45 1", "minecraft:absorption 5 1"), obj -> obj instanceof String);

            spawnParticles = builder
                     .comment(format("If set to true, totem particles will spawn for all nearby players %s."))
                     .translation(LANG_KEY + "result.particles")
                     .define("Spawn Particles", true);

            playSound = builder
                     .comment(format("If set to true, totem sound will play for all nearby players %s."))
                     .translation(LANG_KEY + "result.sound")
                     .define("Play Sound", true);

            playAnimation = builder
                     .comment(format("If set to true, the large floating totem animation will that takes up the screen will play upon %s."))
                     .translation(LANG_KEY + "result.animation")
                     .define("Play Animation", false);

            setHealthToPercentageOfMax = builder
                     .comment(format("If set to true, 'New Health Value' will specify the percent of the player's max heath to set the new health "
                             + "value to %s. If set to false, it will specify the number of hearts (1 = 1/2 heart) to set the new health value to."))
                     .translation(LANG_KEY + "health.percentage")
                     .define("Set Health To Percentage Of Max", false);

            health = builder
                    .comment(format("Specifies the value (either percentage of max health, or absolute value (1 = 1/2 heart), depending "
                            + "on what 'Set Health To Percentage Of Max' is set to) that the players heath will be set to %s."))
                    .translation(LANG_KEY + "health.value")
                    .defineInRange("New Health Value", 1, 0, Double.MAX_VALUE);

            maxDistanceToShelf = builder
                    .comment(format("Specifies the maximum distance in meters from a totem shelf an entity must be when binding that " +
                            "shelf to that entity for the binding to be successful."))
                    .translation(LANG_KEY + "shelf.distance")
                    .defineInRange("Max Distance To Shelf", 10, 0, Double.MAX_VALUE);

            maxBoundShelves = builder
                    .comment(format("Specifies the maximum number of shelves that can be bound to an entity at a time. If an additional shelf " +
                            "is bound once the max has been reached, a randomly selected currently bound shelf will be struck with lightning and converted " +
                            "into a useless charred shelf. Items can be taken from a charred shelf but cannot be placed in it, and any totems it will " +
                            "have no effect as long as they remain in it."))
                    .translation(LANG_KEY + "shelf.max")
                    .defineInRange("Max Bound Shelves", 10, 1, Integer.MAX_VALUE);

            build();
        }
    }

    private static String format(String comment)
    {
        return String.format(comment, "upon being saved from death by a non-held bound totem of undying");
    }

    private static class ConfigBase
    {
        protected final Builder builder = new ForgeConfigSpec.Builder();
        private final ModConfig.Type type;
        private ForgeConfigSpec spec;

        public ConfigBase(String category, String description, ModConfig.Type type)
        {
            builder.comment(description).push(category);
            this.type = type;
        }

        protected void build()
        {
            builder.pop();
            spec = builder.build();
        }

        public void register()
        {
            ModLoadingContext.get().registerConfig(type, spec);
        }
    }

    public static final Server SERVER = new Server();

    @SubscribeEvent
    public void onLoad(final ModConfig.Loading configEvent)
    {
        BoundTotems.LOGGER.debug("Loaded forge config file {}", configEvent.getConfig().getFileName());
    }

    @SubscribeEvent
    public void onFileChange(final ModConfig.Reloading configEvent)
    {
        BoundTotems.LOGGER.fatal("Forge config just got changed on the file system!");
    }

    public static void register()
    {
        SERVER.register();
        FMLJavaModLoadingContext.get().getModEventBus().register(Config.class);
    }
}