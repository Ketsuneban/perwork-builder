package dev.ketsuneban.perfectify;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Perfectify.MOD_ID)
public final class SnowFeatures {
    public static final int MAX_SNOW_LAYERS = 5;
    private static int ticks;

    private SnowFeatures() {}

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (++ticks % 100 != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (!level.isRaining()) continue;
            for (var player : level.players()) {
                for (int trial = 0; trial < 3; trial++) {
                    if (level.random.nextInt(3) == 0) continue;
                    int x = player.getBlockX() + level.random.nextInt(41) - 20;
                    int z = player.getBlockZ() + level.random.nextInt(41) - 20;
                    BlockPos check = new BlockPos(x, player.getBlockY(), z);
                    if (!level.hasChunkAt(check)) continue;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos sky = new BlockPos(x, y, z);
                    if (!level.hasChunkAt(sky) || !level.canSeeSky(sky)) continue;
                    if (level.getBiome(sky).value().getPrecipitationAt(sky)
                            != Biome.Precipitation.SNOW) continue;
                    BlockPos surface = sky.below();
                    var old = level.getBlockState(surface);
                    if (old.is(Blocks.SNOW)) {
                        int layers = old.getValue(SnowLayerBlock.LAYERS);
                        if (layers < MAX_SNOW_LAYERS) {
                            level.setBlock(surface, old.setValue(SnowLayerBlock.LAYERS, layers + 1), 3);
                        }
                    } else {
                        var overlay = level.getBlockState(sky);

                        boolean soft = overlay.isAir() || overlay.is(Blocks.SHORT_GRASS)
                            || overlay.is(Blocks.FERN) || overlay.is(Blocks.DEAD_BUSH)
                            || overlay.is(Blocks.DANDELION) || overlay.is(Blocks.POPPY)
                            || overlay.is(Blocks.BLUE_ORCHID);
                        var layer = Blocks.SNOW.defaultBlockState();
                        if (soft && layer.canSurvive(level, sky)) {
                            level.setBlock(sky, layer, 3);
                        }
                    }
                }
            }
        }
    }
}
