package com.shmkane.sellstick.utilities;

import com.earth2me.essentials.IEssentials;
import com.shmkane.sellstick.configs.PriceConfig;
import com.shmkane.sellstick.configs.SellstickConfig;
import com.shmkane.sellstick.SellStick;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class EventUtils {

    public static double calculateContainerWorth(PlayerInteractEvent event) {

        InventoryHolder container = (InventoryHolder) Objects.requireNonNull(event.getClickedBlock()).getState();

        ItemStack[] containerContents = container.getInventory().getContents();

        double total = 0;

        SellstickConfig.PriceSource priceSource = SellstickConfig.getPriceSource();

        for (ItemStack itemstack : containerContents) {

            // Check if ItemStack is null
            if (itemstack == null) continue;

            if (itemstack.getItemMeta().hasDisplayName()) continue;

            // Reset each variable on each itemstack
            double price = 0;
            double slotPrice;

            switch (priceSource) {
                case PRICESYML:
                    ConfigurationSection pricesSection = PriceConfig.getConfig().getConfigurationSection("prices");

                    // Initialize a map to store prices
                    assert pricesSection != null;
                    Map<String, Object> prices = pricesSection.getValues(false);

                    // Check Price of ItemStack
                    for (Map.Entry<String, Object> entry : prices.entrySet()) {
                        if (itemstack.getType().toString().equalsIgnoreCase(entry.getKey())) {
                            price = Double.parseDouble(entry.getValue().toString());
                        }
                    }

                    break;
                case SHOPGUI:
                    price = ShopGuiPlusApi.getItemStackPriceSell(itemstack);

                    if (price < 0) {
                        price = 0;
                    }

                    break;
                case ESSWORTH:
                    IEssentials ess = (IEssentials) SellStick.getInstance().getServer().getPluginManager().getPlugin("Essentials");

                    assert ess != null;
                    BigDecimal essPrice = ess.getWorth().getPrice(ess, itemstack);

                    // Check for null before getting double value
                    if (essPrice != null) {
                        price = essPrice.doubleValue();
                    } else {
                        // Handle the case where the price is null (log an error, throw an exception, etc.)
                        // For now, setting price to 0 if it's null
                        price = 0;
                    }

                    break;
            }

            int amount = itemstack.getAmount();

            // ShopGUI already implements amount within the API
            if (priceSource == SellstickConfig.PriceSource.SHOPGUI) {
                amount = 1;
            }

            slotPrice = price * amount;

            if (slotPrice > 0) {
                container.getInventory().remove(itemstack);
            }
            total += slotPrice;
        }
        return total;
    }


    // Checks if clicked block is on a chest, barrel or Shulker Box with a SellStick
    @Deprecated
    public static boolean didClickContainerWithSellStick(PlayerInteractEvent event) {
        ItemStack playerHand = event.getPlayer().getInventory().getItemInMainHand();

        if (ItemUtils.isSellStick(playerHand)) return false;

        Block block = event.getClickedBlock();
        return (block instanceof Chest || block instanceof Barrel || block instanceof ShulkerBox);
    }

    // Checks if clicked block is on a chest, barrel or shulker box
    public static boolean didClickSellStickBlock(Block block) {
        return (block.getState() instanceof Chest || block.getState() instanceof Barrel || block.getState() instanceof ShulkerBox);
    }

    // Handles the SellStick in SaleEvent and PostSaleEvent - (Originally Made by MrGhetto)
    public static boolean saleEvent(Player player, ItemStack sellStick, double total) {

        if (!ItemUtils.isInfinite(sellStick)) {
            sellStick = ItemUtils.subtractUses(sellStick);
        }
        player.getInventory().setItemInMainHand(sellStick);

        double multiplier = setMultiplier(player);
        Economy econ = SellStick.getInstance().getEcon();

        EconomyResponse response = econ.depositPlayer(player, total * multiplier);

        if (!response.transactionSuccess()) {
            ChatUtils.sendMsg(player, String.format("An error occurred: " + SellstickConfig.prefix, response.errorMessage), true);
            return false;
        }

        String[] send = SellstickConfig.sellMessage.split("\\\\n");

        for (String msg : send) {
            ChatUtils.sendMsg(player, msg.replace("%balance%", econ.format(response.balance)).replace("%price%", econ.format(response.amount)),true);
        }
        ChatUtils.log(Level.INFO,player.getName() + " sold items via SellStick for " + response.amount + " and now has " + response.balance);

        if (ItemUtils.getUses(sellStick) <= 0) {
            player.getInventory().removeItem(sellStick);
            ChatUtils.sendMsg(player, SellstickConfig.brokenStick, true);
        }

        return true;
    }

    static double setMultiplier(Player player) {
        /*
         * Permissions based multiplier check. If user doesn't have access to
         * sellstick.multiplier.x permission Multiplier defaults to 1 as seen below.
         */
        double multiplier = 1;

        for (PermissionAttachmentInfo perm : player.getEffectivePermissions()) {
            if (perm.getPermission().startsWith("sellstick.multiplier")) {
                String stringPerm = perm.getPermission();
                String permSection = stringPerm.replaceAll("sellstick.multiplier.", "");
                if (Double.parseDouble(permSection) > multiplier) {
                    multiplier = Double.parseDouble(permSection);
                }
            }
        }
        return multiplier;
    }
}
