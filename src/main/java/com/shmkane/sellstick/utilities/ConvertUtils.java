package com.shmkane.sellstick.utilities;

import com.shmkane.sellstick.SellStick;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConvertUtils {

    public static void convertSellStick (Player player) {
        if (player.getInventory().getItemInMainHand().isEmpty() || player.getInventory().getItemInMainHand().getType().isAir()) {
            return;
        }

        ItemStack sellstick = player.getInventory().getItemInMainHand();
        String name = sellstick.getItemMeta().getDisplayName();
        if (!name.startsWith("§e✦ §e§lSellStick") && !name.startsWith("§6§lSellStick")) {
            ChatUtils.sendMsg(player, "<red>This is not an old SellStick! ", true);
            Bukkit.getLogger().info(sellstick.getItemMeta().getDisplayName());
            return;
        }

        if (sellstick.getAmount() != 1) {
            ChatUtils.sendMsg(player, "<red>You cannot convert more than one SellStick at a time!", true);
            return;
        }

        if (sellstick.getItemMeta() == null) {
            return;
        }

        ItemMeta itemMeta = sellstick.getItemMeta();
        List<Component> sellStickLore = itemMeta.lore();

        if (sellStickLore == null || sellStickLore.size() < 3) {
            ChatUtils.sendMsg(player, "<red>SellStick does not have enough lore lines!", true);
            return;
        }

        String numberLine = MiniMessage.miniMessage().serialize(sellStickLore.get(2));

        if (numberLine.contains("Infinite")) {
            player.getInventory().removeItem(sellstick);
            Bukkit.getServer().getScheduler().runTaskLater(SellStick.getInstance(), () -> {
                // Your code to be executed after 5 seconds goes here
                // For example, you can send a delayed message to the player
                CommandUtils.giveSellStick(player, Integer.MAX_VALUE);
            }, 10L);
            ChatUtils.sendMsg(player, "<green>Replaced old Sellstick!", true);
            return;
        }

        // Use a regular expression to extract the number from the lore line
        Matcher matcher = Pattern.compile("\\d+").matcher(numberLine);

        if (!matcher.find())  {
            ChatUtils.sendMsg(player,"No number found in the lore line.", true);
            return;
        }

        try {
            int uses = Integer.parseInt(matcher.group());
            // Now 'uses' contains the extracted number from the lore line
            player.getInventory().removeItem(sellstick);
            Bukkit.getServer().getScheduler().runTaskLater(SellStick.getInstance(), () -> {
                // Your code to be executed after 5 seconds goes here
                // For example, you can send a delayed message to the player
                CommandUtils.giveSellStick(player, uses);
            }, 10L);
            ChatUtils.sendMsg(player, "<green>Replaced old Sellstick!", true);
        } catch (NumberFormatException e) {
            ChatUtils.sendMsg(player, "Unable to parse the number from the lore line.", true);
        }
    }
}
