package dev.addition.ten.economy.command;

import dev.addition.ten.economy.Balance;
import dev.addition.ten.economy.Currency;
import dev.addition.ten.economy.Wallet;
import dev.addition.ten.player.PlayerManager;
import dev.addition.ten.player.PlayerRef;
import dev.addition.ten.util.command.EnumArgument;
import dev.addition.ten.util.command.FormattedNumberArgument;
import dev.addition.ten.util.command.PlayerRefArgument;
import dev.addition.ten.util.text.FormatUtil;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class BalanceCommand {

    public static CommandAPICommand create() {
        return new CommandAPICommand("balance")
                .withAliases("bal")
                .withOptionalArguments(
                        EnumArgument.enumArgument("currency", Currency.class),
                        PlayerRefArgument.offlinePlayerRefArgument("target"),
                        EnumArgument.enumArgument("operation", Operation.class)
                                .withPermission("ten.command.balance.modify"),
                        FormattedNumberArgument.formatNumberArgument("amount", 0D, null)
                                .withPermission("ten.command.balance.modify"))
                .executes((ctx) -> {
                    Currency currency = ctx.args()
                            .getOptionalByClass("currency", Currency.class)
                            .orElse(Currency.MONEY);

                    CompletableFuture<PlayerRef> target = ctx.args()
                            .getOptionalByClass("target", PlayerRefArgument.FUTURE_CLASS)
                            .orElseGet(() -> CompletableFuture.completedFuture(PlayerManager.getOnlinePlayer(ctx.sender().getName())));

                    Operation operation = ctx.args()
                            .getOptionalByClass("operation", Operation.class)
                            .orElse(null);

                    Double amount = ctx.args()
                            .getOptionalByClass("amount", Double.class)
                            .orElse(null);

                    target.thenAccept(playerRef -> {
                        if (playerRef == null) {
                            ctx.sender().sendMessage(Component.text("No player by that name was found.", NamedTextColor.RED));
                            return;
                        }

                        Wallet wallet = playerRef.getData().getWallet();
                        Balance currentBalance = playerRef.getData().getWallet().getBalance(currency);

                        if (operation == null || amount == null) {
                            ctx.sender().sendMessage(Component.text(playerRef.getName() + " has ")
                                    .append(FormatUtil.formatComponent(currentBalance)));
                            return;
                        }

                        operation.apply(wallet, Balance.of(currency, amount));
                        Balance newBalance = wallet.getBalance(currency);

                        ctx.sender().sendMessage(
                                Component.text("Modified " + playerRef.getName() + " balance. They now have ")
                                        .append(FormatUtil.formatComponent(newBalance))
                                        .append(Component.text(".")));
                    }).exceptionally(throwable -> {
                        Throwable cause = throwable.getCause();
                        String message = (cause != null) ? cause.getMessage() : throwable.getMessage();

                        ctx.sender().sendMessage(Component.text(message, NamedTextColor.RED));
                        return null;
                    });
                });
    }

    private enum Operation {
        ADD(Wallet::addBalance),
        REMOVE(Wallet::removeBalance),
        SET(Wallet::setBalance),
        ;

        private final BiConsumer<Wallet, Balance> operation;

        Operation(BiConsumer<Wallet, Balance> operation) {
            this.operation = operation;
        }

        public void apply(Wallet wallet, Balance amount) {
            operation.accept(wallet, amount);
        }
    }
}
