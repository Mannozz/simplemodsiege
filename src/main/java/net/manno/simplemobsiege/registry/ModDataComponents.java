package net.manno.simplemobsiege.registry;

import net.manno.simplemobsiege.SimpleMobSiege;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, SimpleMobSiege.MODID);

    // Stores the resource location string of the entity type (e.g. "minecraft:zombie")
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MOB_TYPE =
            DATA_COMPONENT_TYPES.register("mob_type",
                    () -> DataComponentType.<String>builder()
                            .persistent(com.mojang.serialization.Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    // Stores the challenge ID or effects
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CHALLENGE_DATA =
            DATA_COMPONENT_TYPES.register("challenge_data",
                    () -> DataComponentType.<String>builder()
                            .persistent(com.mojang.serialization.Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}

