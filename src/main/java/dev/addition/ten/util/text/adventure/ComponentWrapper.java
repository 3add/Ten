package dev.addition.ten.util.text.adventure;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Style Persistence based of ComponentSplitter

public final class ComponentWrapper {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\S+|\\s+)");

    public static List<Component> wrapComponent(final Component component, final int maxChars) {
        final LinkedList<Component> split = split(component, maxChars);
        return split.stream().map(Component::compact).toList();
    }

    private static LinkedList<Component> split(final Component component, final int maxChars) {
        final LinkedList<Component> result = new LinkedList<>();

        final TextComponent parentLinePrototype = Component.empty().style(component.style());
        result.add(parentLinePrototype);

        if (component instanceof final TextComponent textComponent) {
            final Matcher matcher = TOKEN_PATTERN.matcher(textComponent.content());
            while (matcher.find()) {
                final String token = matcher.group();
                final Component tokenComp = Component.text(token).style(component.style());
                appendTokenToResult(result, tokenComp, maxChars);
            }
        }

        for (final Component child : component.children()) {
            final LinkedList<Component> parts = split(child, maxChars);
            final Component last = result.removeLast();
            final Component firstPart = parts.removeFirst();

            result.add(last.append(firstPart));

            for (final Component part : parts) {
                result.add(parentLinePrototype.append(part));
            }
        }

        return result;
    }

    private static void appendTokenToResult(final LinkedList<Component> result, final Component token, final int maxChars) {
        final Component last = result.removeLast();
        final int lastLen = plainLength(last);
        final int tokenLen = plainLength(token);

        if (lastLen > 0 && lastLen + tokenLen > maxChars) {
            result.add(last);
            final Component newLine = Component.empty().style(last.style()).append(token);
            result.add(newLine);
        } else {
            result.add(last.append(token));
        }
    }

    private static int plainLength(final Component component) {
        if (component instanceof final TextComponent text) {
            int len = text.content().length();
            for (final Component child : text.children()) {
                len += plainLength(child);
            }
            return len;
        } else {
            int len = 0;
            for (final Component child : component.children()) {
                len += plainLength(child);
            }
            return len;
        }
    }
}