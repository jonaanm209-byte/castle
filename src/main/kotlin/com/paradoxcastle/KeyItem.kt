package com.paradoxcastle

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag

/** Helpers for reading and writing a key's charge. A key with no component has charge 0. */
object KeyCharge {
    /** Paradox KOs needed to fully charge a key. Change here to tune difficulty. */
    const val REQUIRED = 10

    fun get(stack: ItemStack): Int =
        stack.getOrDefault(ModComponents.CHARGE.get(), 0).coerceIn(0, REQUIRED)

    fun set(stack: ItemStack, value: Int) {
        stack.set(ModComponents.CHARGE.get(), value.coerceIn(0, REQUIRED))
    }

    fun isFull(stack: ItemStack): Boolean = get(stack) >= REQUIRED

    /** Adds [amount] charge and returns the new value. */
    fun add(stack: ItemStack, amount: Int = 1): Int {
        val value = (get(stack) + amount).coerceAtMost(REQUIRED)
        set(stack, value)
        return value
    }
}

/**
 * Koraidon / Miraidon key. Spawns with zero charge; the charge bar fills as the holder KOs
 * wild paradox Pokémon. The bar is drawn by overriding the durability-bar hooks, so the key
 * can never "break" the way a damageable item would.
 */
class KeyItem(properties: Properties, val legend: Legend) : Item(properties) {

    override fun isBarVisible(stack: ItemStack): Boolean = true

    override fun getBarWidth(stack: ItemStack): Int =
        Math.round(13.0f * KeyCharge.get(stack) / KeyCharge.REQUIRED)

    override fun getBarColor(stack: ItemStack): Int = legend.barColor

    override fun isFoil(stack: ItemStack): Boolean = KeyCharge.isFull(stack)

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltip: MutableList<Component>,
        flag: TooltipFlag
    ) {
        val charge = KeyCharge.get(stack)
        val colour = if (KeyCharge.isFull(stack)) ChatFormatting.GREEN else ChatFormatting.GRAY
        tooltip.add(
            Component.translatable("tooltip.paradoxcastle.charge", charge, KeyCharge.REQUIRED).withStyle(colour)
        )
        tooltip.add(
            Component.translatable(
                if (KeyCharge.isFull(stack)) "tooltip.paradoxcastle.ready" else "tooltip.paradoxcastle.how_to_charge"
            ).withStyle(ChatFormatting.DARK_GRAY)
        )
    }
}
