package com.paradoxcastle

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.function.Supplier

/**
 * A pedestal bound to one [Legend]. Right-click with a fully charged matching key to
 * consume it and summon the legendary in front of you.
 */
class PedestalBlock(properties: Properties, val legend: Legend) : Block(properties) {

    // Required by Block in 1.21; only used for data-driven (de)serialisation, which pedestals don't need.
    override fun codec(): MapCodec<out Block> =
        MapCodec.unit(Supplier { PedestalBlock(BlockBehaviour.Properties.of(), legend) })

    override fun getShape(
        state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext
    ): VoxelShape = SHAPE

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): ItemInteractionResult {
        val item = stack.item
        if (item !is KeyItem) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        if (level.isClientSide) return ItemInteractionResult.SUCCESS

        if (item.legend != legend) {
            player.displayClientMessage(
                Component.translatable("message.paradoxcastle.wrong_key", Component.translatable(legend.displayKey)), true
            )
            return ItemInteractionResult.CONSUME
        }
        if (!KeyCharge.isFull(stack)) {
            player.displayClientMessage(
                Component.translatable("message.paradoxcastle.not_charged", KeyCharge.get(stack), KeyCharge.REQUIRED), true
            )
            return ItemInteractionResult.CONSUME
        }

        if (Summoner.summon(level as ServerLevel, player as ServerPlayer, legend)) {
            if (!player.abilities.instabuild) stack.shrink(1)
        }
        return ItemInteractionResult.CONSUME
    }

    // Empty hand / non-key item: tell the player what the pedestal wants.
    override fun useWithoutItem(
        state: BlockState, level: Level, pos: BlockPos, player: Player, hit: BlockHitResult
    ): InteractionResult {
        if (!level.isClientSide) {
            player.displayClientMessage(
                Component.translatable("message.paradoxcastle.needs_key", Component.translatable(legend.displayKey)), true
            )
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    companion object {
        private val SHAPE: VoxelShape = Shapes.or(
            box(1.0, 0.0, 1.0, 15.0, 3.0, 15.0),
            box(4.0, 3.0, 4.0, 12.0, 11.0, 12.0),
            box(2.0, 11.0, 2.0, 14.0, 14.0, 14.0)
        )
    }
}
