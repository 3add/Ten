package dev.addition.randomkits.util.text;

import org.jetbrains.annotations.NotNull;

public class CaseUtil {

    /**
     * Used to turn a string into a proper cased string
     * @param raw The raw input
     * @return The proper cased output
     */
    public static @NotNull String toProperCase(@NotNull String raw) {
        if (raw.isEmpty()) return raw;

        String[] parts = raw.replace('-', '_').split("_");

        StringBuilder sb = new StringBuilder();

        for (String p : parts) {
            if (p.isEmpty()) continue;

            // lowercase rest, upper first
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase());
            }

            sb.append(' ');
        }

        // remove last space
        return sb.toString().trim();
    }
}
