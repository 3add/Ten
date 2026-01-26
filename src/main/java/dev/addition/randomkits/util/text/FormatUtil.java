package dev.addition.randomkits.util.text;

import dev.addition.randomkits.economy.Balance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Objects;

/**
 * Utils for converting objects to {@link Component} or {@link String} and back
 */
public final class FormatUtil {

    /**
     * Used to format a number with "k", "m", etc
     * @param number The unparsed number
     * @return The formatted number as String
     */
    public static @NotNull String formatText(Number number) {
        if (number == null) return "0";

        double value = number.doubleValue();
        if (value == 0) return "0";

        boolean isNegative = value < 0;
        value = Math.abs(value);

        int exponent = (int) Math.floor(Math.log10(value));
        NumberSuffix suffix = NumberSuffix.fromExponent(exponent);

        double scaled = value / suffix.getMultiplier();

        DecimalFormat df = new DecimalFormat("#.##");
        String formatted = df.format(scaled);

        return (isNegative ? "-" : "") + formatted + suffix.getSuffix();
    }

    /**
     * The reverse of {@link FormatUtil#formatText(Number)}, get a number from formatted string
     * @param formatted The formatted string
     * @return The number
     */
    public static @Nullable Double fromFormatText(String formatted) {
        if (formatted == null || formatted.isBlank()) return null;

        formatted = formatted.trim().toLowerCase();

        String numberPart = formatted;
        NumberSuffix suffix = NumberSuffix.NONE;

        if (!formatted.isEmpty() && Character.isLetter(formatted.charAt(formatted.length() - 1))) {
            String suffixStr = formatted.substring(formatted.length() - 1);
            suffix = NumberSuffix.fromString(suffixStr);

            if (suffix == null) return null;

            numberPart = formatted.substring(0, formatted.length() - 1).trim();
        }

        try {
            double value = Double.parseDouble(numberPart);
            return value * suffix.getMultiplier();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Formats an ItemStack, supports amount, item name otherwise fallback to material
     * @param item The item to parse
     * @return The formatted item as string
     */
    public static String formatText(@NotNull ItemStack item) {
        Component customName = item.getItemMeta().customName();
        String itemIdentifier = customName != null
                ? PlainTextComponentSerializer.plainText().serialize(customName)
                : item.getType().name().toLowerCase();

        if (item.getAmount() == 1)
            return itemIdentifier;

        return item.getAmount() + "x of " + itemIdentifier;
    }

    /**
     * Formats an ItemStack, supports amount, item name otherwise fallback to material
     * @param item The item to parse
     * @return The formatted item as Component (colored)
     */
    public static Component formatComponent(@NotNull ItemStack item) {
        Component customName = item.getItemMeta().displayName();
        Component itemIdentifier;

        itemIdentifier = Objects.requireNonNullElseGet(customName, () -> Component
                .text(CaseUtil.toProperCase(item.getType().name()))
                .color(TextColor.color(0x29A0FF)));

        if (item.getAmount() == 1) {
            return itemIdentifier;
        }

        return Component.text(item.getAmount() + "x of ").color(TextColor.color(0x29A0FF))
                .append(itemIdentifier);
    }

    /**
     * Format a location as "x, y, z"
     * @param location The Location to format
     * @return The formatted Location as String
     */
    public static @NotNull String formatComponent(@NotNull Location location) {
        return formatText(location.getX()) + ", " + formatText(location.getY()) + ", " + formatText(location.getZ());
    }

    /**
     * Format a boolean as "Yes" or "No"
     * @param state The boolean
     * @return true -> "Yes", false -> "No"
     */
    @Contract(pure = true)
    public static @NotNull String formatText(boolean state) {
        return state ? "Yes" : "No";
    }

    /**
     * Format a boolean as "Yes" or "No"
     * @param state The boolean
     * @return true -> "Yes", false -> "No" as Component (colored)
     */
    public static @NotNull Component formatComponent(boolean state) {
        return state ? Component.text("Yes").color(TextColor.color(0x30E543))
                : Component.text("No").color(TextColor.color(0xFF3F3F));
    }

    /**
     * Format a Balance as Component
     * @param balance The Balance
     * @return The formatted Balance as Component
     */
    public static @NotNull Component formatComponent(Balance balance) {
        TextColor color = balance.currency().getColoredName().color();

        return Component.text(formatText(balance.amount()))
                .color(color)
                .appendSpace().append(balance.currency().getColoredName());
    }
}
