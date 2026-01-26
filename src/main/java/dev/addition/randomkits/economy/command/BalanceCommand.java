package dev.addition.randomkits.economy.command;

import dev.addition.randomkits.economy.Balance;
import dev.addition.randomkits.economy.Currency;
import dev.addition.randomkits.economy.Wallet;
import dev.addition.randomkits.util.command.EnumArgument;
import dev.addition.randomkits.util.command.FormattedNumberArgument;
import dev.addition.randomkits.util.command.PlayerRefArgument;
import dev.addition.randomkits.util.text.FormatUtil;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.function.BiConsumer;

public class BalanceCommand {

    public static CommandAPICommand create() {
        return new CommandAPICommand("balance")
                .withAliases("bal")
                .withOptionalArguments(
                        EnumArgument.enumArgument("currency", Currency.class),
                        PlayerRefArgument.offlinePlayerRefArgument("target"),
                        EnumArgument.enumArgument("operation", Operation.class)
                                .withPermission("randomkits.command.balance.modify"),
                        FormattedNumberArgument.formatNumberArgument("amount", 0D, null)
                                .withPermission("randomkits.command.balance.modify"))
                .executes((commandContext) -> {
                    Currency currency = commandContext.args()
                            .getOptionalByClass("currency", Currency.class)
                            .orElse(Currency.MONEY);

                    String targetName = commandContext.args()
                            .getOptionalByClass("target", String.class)
                            .orElseGet(() -> commandContext.sender().getName());

                    Operation operation = commandContext.args()
                            .getOptionalByClass("operation", Operation.class)
                            .orElse(null);

                    Double amount = commandContext.args()
                            .getOptionalByClass("amount", Double.class)
                            .orElse(null);

                    PlayerRefArgument.resolvePlayerRef(targetName, playerRef -> {
                        if (playerRef == null) {
                            commandContext.sender().sendMessage(Component.text("No player by that name was found.", NamedTextColor.RED));
                            return;
                        }

                        Wallet wallet = playerRef.getData().getWallet();
                        Balance currentBalance = playerRef.getData().getWallet().getBalance(currency);

                        if (operation == null || amount == null) {
                            commandContext.sender().sendMessage(Component.text(playerRef.getName() + " has ")
                                    .append(FormatUtil.formatComponent(currentBalance)));
                            return;
                        }

                        operation.apply(wallet, Balance.of(currency, amount));
                        Balance newBalance = wallet.getBalance(currency);

                        commandContext.sender().sendMessage(
                                Component.text("Modified " + playerRef.getName() + " balance. They now have ")
                                        .append(FormatUtil.formatComponent(newBalance))
                                        .append(Component.text(".")));
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
