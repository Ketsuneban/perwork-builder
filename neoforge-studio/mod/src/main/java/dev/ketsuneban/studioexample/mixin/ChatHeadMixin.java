package dev.ketsuneban.studioexample.mixin;

import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Display 8x8 skin portraits next to visible player chat lines.
 * The chat renderer is shifted right by 11 pixels to leave room for avatars.
 */
@Mixin(ChatComponent.class)
public abstract class ChatHeadMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;

    @Inject(method = "render", at = @At("HEAD"))
    private void studio$startChatIcons(GuiGraphics g, int tick, int mouseX,
                                       int mouseY, boolean focused, CallbackInfo ci) {
        g.pose().pushPose();
        g.pose().translate(11.0F, 0, 0);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void studio$drawChatIcons(GuiGraphics g, int tick, int mouseX,
                                      int mouseY, boolean focused, CallbackInfo ci) {
        g.pose().popPose();
        if (minecraft.getConnection() == null) return;
        double scale = ((ChatComponent)(Object)this).getScale();
        int lines = Math.min(((ChatComponent)(Object)this).getLinesPerPage(),
                             trimmedMessages.size() - chatScrollbarPos);
        if (lines <= 0) return;

        for (int i = 0; i < lines; i++) {
            GuiMessage.Line line = trimmedMessages.get(i + chatScrollbarPos);
            if (!focused && tick - line.addedTime() > 200) continue;
            StringBuilder plain = new StringBuilder();
            line.content().accept((index, style, codepoint) -> {
                plain.appendCodePoint(codepoint);
                return true;
            });
            String s = plain.toString();

            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                String player = info.getProfile().getName();
                if (s.matches("(?s)^.{0,32}[<\\\\[]?" + Pattern.quote(player)
                       + "[>\\\\]]?(?::|\\\\s).*$")) {
                    int y = g.guiHeight() - 39
                        - (int)Math.round((i + 1) * 9 * scale);
                    g.fill(2, y - 1, 13, y + 10, 0xAA101A2A);
                    g.blit(info.getSkin().texture(), 3, y, 8, 8, 8, 8, 64, 64);
                    break;
                }
            }
        }
    }
}
