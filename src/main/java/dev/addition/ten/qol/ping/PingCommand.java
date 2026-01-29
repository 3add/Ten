package dev.addition.ten.qol.ping;

import dev.addition.ten.player.PlayerManager;
import dev.addition.ten.player.PlayerRef;
import dev.addition.ten.util.command.PlayerRefArgument;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

public class PingCommand {

    public static CommandAPICommand create() {
        return new CommandAPICommand("ping")
                .withOptionalArguments(
                        PlayerRefArgument.onlinePlayerRefArgument("target")
                )
                .executes(ctx -> {

                    PlayerRef target = ctx.args()
                            .getOptionalByClass("target", PlayerRef.class)
                            .orElse(null);

                    if (target == null) {
                        if (ctx.sender() instanceof Player player) {
                            target = PlayerManager.getOnlinePlayer(player);
                        } else {
                            ctx.sender().sendMessage(Component.text("You must specify a target when using this command from console.", NamedTextColor.RED));
                            return;
                        }
                    }

                    double ping = PingManager.INSTANCE.getPing(target);
                    Component message = Component.text()
                            .append(Component.text(target.getName()).color(TextColor.color(0x58B5FF)))
                            .append(Component.text("'s ping is ", NamedTextColor.GRAY))
                            .append(Component.text(
                                    ping >= 0
                                            ? String.format("%.2f ms", ping)
                                            : "unknown",
                                    ping >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED
                            ))
                            .build();

                    ctx.sender().sendMessage(message);
                });
    }
}
