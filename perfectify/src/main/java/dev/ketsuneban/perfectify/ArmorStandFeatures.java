package dev.ketsuneban.perfectify;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Rotations;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Perfectify.MOD_ID)
public final class ArmorStandFeatures {
    private static final String POSE_KEY = "perfectifyPose";
    private static final Map<ArmorStand, Integer> LAST_POWER = new WeakHashMap<>();
    private static final Rotations[][] POSES = {
        {
            new Rotations(0F, 0F, 0F),
            new Rotations(0F, 0F, 0F),
            new Rotations(-10F, 0F, -10F),
            new Rotations(-15F, 0F, 10F),
            new Rotations(-1F, 0F, -1F),
            new Rotations(1F, 0F, 1F)
        },
        {
            new Rotations(0F, 0F, 0F),
            new Rotations(0F, 0F, 0F),
            new Rotations(0F, 0F, 0F),
            new Rotations(0F, 0.01F, 0.01F),
            new Rotations(0F, -0.1F, -0.01F),
            new Rotations(0F, 0F, 0F)
        },
        {
            new Rotations(15F, 0F, 0F),
            new Rotations(0F, 0F, 2F),
            new Rotations(-30F, 15F, 15F),
            new Rotations(-60F, -20F, -10F),
            new Rotations(-1F, 0F, -1F),
            new Rotations(1F, 0F, 1F)
        },
        {
            new Rotations(-5F, 0F, 0F),
            new Rotations(0F, 0F, 2F),
            new Rotations(10F, 0F, -5F),
            new Rotations(-60F, 20F, -10F),
            new Rotations(-3F, -3F, -3F),
            new Rotations(3F, 3F, 3F)
        },
        {
            new Rotations(-15F, 0F, 0F),
            new Rotations(0F, 0F, -2F),
            new Rotations(20F, 0F, -10F),
            new Rotations(-110F, 50F, 0F),
            new Rotations(5F, -3F, -3F),
            new Rotations(-5F, 3F, 3F)
        },
        {
            new Rotations(-15F, 0F, 0F),
            new Rotations(0F, 0F, 0F),
            new Rotations(-110F, 35F, 0F),
            new Rotations(-110F, -35F, 0F),
            new Rotations(5F, -3F, -3F),
            new Rotations(-5F, 3F, 3F)
        },
        {
            new Rotations(-15F, 0F, 0F),
            new Rotations(0F, 0F, 0F),
            new Rotations(-110F, -35F, 0F),
            new Rotations(-110F, 35F, 0F),
            new Rotations(5F, -3F, -3F),
            new Rotations(-5F, 3F, 3F)
        },
        {
            new Rotations(0F, 0F, 0F),
            new Rotations(0F, 0F, 0F),
            new Rotations(10F, 0F, -5F),
            new Rotations(-70F, -40F, 0F),
            new Rotations(-1F, 0F, -1F),
            new Rotations(1F, 0F, 1F)
        },
        {
            new Rotations(16F, 20F, 0F),
            new Rotations(0F, 0F, 0F),
            new Rotations(4F, 8F, 237F),
            new Rotations(246F, 0F, 89F),
            new Rotations(-14F, -18F, -16F),
            new Rotations(8F, 20F, 4F)
        },
        {
            new Rotations(-10F, 0F, -5F),
            new Rotations(0F, 0F, 0F),
            new Rotations(-105F, 0F, 0F),
            new Rotations(-100F, 0F, 0F),
            new Rotations(7F, 0F, 0F),
            new Rotations(-46F, 0F, 0F)
        },
        {
            new Rotations(-5F, 18F, 0F),
            new Rotations(0F, 22F, 0F),
            new Rotations(8F, 0F, -114F),
            new Rotations(0F, 84F, 111F),
            new Rotations(-111F, 55F, 0F),
            new Rotations(0F, 23F, -13F)
        },
        {
            new Rotations(-10F, -20F, 0F),
            new Rotations(0F, -18F, 0F),
            new Rotations(0F, 0F, -112F),
            new Rotations(8F, 90F, 111F),
            new Rotations(0F, 0F, 13F),
            new Rotations(-119F, -42F, 0F)
        },
        {
            new Rotations(-4F, 67F, 0F),
            new Rotations(0F, 8F, 0F),
            new Rotations(16F, 32F, -8F),
            new Rotations(-99F, 63F, 0F),
            new Rotations(0F, -75F, -8F),
            new Rotations(4F, 63F, 8F)
        }
    };

    private ArmorStandFeatures() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ArmorStand stand) {
            stand.setShowArms(true);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ArmorStand stand) || stand.level().isClientSide()) return;
        int power = Math.max(stand.level().getBestNeighborSignal(stand.blockPosition()),
            stand.level().getBestNeighborSignal(stand.blockPosition().below()));
        Integer previous = LAST_POWER.put(stand, power);
        if (previous != null && previous == power) return;
        if (previous == null && power == 0) return;
        setPose(stand, Math.min(12, Math.max(0, power - 1)));
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof ArmorStand stand)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND || !event.getEntity().isShiftKeyDown()) return;

        if (!event.getLevel().isClientSide()) {
            ItemStack held = event.getEntity().getMainHandItem().copy();
            if (held.isEmpty()) {
                int next = (stand.getPersistentData().getInt(POSE_KEY) + 1) % POSES.length;
                setPose(stand, next);
            } else {
                ItemStack stored = stand.getItemBySlot(EquipmentSlot.OFFHAND).copy();
                stand.setShowArms(true);
                stand.setItemSlot(EquipmentSlot.OFFHAND, held);
                event.getEntity().setItemInHand(InteractionHand.MAIN_HAND, stored);
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static void setPose(ArmorStand stand, int poseIndex) {
        int index = Math.max(0, Math.min(POSES.length - 1, poseIndex));
        Rotations[] p = POSES[index];
        stand.setHeadPose(p[0]);
        stand.setBodyPose(p[1]);
        stand.setLeftArmPose(p[2]);
        stand.setRightArmPose(p[3]);
        stand.setLeftLegPose(p[4]);
        stand.setRightLegPose(p[5]);
        stand.setShowArms(true);
        stand.getPersistentData().putInt(POSE_KEY, index);
    }
}
