package dev.addition.ten.economy.command;

import dev.addition.ten.economy.Balance;
import dev.addition.ten.economy.Currency;
import dev.addition.ten.player.PlayerManager;
import dev.addition.ten.player.PlayerRef;
import dev.addition.ten.util.command.EnumArgument;
import dev.addition.ten.util.command.FormattedNumberArgument;
import dev.addition.ten.util.command.PlayerRefArgument;
import dev.addition.ten.util.text.FormatUtil;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class PayCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PayCommand.class);

    public static CommandAPICommand create() {
        return new CommandAPICommand("pay")
                .withArguments(
                        PlayerRefArgument.offlinePlayerRefArgument("target"),
                        FormattedNumberArgument.formatNumberArgument("amount", 0D, null))
                .withOptionalArguments(EnumArgument.enumArgument("currency", Currency.class))
                .executes(ctx -> {

                    @Nullable CompletableFuture<PlayerRef> target = ctx.args()
                            .getOptionalByClass("target", PlayerRefArgument.FUTURE_CLASS)
                            .orElse(null);

                    Double amount = ctx.args().getByClass("amount", Double.class);

                    Currency currency = ctx.args()
                            .getOptionalByClass("currency", Currency.class)
                            .orElse(Currency.MONEY);

                    if (amount == null || target == null) {
                        ctx.sender().sendMessage(Component.text("Invalid arguments provided.", NamedTextColor.RED));
                        return;
                    }

                    Balance toTransferBalance = Balance.of(currency, amount);
                    PlayerRef sender = PlayerManager.getOnlinePlayer(ctx.sender().getName());

                    target.thenAccept(receiver -> {
                        if (receiver == null) {
                            ctx.sender().sendMessage(Component.text("No player by that name was found.", NamedTextColor.RED));
                            return;
                        }

                        LOGGER.info("{} sending to {} ", sender.getName(), receiver.getName());

                        if (receiver.equals(sender)) {
                            ctx.sender().sendMessage(Component.text("You cannot pay yourself.", NamedTextColor.RED));
                            return;
                        }

                        if (!sender.getData().getWallet().canAfford(toTransferBalance)) {
                            ctx.sender().sendMessage(Component.text("You do not have enough ", NamedTextColor.RED)
                                    .append(currency.getColoredName())
                                    .append(Component.text(" to complete this transaction.", NamedTextColor.RED)));
                            return;
                        }

                        sender.getData().getWallet().transferTo(toTransferBalance, receiver.getData().getWallet());

                        ctx.sender().sendMessage(Component.text("You have paid ")
                                .append(Component.text(receiver.getName(), NamedTextColor.GREEN))
                                .append(Component.text(" "))
                                .append(FormatUtil.formatComponent(toTransferBalance)));

                        if (receiver.isOnline()) {
                            receiver.toBukkitPlayer().sendMessage(Component.text("You have received ")
                                    .append(FormatUtil.formatComponent(toTransferBalance))
                                    .append(Component.text(" from "))
                                    .append(Component.text(sender.getName(), NamedTextColor.GREEN)));
                        }
                    }).exceptionally(throwable -> {
                        Throwable cause = throwable.getCause();
                        String message = (cause != null) ? cause.getMessage() : throwable.getMessage();

                        ctx.sender().sendMessage(Component.text(message, NamedTextColor.RED));
                        return null;
                    });
                });
    }
}
