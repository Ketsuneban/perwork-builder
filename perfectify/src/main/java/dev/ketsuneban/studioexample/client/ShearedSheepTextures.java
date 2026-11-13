package dev.ketsuneban.studioexample.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * Produce recolored sheared-sheep skin from the player's installed Minecraft
 * texture at runtime. It does not redistribute Mojang's copyrighted texture.
 * Only neutral wool-like texels are tinted; face/skin colors stay intact.
 */
public final class ShearedSheepTextures {
    private static final ResourceLocation VANILLA =
            ResourceLocation.withDefaultNamespace("textures/entity/sheep/sheep.png");
    private static final Map<DyeColor, ResourceLocation> VARIANTS =
            new EnumMap<>(DyeColor.class);
    private static boolean unavailable;

    private ShearedSheepTextures() {}

    public static ResourceLocation forColor(DyeColor color) {
        if (color == DyeColor.WHITE || unavailable) return VANILLA;
        ResourceLocation cached = VARIANTS.get(color);
        if (cached != null) return cached;

        Minecraft mc = Minecraft.getInstance();
        try {
            var original = mc.getResourceManager().getResource(VANILLA);
            if (original.isEmpty()) return VANILLA;
            try (InputStream stream = original.get().open();
                 NativeImage image = NativeImage.read(stream)) {
                int target = color.getFireworkColor();
                int targetR = (target >> 16) & 255;
                int targetG = (target >> 8) & 255;
                int targetB = target & 255;
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int packed = image.getPixelRGBA(x, y);
                        int alpha = (packed >>> 24) & 255;
                        int red = packed & 255;
                        int green = (packed >> 8) & 255;
                        int blue = (packed >> 16) & 255;

                        // Restrict recoloring to desaturated, light remnants.
                        int range = Math.max(red, Math.max(green, blue))
                            - Math.min(red, Math.min(green, blue));
                        if (alpha > 10 && red > 180 && green > 180 && blue > 180
                            && range < 14) {
                            float brightness = (red + green + blue) / (3.0F * 255.0F);
                            int r = clamp(targetR * brightness);
                            int g = clamp(targetG * brightness);
                            int b = clamp(targetB * brightness);
                            image.setPixelRGBA(x, y, (alpha << 24) | (b << 16) | (g << 8) | r);
                        }
                    }
                }
                // DynamicTexture owns native image memory after registration.
                ResourceLocation key = mc.getTextureManager()
                    .register("studio_sheared_" + color.getName(),
                        new DynamicTexture(image.mappedCopy(px -> px)));
                VARIANTS.put(color, key);
                return key;
            }
        } catch (IOException | RuntimeException exception) {
            unavailable = true;
            return VANILLA;
        }
    }

    private static int clamp(float n) { return Math.max(0, Math.min(255, Math.round(n))); }
}
