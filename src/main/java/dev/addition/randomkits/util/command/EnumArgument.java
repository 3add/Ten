package dev.addition.randomkits.util.command;

import com.mojang.brigadier.LiteralMessage;
import dev.addition.randomkits.util.text.CaseUtil;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class EnumArgument {

    public static <T extends Enum<T>> Argument<T> enumArgument(String nodeName, Class<T> enumClass) {

        return new CustomArgument<>(new StringArgument(nodeName), info -> {

            T constant = null;

            for (T enumConstant : enumClass.getEnumConstants()) {
                if (enumConstant.name().equalsIgnoreCase(info.input())) {
                    constant = enumConstant;
                }
            }

            if (constant == null) error("Couldn't find a " + info + " " + CaseUtil.toProperCase(enumClass.getSimpleName()));

            return constant;
        }).replaceSuggestions((info, builder) -> {
            for (T enumConstant : enumClass.getEnumConstants()) {
                String properName = enumConstant.name().toLowerCase();
                builder.suggest(properName, new LiteralMessage(properName));
            }

            return builder.buildFuture();
        });
    }

    private static void error(String message) throws CustomArgument.CustomArgumentException {
        throw CustomArgument.CustomArgumentException.fromAdventureComponent(Component.text(message, NamedTextColor.RED));
    }
}
