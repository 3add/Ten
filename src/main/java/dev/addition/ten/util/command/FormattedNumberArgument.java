package dev.addition.ten.util.command;

import com.mojang.brigadier.LiteralMessage;
import dev.addition.ten.util.text.CaseUtil;
import dev.addition.ten.util.text.FormatUtil;
import dev.addition.ten.util.text.NumberSuffix;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class FormattedNumberArgument {

    public static Argument<Double> formatNumberArgument(String nodeName, @Nullable Double min, @Nullable Double max) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            Double input = FormatUtil.fromFormatText(info.input());

            if (input == null) {
                error(info.input() + " is not a valid number");
            }

            if (min != null && input < min) {
                error(info.input() + " is too big (max " + FormatUtil.formatText(min) + ")");
            }

            if (max != null && input > max) {
                error(info.input() + " is too small (min " + FormatUtil.formatText(max) + ")");
            }

            return input;
        }).replaceSuggestions(((info, builder) -> {
            String current = info.currentArg().trim().toLowerCase();

            // If current is empty, suggest that the user enters an amount
            if (current.isEmpty()) return builder.suggest("<amount>").buildFuture();

            // If last character is a letter, don't suggest anything
            if (Character.isLetter(current.charAt(current.length() - 1))) return builder.buildFuture();

            builder = builder.createOffset(builder.getStart() + info.currentArg().length());

            for (NumberSuffix numberSuffix : NumberSuffix.values()) {
                builder.suggest(numberSuffix.getSuffix(), new LiteralMessage(CaseUtil.toProperCase(numberSuffix.getIdentifier())));
            }

            return builder.buildFuture();
        }));
    }

    private static void error(String message) throws CustomArgumentException {
        throw CustomArgumentException.fromAdventureComponent(Component.text(message, NamedTextColor.RED));
    }
}
