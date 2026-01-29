package dev.addition.ten.util.command;

import dev.addition.ten.player.PlayerManager;
import dev.addition.ten.player.PlayerRef;
import dev.addition.ten.util.MojangUtil;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;

public class PlayerRefArgument {

    @SuppressWarnings("unchecked")
    public static Class<CompletableFuture<PlayerRef>> FUTURE_CLASS = (Class<CompletableFuture<PlayerRef>>) (Class<?>) CompletableFuture.class;

    public static Argument<PlayerRef> onlinePlayerRefArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            String inputName = info.input();
            if (!MojangUtil.isValidUsername(inputName)) error(inputName + " is not a valid Minecraft username");

            PlayerRef playerRef = PlayerManager.getCachedPlayer(inputName);
            if (playerRef == null || !playerRef.isOnline()) error("Failed to find an online player named " + inputName);

            return playerRef;
        }).replaceSuggestions((info, builder) -> {
            PlayerManager.getOnlinePlayers().stream()
                    .map(PlayerRef::getName)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        });
    }

    /**
     * Argument that resolves to a PlayerRef for both online and offline players.
     * @param nodeName the name of the argument node
     * @return an Argument that resolves to a CompletableFuture of PlayerRef
     * @apiNote You must handle exceptions in the command executor,
     * the thrown exceptions are instance of {@link RuntimeException} and contain the error message ({@link RuntimeException#getCause()}, {@link Throwable#getMessage()}).
     */
    public static Argument<CompletableFuture<PlayerRef>> offlinePlayerRefArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            String inputName = info.input();

            if (!MojangUtil.isValidUsername(inputName)) {
                error(inputName + " is not a valid Minecraft username");
            }

            return PlayerManager.loadPlayerAsync(inputName).thenApply(opt -> opt.orElseThrow(() ->
                    new RuntimeException("Could not find a player (or they've never joined) named " + inputName)
            ));
        }).replaceSuggestions((info, builder) -> {
            PlayerManager.getCachedPlayers().stream()
                    .map(PlayerRef::getName)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        });
    }

    private static void error(String message) throws CustomArgument.CustomArgumentException {
        throw CustomArgument.CustomArgumentException.fromAdventureComponent(Component.text(message, NamedTextColor.RED));
    }
}