package com.github.phylogeny.boundtotems.init;

import com.github.phylogeny.boundtotems.BoundTotems;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class SoundsMod
{
    public static final DeferredRegister<SoundEvent> SOUNDS = new DeferredRegister<>(ForgeRegistries.SOUND_EVENTS, BoundTotems.MOD_ID);

    public static final RegistryObject<SoundEvent> EXHALE = register("exhale");
    public static final RegistryObject<SoundEvent> BIND_SHELF = register("bind_shelf");

    private static RegistryObject<SoundEvent> register(String name)
    {
        return SOUNDS.register(name, () -> new SoundEvent(BoundTotems.getResourceLoc(name)));
    }
}