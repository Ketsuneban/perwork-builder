package dev.ketsuneban.perfectify;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Perfectify.MOD_ID, value = Dist.CLIENT)
public final class PerfectifyClient {
    private static final long MAX_GAP_NANOS = 2_000_000_000L;
    private static final int[] SECRET_CODE = {
        GLFW.GLFW_KEY_KP_9,
        GLFW.GLFW_KEY_KP_9,
        GLFW.GLFW_KEY_KP_3
    };

    private static int progress = 0;
    private static long lastPressNanos = 0L;

    private PerfectifyClient() { }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {

        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            progress = 0;
            return;
        }

        long now = System.nanoTime();
        if (progress > 0 && now - lastPressNanos > MAX_GAP_NANOS) {
            progress = 0;
        }

        int key = event.getKey();
        if (key == SECRET_CODE[progress]) {
            progress++;
            lastPressNanos = now;
            if (progress == SECRET_CODE.length) {
                progress = 0;
                showSecretTitle(minecraft);
            }
        } else {

            progress = (key == SECRET_CODE[0]) ? 1 : 0;
            lastPressNanos = progress == 1 ? now : 0L;
        }
    }

    private static void showSecretTitle(Minecraft minecraft) {

        minecraft.gui.setTimes(10, 70, 20);
        minecraft.gui.setSubtitle(
            Component.literal("NUMPAD 993 ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal("• EVENT RECEIVED")
                    .withStyle(ChatFormatting.GRAY))
        );
        minecraft.gui.setTitle(
            Component.literal("SYSTEM EVENT")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
        );
    }
}
