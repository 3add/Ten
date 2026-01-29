package dev.addition.ten.economy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public enum Currency {
    MONEY(Component.text("Money").color(TextColor.color(0x2BE063))),
    TOKENS(Component.text("Tokens").color(TextColor.color(0xFCD05C)));

    private final Component coloredName;

    Currency(Component coloredName) {
        this.coloredName =coloredName;
    }

    public Component getColoredName() {
        return coloredName;
    }
}
