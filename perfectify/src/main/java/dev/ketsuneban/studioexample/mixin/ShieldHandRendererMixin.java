package dev.ketsuneban.studioexample.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Remove the equipment-interpolation dip when starting a shield block.
 * This only alters the visual animation, never shield hit validation.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ShieldHandRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"),
                    argsOnly = true, ordinal = 3)
    private float studio$instantShieldEquip(float equipProgress) {
        if (minecraft.player != null && minecraft.player.isUsingItem()
                && minecraft.player.getUseItem().is(Items.SHIELD)) {
            return 0.0F;
        }
        return equipProgress;
    }
}
