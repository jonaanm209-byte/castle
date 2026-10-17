package com.paradoxcastle

import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3

/** Spawns the legendary in front of the player. */
object Summoner {

    fun summon(level: ServerLevel, player: ServerPlayer, legend: Legend): Boolean {
        val entity = try {
            PokemonProperties.parse("${legend.species} level=${legend.level}").createEntity(level)
        } catch (e: Exception) {
            player.displayClientMessage(Component.translatable("message.paradoxcastle.summon_failed"), false)
            return false
        }

        val look = player.lookAngle
        val flat = Vec3(look.x, 0.0, look.z)
        val dir = if (flat.lengthSqr() < 1.0e-4) Vec3(0.0, 0.0, 1.0) else flat.normalize()

        // Prefer 5 blocks in front (these are big Pokémon), fall back to closer spots.
        val spot = listOf(5.0, 4.0, 3.0, 2.0)
            .firstNotNullOfOrNull { dist -> findStandingSpot(level, player.position().add(dir.scale(dist))) }
            ?: player.position().add(dir.scale(2.0))

        // Face the player.
        val yaw = Math.toDegrees(Math.atan2(dir.x, -dir.z)).toFloat()
        entity.moveTo(spot.x, spot.y, spot.z, yaw, 0.0f)
        entity.setPersistenceRequired() // don't despawn before the player can catch/fight it
        level.addFreshEntity(entity)

        // Flourish.
        EntityType.LIGHTNING_BOLT.create(level)?.let { bolt ->
            bolt.moveTo(spot.x, spot.y, spot.z)
            bolt.setVisualOnly(true)
            level.addFreshEntity(bolt)
        }
        level.playSound(null, BlockPos.containing(spot), SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 2.0f, 0.6f)
        player.sendSystemMessage(
            Component.translatable("message.paradoxcastle.summoned", Component.translatable(legend.displayKey))
        )
        return true
    }

    /** Finds a spot near [around] with solid ground and 3 blocks of headroom. */
    private fun findStandingSpot(level: ServerLevel, around: Vec3): Vec3? {
        val base = BlockPos.containing(around)
        for (dy in 3 downTo -3) {
            val feet = base.offset(0, dy, 0)
            val ground = feet.below()
            val solid = !level.getBlockState(ground).getCollisionShape(level, ground).isEmpty
            val clear = (0..2).all { h ->
                val p = feet.above(h)
                level.getBlockState(p).getCollisionShape(level, p).isEmpty && level.getFluidState(p).isEmpty
            }
            if (solid && clear) return Vec3(feet.x + 0.5, feet.y.toDouble(), feet.z + 0.5)
        }
        return null
    }
}
