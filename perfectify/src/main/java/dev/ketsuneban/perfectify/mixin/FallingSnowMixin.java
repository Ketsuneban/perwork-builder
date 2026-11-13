package dev.ketsuneban.perfectify.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowLayerBlock.class)
public abstract class FallingSnowMixin {
    @Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
    private void studio$onUnsupportedSnow(
            BlockState state, Direction direction, BlockState neighbor,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos,
            CallbackInfoReturnable<BlockState> callback) {
        if (direction != Direction.DOWN || state.canSurvive(level, pos)
                || !(level instanceof ServerLevel server)) return;

        FallingBlockEntity.fall(server, pos, state);
        callback.setReturnValue(Blocks.AIR.defaultBlockState());
    }
}
