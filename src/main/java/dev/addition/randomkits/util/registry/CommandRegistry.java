package dev.addition.randomkits.util.registry;

import dev.addition.randomkits.economy.command.BalanceCommand;
import dev.jorel.commandapi.CommandAPICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CommandRegistry extends Registry<CommandAPICommand> {

    public final static CommandRegistry INSTANCE = new CommandRegistry();
    private static final Logger log = LoggerFactory.getLogger(CommandRegistry.class);

    private CommandRegistry() {
        register(BalanceCommand.create()
        );
    }

    public void registerAll() {
        List<CommandAPICommand> commands = getRegisteredItems();
        commands.forEach(CommandAPICommand::register);
        log.info("Registered {} commands", commands.size());;
    }
}
