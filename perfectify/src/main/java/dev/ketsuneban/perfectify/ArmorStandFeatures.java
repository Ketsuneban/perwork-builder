package dev.ketsuneban.perfectify;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Rotations;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Perfectify.MOD_ID)
public final class ArmorStandFeatures {
    private static final Map<ArmorStand, Boolean> WAS_POWERED = new WeakHashMap<>();
    private static int poseTick;

    private ArmorStandFeatures() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ArmorStand stand) {
            stand.setShowArms(true);
        }
    }

    @SubscribeEvent
    public static void offhand(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ArmorStand stand)) return;
        if (!event.getEntity().isShiftKeyDown() ||
                event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;

        if (!event.getLevel().isClientSide()) {
            ItemStack held = event.getEntity().getMainHandItem().copy();
            ItemStack equipped = stand.getItemBySlot(EquipmentSlot.OFFHAND).copy();
            stand.setShowArms(true);
            stand.setItemSlot(EquipmentSlot.OFFHAND, held);
            event.getEntity().setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, equipped);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
        if (++poseTick % 10 != 0) return;
        for (var level : event.getServer().getAllLevels()) {
            for (var player : level.players()) {
                for (var stand : level.getEntitiesOfClass(ArmorStand.class,
                        player.getBoundingBox().inflate(24.0))) {
                    boolean power = level.hasNeighborSignal(stand.blockPosition()) ||
                            level.hasNeighborSignal(stand.blockPosition().below());
                    boolean previous = WAS_POWERED.getOrDefault(stand, false);
                    if (power && !previous) changePose(stand);
                    WAS_POWERED.put(stand, power);
                }
            }
        }
    }

    private static void changePose(ArmorStand stand) {
        int p = stand.getRandom().nextInt(8);

        float head = switch (p) { case 1, 5 -> -14F; case 3, 7 -> 14F; default -> 0F; };
        stand.setHeadPose(new Rotations(head, p % 2 == 0 ? 0 : 15, 0));
        stand.setBodyPose(new Rotations(0, 0, p % 3 == 0 ? 6 : 0));
        stand.setRightArmPose(new Rotations(-20 - p * 8, 0, 5 + p * 3));
        stand.setLeftArmPose(new Rotations(-20 - (7 - p) * 8, 0, -5 - p * 3));
        stand.setRightLegPose(new Rotations(p % 2 == 0 ? 0 : 12, 0, 0));
        stand.setLeftLegPose(new Rotations(p % 2 == 0 ? 0 : -12, 0, 0));
    }
}
