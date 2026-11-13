package dev.ketsuneban.perfectify;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Perfectify.MOD_ID)
public final class SmartMobsAndGolems {
    private static int ticks;

    private SmartMobsAndGolems() {}

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        ticks++;
        if (ticks % 40 == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                for (var player : level.players()) {
                    int budget = 6;
                    for (Monster monster : level.getEntitiesOfClass(
                            Monster.class, player.getBoundingBox().inflate(26))) {
                        if (budget-- <= 0) break;
                        LivingEntity target = monster.getTarget();
                        if (target == null || !target.isAlive() || !monster.onGround()) continue;
                        if (!monster.getSensing().hasLineOfSight(target)) continue;
                        if (!monster.getNavigation().isDone()) continue;
                        double dist2 = monster.distanceToSqr(target);
                        if (dist2 > 400 || dist2 < 4) continue;

                        double dx = target.getX() - monster.getX();
                        double dz = target.getZ() - monster.getZ();
                        double len = Math.sqrt(dx * dx + dz * dz);
                        if (len < 0.1) continue;

                        double side = level.random.nextBoolean() ? 1 : -1;
                        double strafeX = -dz / len * side;
                        double strafeZ = dx / len * side;
                        if (monster instanceof Skeleton) {
                            double retreat = dist2 < 49 ? -4.0 : 2.0;
                            monster.getNavigation().moveTo(
                                monster.getX() + strafeX * 3 + dx / len * retreat,
                                target.getY(),
                                monster.getZ() + strafeZ * 3 + dz / len * retreat,
                                1.15);
                        } else if (monster instanceof Zombie) {

                            if (dist2 > 16) monster.getNavigation().moveTo(
                                target.getX() + strafeX * 2.2,
                                target.getY(),
                                target.getZ() + strafeZ * 2.2,
                                1.1);
                        } else if (monster instanceof Creeper) {

                            if (dist2 > 36) monster.getNavigation().moveTo(
                                target.getX() + strafeX * 1.7,
                                target.getY(),
                                target.getZ() + strafeZ * 1.7,
                                1.0);
                        }
                    }
                }
            }
        }

        if (ticks % 200 == 0) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                for (var player : level.players()) {
                    for (IronGolem golem : level.getEntitiesOfClass(
                            IronGolem.class, player.getBoundingBox().inflate(20))) {
                        if (golem.getTarget() != null || level.random.nextInt(6) != 0) continue;
                        List<Villager> children = level.getEntitiesOfClass(Villager.class,
                            golem.getBoundingBox().inflate(6), Villager::isBaby);
                        if (!children.isEmpty()) {
                            golem.offerFlower(true);
                        }
                    }
                }
            }
        }
    }
}
