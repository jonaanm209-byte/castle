package com.paradoxcastle

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod

@Mod(ParadoxCastle.MOD_ID)
class ParadoxCastle(modBus: IEventBus) {

    companion object {
        const val MOD_ID = "paradoxcastle"
    }

    init {
        ModComponents.REGISTER.register(modBus)
        ModBlocks.REGISTER.register(modBus)
        ModItems.REGISTER.register(modBus)
        ModTabs.REGISTER.register(modBus)

        // A wild paradox Pokémon fainting in a battle charges the winners' keys.
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.NORMAL) { event ->
            KoTracker.onFaint(event)
        }
    }
}
