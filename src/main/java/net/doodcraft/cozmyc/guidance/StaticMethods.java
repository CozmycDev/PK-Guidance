package net.doodcraft.cozmyc.guidance;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StaticMethods {

    private static final Pattern colorPattern = Pattern.compile("#[a-fA-F0-9]{6}");

    public static void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline() || message == null) {
            return;
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(addColor(message)));
    }

    public static String addColor(String message) {
        Matcher matcher = colorPattern.matcher(message);

        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, net.md_5.bungee.api.ChatColor.of(color) + "");
            matcher = colorPattern.matcher(message);
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String parse(String message, Player player) {
        return message.replace("<player>", player.getName());
    }
}
