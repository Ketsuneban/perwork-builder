package dev.ketsuneban.perfectify.mixin;

import net.minecraft.world.level.material.MapColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MapColor.class)
public abstract class MapPaletteMixin {
    @Inject(method = "getColorFromPackedId", at = @At("RETURN"), cancellable = true)
    private static void studio$softenMap(int packed, CallbackInfoReturnable<Integer> cir) {
        int color = cir.getReturnValue();
        int alpha = color >>> 24;
        if (alpha < 32) return;
        int red = color & 255;
        int green = (color >>> 8) & 255;
        int blue = (color >>> 16) & 255;
        float gray = (red * 0.23F + green * 0.66F + blue * 0.11F);
        red = clamp(gray * 0.13F + red * 0.87F + 5.5F);
        green = clamp(gray * 0.13F + green * 0.87F + 5.0F);
        blue = clamp(gray * 0.13F + blue * 0.87F + 6.5F);
        cir.setReturnValue(alpha << 24 | blue << 16 | green << 8 | red);
    }
    private static int clamp(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }
}
