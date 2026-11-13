package dev.ketsuneban.perfectify.mixin;

import dev.ketsuneban.perfectify.client.ShearedSheepTextures;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SheepRenderer.class)
public abstract class SheepRendererMixin {
    @Inject(method = "getTextureLocation", at = @At("RETURN"), cancellable = true)
    private void studio$recolorAfterShearing(
            Sheep sheep, CallbackInfoReturnable<ResourceLocation> cir) {
        if (sheep.isSheared()) {
            cir.setReturnValue(ShearedSheepTextures.forColor(sheep.getColor()));
        }
    }
}
