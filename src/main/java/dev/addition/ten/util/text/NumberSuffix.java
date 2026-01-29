package dev.addition.ten.util.text;

import org.jetbrains.annotations.Nullable;

public enum NumberSuffix {
    NONE("", "", 1L),
    THOUSAND("thousand", "k", 1_000L),
    MILLION("million", "m", 1_000_000L),
    BILLION("billion", "b", 1_000_000_000L),
    TRILLION("trillion", "t", 1_000_000_000_000L);

    private final String identifier;
    private final String suffix;
    private final long multiplier;

    NumberSuffix(String identifier, String suffix, long multiplier) {
        this.identifier = identifier;
        this.suffix = suffix;
        this.multiplier = multiplier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getSuffix() {
        return suffix;
    }

    public long getMultiplier() {
        return multiplier;
    }

    public static @Nullable NumberSuffix fromString(String s) {
        for (NumberSuffix ns : values()) {
            if (ns.suffix.equals(s)) return ns;
        }

        return null;
    }

    public static NumberSuffix fromExponent(int exponent) {
        int group = Math.max(0, Math.min(exponent / 3, values().length - 1));
        return values()[group];
    }
}