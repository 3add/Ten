package dev.addition.ten.util.registry;

import dev.addition.ten.Ten;
import dev.addition.ten.auction.AuctionCommand;
import dev.addition.ten.economy.command.BalanceCommand;
import dev.addition.ten.economy.command.PayCommand;
import dev.addition.ten.qol.ping.PingCommand;
import dev.jorel.commandapi.CommandAPICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CommandRegistry extends Registry<CommandAPICommand> {

    public final static CommandRegistry INSTANCE = new CommandRegistry();
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandRegistry.class);

    private CommandRegistry() {
        register(
                BalanceCommand.create(),
                PayCommand.create(),
                PingCommand.create(),
                AuctionCommand.create()
        );
    }

    public void registerAll(Ten plugin) {
        List<CommandAPICommand> commands = getRegisteredItems();
        commands.forEach(command -> command.register(plugin));
        LOGGER.info("Registered {} commands", commands.size());;
    }
}
