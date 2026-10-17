package com.paradoxcastle

import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor
import com.cobblemon.mod.common.pokemon.Species
import com.mojang.logging.LogUtils
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

/**
 * Charges Koraidon/Miraidon keys when a wild paradox Pokémon is knocked out in a battle.
 * Nothing in this mod spawns paradox Pokémon; it only reacts to ones that already exist.
 */
object KoTracker {

    /** Normalised (lowercase alphanumerics) names of every paradox Pokémon. */
    private val PARADOX_NAMES = setOf(
        // Ancient
        "greattusk", "screamtail", "brutebonnet", "fluttermane", "slitherwing",
        "sandyshocks", "roaringmoon", "walkingwake", "gougingfire", "ragingbolt",
        // Future
        "irontreads", "ironbundle", "ironhands", "ironjugulis", "ironmoth",
        "ironthorns", "ironvaliant", "ironleaves", "ironboulder", "ironcrown"
    )

    fun isParadox(species: Species): Boolean {
        if (species.labels.any { it.equals("paradox", ignoreCase = true) }) return true
        return species.name.lowercase().filter { it.isLetterOrDigit() } in PARADOX_NAMES
    }

    private val LOGGER = LogUtils.getLogger()

    /** Never lets a problem here (e.g. an unusual actor from another mod) propagate into a battle. */
    fun onFaint(event: BattleFaintedEvent) {
        try {
            handleFaint(event)
        } catch (e: Exception) {
            LOGGER.error("Paradox Castle: failed to process a fainted Pokémon", e)
        }
    }

    private fun handleFaint(event: BattleFaintedEvent) {
        val killed = event.killed
        val victimActor = killed.actor

        // Only wild Pokémon count (not trainers' or other players' Pokémon).
        if (victimActor !is PokemonBattleActor) return
        if (!isParadox(killed.effectedPokemon.species)) return

        // Everyone on the opposing side of the fainted Pokémon gets credit.
        val winners = victimActor.getSide().getOppositeSide().actors
            .filterIsInstance<PlayerBattleActor>()
            .mapNotNull { it.entity }

        winners.forEach { chargeKeys(it) }
    }

    private fun chargeKeys(player: ServerPlayer) {
        val inventory = player.inventory
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            val item = stack.item
            if (item !is KeyItem || KeyCharge.isFull(stack)) continue

            val charge = KeyCharge.add(stack)
            val name = Component.translatable(item.legend.displayKey)
            if (charge >= KeyCharge.REQUIRED) {
                player.displayClientMessage(Component.translatable("message.paradoxcastle.key_full", name), true)
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f)
            } else {
                player.displayClientMessage(
                    Component.translatable("message.paradoxcastle.key_charged", name, charge, KeyCharge.REQUIRED), true
                )
                player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f)
            }
        }
    }
}
