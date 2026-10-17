package com.paradoxcastle

import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.util.ExtraCodecs
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModComponents {
    val REGISTER: DeferredRegister<DataComponentType<*>> =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ParadoxCastle.MOD_ID)

    /** How many paradox KOs a key has absorbed. A key without this component has charge 0. */
    val CHARGE: DeferredHolder<DataComponentType<*>, DataComponentType<Int>> =
        REGISTER.register("charge", Supplier {
            DataComponentType.builder<Int>()
                .persistent(ExtraCodecs.NON_NEGATIVE_INT)
                .networkSynchronized(ByteBufCodecs.VAR_INT)
                .build()
        })
}

object ModBlocks {
    val REGISTER: DeferredRegister.Blocks = DeferredRegister.createBlocks(ParadoxCastle.MOD_ID)

    private fun pedestalProps(): BlockBehaviour.Properties =
        BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_BLACKSTONE)
            .noOcclusion()
            .strength(50.0f, 1200.0f) // obsidian-like: hard to destroy by accident
            .lightLevel { 8 }

    val KORAIDON_PEDESTAL: DeferredBlock<PedestalBlock> = REGISTER.registerBlock(
        "koraidon_pedestal", { p -> PedestalBlock(p, Legend.KORAIDON) }, pedestalProps()
    )

    val MIRAIDON_PEDESTAL: DeferredBlock<PedestalBlock> = REGISTER.registerBlock(
        "miraidon_pedestal", { p -> PedestalBlock(p, Legend.MIRAIDON) }, pedestalProps()
    )
}

object ModItems {
    val REGISTER: DeferredRegister.Items = DeferredRegister.createItems(ParadoxCastle.MOD_ID)

    private fun keyProps(): Item.Properties =
        Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()

    val KORAIDON_KEY: DeferredItem<KeyItem> = REGISTER.registerItem(
        "koraidon_key", { p -> KeyItem(p, Legend.KORAIDON) }, keyProps()
    )

    val MIRAIDON_KEY: DeferredItem<KeyItem> = REGISTER.registerItem(
        "miraidon_key", { p -> KeyItem(p, Legend.MIRAIDON) }, keyProps()
    )

    val KORAIDON_PEDESTAL = REGISTER.registerSimpleBlockItem(ModBlocks.KORAIDON_PEDESTAL)
    val MIRAIDON_PEDESTAL = REGISTER.registerSimpleBlockItem(ModBlocks.MIRAIDON_PEDESTAL)

    fun keyFor(legend: Legend): Item = when (legend) {
        Legend.KORAIDON -> KORAIDON_KEY.get()
        Legend.MIRAIDON -> MIRAIDON_KEY.get()
    }
}

object ModTabs {
    val REGISTER: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ParadoxCastle.MOD_ID)

    val MAIN: DeferredHolder<CreativeModeTab, CreativeModeTab> = REGISTER.register("main", Supplier {
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.${ParadoxCastle.MOD_ID}"))
            .icon { ItemStack(ModItems.KORAIDON_KEY.get()) }
            .displayItems { _, output ->
                output.accept(ModItems.KORAIDON_KEY.get())
                output.accept(ModItems.MIRAIDON_KEY.get())
                // Creative-only convenience: pre-charged keys for testing.
                for (key in listOf(ModItems.KORAIDON_KEY.get(), ModItems.MIRAIDON_KEY.get())) {
                    val full = ItemStack(key)
                    KeyCharge.set(full, KeyCharge.REQUIRED)
                    output.accept(full)
                }
                output.accept(ModItems.KORAIDON_PEDESTAL.get())
                output.accept(ModItems.MIRAIDON_PEDESTAL.get())
            }
            .build()
    })
}
