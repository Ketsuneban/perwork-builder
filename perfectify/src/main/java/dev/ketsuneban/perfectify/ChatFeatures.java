package dev.ketsuneban.perfectify;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Perfectify.MOD_ID)
public final class ChatFeatures {
    private ChatFeatures() {}

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        List<String> names = new ArrayList<>();
        for (var player : event.getPlayer().server.getPlayerList().getPlayers()) {
            names.add(player.getGameProfile().getName());
        }
        names.sort(Comparator.comparingInt(String::length).reversed());
        event.setMessage(format(event.getRawText(), names));
    }

    @SubscribeEvent
    public static void tabList(PlayerEvent.TabListNameFormat event) {

        event.setDisplayName(Component.literal(event.getEntity().getGameProfile().getName())
            .withStyle(ChatFormatting.AQUA));
    }

    private static MutableComponent format(String input, List<String> playerNames) {
        MutableComponent output = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);
            if (character == '&' && i + 1 < input.length()) {
                ChatFormatting formatting = ChatFormatting.getByCode(input.charAt(i + 1));
                if (formatting != null) {
                    appendHighlighted(output, buffer.toString(), style, playerNames);
                    buffer.setLength(0);
                    if (formatting == ChatFormatting.RESET) style = Style.EMPTY;
                    else if (formatting.isColor()) style = Style.EMPTY.applyFormat(formatting);
                    else style = style.applyFormat(formatting);
                    i++;
                    continue;
                }
            }
            buffer.append(character);
        }
        appendHighlighted(output, buffer.toString(), style, playerNames);
        return output;
    }

    private static void appendHighlighted(MutableComponent result, String text,
                                           Style style, List<String> names) {
        if (text.isEmpty()) return;
        if (names.isEmpty()) {
            result.append(Component.literal(text).setStyle(style));
            return;
        }
        StringBuilder regex = new StringBuilder();
        for (String name : names) {
            if (name.isBlank()) continue;
            if (!regex.isEmpty()) regex.append('|');
            regex.append(Pattern.quote(name));
        }
        if (regex.isEmpty()) {
            result.append(Component.literal(text).setStyle(style));
            return;
        }
        var matcher = Pattern.compile("(?i)(?<![A-Za-z0-9_])(?:" + regex +
                                     ")(?![A-Za-z0-9_])").matcher(text);
        int offset = 0;
        while (matcher.find()) {
            if (matcher.start() > offset) {
                result.append(Component.literal(text.substring(offset, matcher.start())).setStyle(style));
            }
            result.append(Component.literal(matcher.group())
                .setStyle(style.withColor(ChatFormatting.AQUA)));
            offset = matcher.end();
        }
        if (offset < text.length()) {
            result.append(Component.literal(text.substring(offset)).setStyle(style));
        }
    }
}
