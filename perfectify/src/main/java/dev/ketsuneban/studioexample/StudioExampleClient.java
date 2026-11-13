package dev.ketsuneban.studioexample;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side secret: press NUMPAD 9, NUMPAD 9, NUMPAD 3 in order.
 *
 * Each key must be pressed separately, within two seconds of the previous key.
 * Only GLFW_PRESS is recognized so holding NumPad 9 cannot fake two presses.
 * Inputs inside chat, inventory, and other screens are ignored.
 */
@EventBusSubscriber(modid = StudioExample.MOD_ID, value = Dist.CLIENT)
public final class StudioExampleClient {
    private static final long MAX_GAP_NANOS = 2_000_000_000L;
    private static final int[] SECRET_CODE = {
        GLFW.GLFW_KEY_KP_9,
        GLFW.GLFW_KEY_KP_9,
        GLFW.GLFW_KEY_KP_3
    };

    private static int progress = 0;
    private static long lastPressNanos = 0L;

    private StudioExampleClient() { }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        // Avoid auto-repeat and key-release double-counting.
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
            // A new NumPad 9 can always begin a fresh 993 sequence.
            progress = (key == SECRET_CODE[0]) ? 1 : 0;
            lastPressNanos = progress == 1 ? now : 0L;
        }
    }

    private static void showSecretTitle(Minecraft minecraft) {
        // Values are in ticks: 0.5s fade in, 3.5s visible, 1s fade out.
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
