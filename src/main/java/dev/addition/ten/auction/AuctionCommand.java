package dev.addition.ten.auction;

import dev.addition.ten.auction.inventory.AuctionInventory;
import dev.addition.ten.economy.Balance;
import dev.addition.ten.economy.Currency;
import dev.addition.ten.util.command.EnumArgument;
import dev.addition.ten.util.command.FormattedNumberArgument;
import dev.addition.ten.util.text.FormatUtil;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public class  AuctionCommand {

    public static CommandAPICommand create() {
        return new CommandAPICommand("auction")
                .withAliases("ah", "aunctionhouse")
                .withSubcommands(createSell())
                .executesPlayer(ctx -> {
                    ctx.sender().openInventory(new AuctionInventory(ctx.sender()).getInventory());
                });
    }

    private static CommandAPICommand createSell() {
        return new CommandAPICommand("sell")
                .withArguments(
                        FormattedNumberArgument.formatNumberArgument("amount", 0D, null),
                        EnumArgument.enumArgument("currency", Currency.class))
                .withOptionalArguments(new GreedyStringArgument("description"))
                .executesPlayer(ctx -> {
                    ItemStack toSellItem = ctx.sender().getInventory().getItemInMainHand();

                    if (toSellItem.isEmpty()) {
                        ctx.sender().sendMessage(Component.text("You must be holding a valid item to sell.", NamedTextColor.RED));
                        return;
                    }

                    Double amount = ctx.args().getByClass("amount", Double.class);
                    Currency currency = ctx.args().getByClass("currency", Currency.class);

                    if (amount == null || currency == null) {
                        ctx.sender().sendMessage(Component.text("Invalid arguments provided.", NamedTextColor.RED));
                        return;
                    }

                    Balance price = Balance.of(currency, amount);
                    @Nullable String description = ctx.args()
                            .getOptionalByClass("description", String.class)
                            .orElse(null);

                    AuctionListing listing = new AuctionListing(UUID.randomUUID(), toSellItem,
                            ctx.sender().getUniqueId(),
                            ctx.sender().getName(),
                            price, description,
                            Instant.now());
                    AuctionManager.addListing(listing);

                    ctx.sender().sendMessage(Component.text("Successfully listed ", NamedTextColor.GRAY)
                            .append(FormatUtil.formatComponent(toSellItem, false))
                            .append(Component.text(" for ", NamedTextColor.GRAY))
                            .append(FormatUtil.formatComponent(price))
                            .append(Component.text(" on the auction house.", NamedTextColor.GRAY)));

                    ctx.sender().getInventory().removeItemAnySlot(toSellItem);
                });
    }
}
